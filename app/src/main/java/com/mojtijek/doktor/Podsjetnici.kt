package com.mojtijek.doktor

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mojtijek.doktor.data.PodsjetniciPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DateFormat
import java.util.Date

/**
 * Lokalni podsjetnici (iOS `NotifManager`): doze, istek recepta, lijek pri kraju, sljedeća doza cjepiva, pregled.
 * Alarmi nisu „točni” (to traži posebnu dozvolu), pa podsjetnik može kasniti nekoliko minuta.
 * Tekst obavijesti nosi ime člana i lijeka; ništa od toga se ne logira.
 */
object Podsjetnici {
    private const val PREFS = "mojtijek"
    private const val UKLJUCENO = "notif_on"
    private const val ZAKAZANO = "podsjetnici_zakazani"
    private const val KANAL = "podsjetnici"
    const val NASLOV = "naslov"
    const val TEKST = "tekst"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val brava = Mutex()
    private var naCekanju: Job? = null

    fun ukljuceni(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(UKLJUCENO, false)
    fun postavi(context: Context, ukljuceno: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(UKLJUCENO, ukljuceno).apply()
        osvjeziUskoro(context)
    }

    fun smijeObavijesti(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Više promjena zaredom (npr. vraćanje kopije) = jedno osvježavanje. */
    fun osvjeziUskoro(context: Context) {
        val app = context.applicationContext
        naCekanju?.cancel()
        naCekanju = scope.launch { delay(1500); osvjezi(app) }
    }

    private fun namjera(context: Context, id: String): Intent =
        Intent(context, PodsjetnikReceiver::class.java).setData(Uri.parse("mojtijek://podsjetnik/$id"))

    private fun dan(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

    private fun tekst(context: Context, s: PodsjetniciPlan.Stavka): Pair<String, String> {
        val ime = s.clan.ime
        return when (s.vrsta) {
            "doza" -> context.getString(R.string.pod_lijek) to "$ime: ${s.terapija!!.naziv} ${s.terapija.jacina ?: ""}".trim()
            "recept" -> context.getString(R.string.pod_recept) to context.getString(R.string.pod_recept_tekst, ime, s.terapija!!.naziv, dan(s.terapija.receptDo!!))
            "narudzba" -> context.getString(R.string.pod_pri_kraju) to context.getString(R.string.pod_narudzba_tekst, ime, s.terapija!!.naziv, s.terapija.jacina ?: "")
            "cjepivo" -> context.getString(R.string.pod_cijepljenje) to context.getString(R.string.pod_cjepivo_tekst, ime, s.cijepljenje!!.naziv, dan(s.cijepljenje.sljedece!!))
            else -> {
                val ev = s.dogadjaj!!
                val kada = ev.vrijeme?.takeIf { it.isNotBlank() }?.let { " " + context.getString(R.string.pod_u_vrijeme, it).trim() } ?: ""
                val priprema = ev.priprema?.trim()?.takeIf { it.isNotEmpty() }?.let { " " + context.getString(R.string.pod_priprema, it).trim() } ?: ""
                context.getString(R.string.pod_pregled) to "$ime: ${ev.naslov} — ${dan(ev.datum)}$kada.$priprema"
            }
        }
    }

    /** Otkaži sve zakazano i zakaži novi plan. Kad su podsjetnici isključeni, samo otkazuje. */
    suspend fun osvjezi(context: Context) = brava.withLock {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alarmi = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zastavice = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        for (id in prefs.getStringSet(ZAKAZANO, emptySet()) ?: emptySet()) {
            alarmi.cancel(PendingIntent.getBroadcast(app, 0, namjera(app, id), zastavice))
        }
        val repo = (app as? DoktorApp)?.repository
        if (!ukljuceni(app) || repo == null) {
            prefs.edit().remove(ZAKAZANO).apply()
            return@withLock
        }
        val plan = repo.planPodsjetnika(System.currentTimeMillis())
        for (s in plan) {
            val (naslov, opis) = tekst(app, s)
            val pi = PendingIntent.getBroadcast(app, 0, namjera(app, s.id).putExtra(NASLOV, naslov).putExtra(TEKST, opis), zastavice)
            alarmi.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.kada, pi)
        }
        prefs.edit().putStringSet(ZAKAZANO, plan.map { it.id }.toSet()).apply()
    }

    fun prikazi(context: Context, id: String, naslov: String, tekst: String) {
        if (!ukljuceni(context) || !smijeObavijesti(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(KANAL, context.getString(R.string.podsjetnici), NotificationManager.IMPORTANCE_HIGH))
        }
        val otvori = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                               PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        nm.notify(id.hashCode(), NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_podsjetnik)
            .setContentTitle(naslov).setContentText(tekst).setStyle(NotificationCompat.BigTextStyle().bigText(tekst))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true).setContentIntent(otvori).build())
    }
}

/** Okinuti podsjetnik: prikaži obavijest i obnovi plan (da iduće doze budu zakazane i kad se aplikacija ne otvara). */
class PodsjetnikReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.data?.lastPathSegment ?: return
        Podsjetnici.prikazi(context, id, intent.getStringExtra(Podsjetnici.NASLOV) ?: return, intent.getStringExtra(Podsjetnici.TEKST) ?: "")
        obnovi(context, goAsync())
    }
}

/** Alarmi ne preživljavaju ponovno pokretanje telefona ni nadogradnju aplikacije; promjena sata ili zone mijenja kada doze dolaze. */
class PodsjetniciObnovaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = obnovi(context, goAsync())
}

private fun obnovi(context: Context, rezultat: BroadcastReceiver.PendingResult) {
    CoroutineScope(Dispatchers.Default).launch {
        try { Podsjetnici.osvjezi(context) } catch (e: Exception) { } finally { rezultat.finish() }
    }
}
