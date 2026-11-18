package com.mojtijek.doktor.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

/** Jedna pročitana vrijednost; `vrijednost2` je dijastolički tlak. */
class Ocitanje(val vrijednost: Double, val ts: Long, val vrijednost2: Double? = null)

/**
 * Čitanje iz Health Connecta (iOS `HealthKitManager.najnovije`): samo pokazatelji koje Praćenje prikazuje,
 * samo čitanje. Vrijednosti se ne logiraju.
 */
object HealthConnect {
    /** Izvor zapisa u bazi. iOS svoje vodi kao "healthkit". */
    const val IZVOR = "healthconnect"

    val JEDINICE = mapOf("koraci" to "", "puls" to "bpm", "tlak" to "mmHg", "san" to "h", "saturacija" to "%", "secer" to "mmol/L", "tezina" to "kg")

    val DOZVOLE: Set<String> = setOf(
        StepsRecord::class, HeartRateRecord::class, BloodPressureRecord::class, SleepSessionRecord::class,
        OxygenSaturationRecord::class, BloodGlucoseRecord::class, WeightRecord::class,
    ).map { HealthPermission.getReadPermission(it) }.toSet()

    fun stanje(context: Context): Int = HealthConnectClient.getSdkStatus(context)
    fun dostupan(context: Context): Boolean = stanje(context) == HealthConnectClient.SDK_AVAILABLE

    /** Dovoljna je i jedna dozvola: čita se što je dopušteno, ostalo se preskače. */
    suspend fun imaDozvolu(context: Context): Boolean =
        HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().any { it in DOZVOLE }

    /** Faze koje se broje kao san; budnost i ležanje u krevetu ne. */
    private val FAZE_SNA = setOf(SleepSessionRecord.STAGE_TYPE_SLEEPING, SleepSessionRecord.STAGE_TYPE_LIGHT,
                                 SleepSessionRecord.STAGE_TYPE_DEEP, SleepSessionRecord.STAGE_TYPE_REM)

    /** Koraci = današnji zbroj; san = zbroj zadnjih 36 h; ostalo = zadnje očitanje u 30 dana (dalje Health Connect ne daje). */
    suspend fun najnovije(context: Context): Map<String, Ocitanje> {
        val c = HealthConnectClient.getOrCreate(context)
        val sada = Instant.now()
        val od = sada.minus(Duration.ofDays(30))
        val out = HashMap<String, Ocitanje>()
        // Pokazatelj bez dozvole baca SecurityException — preskače se, ostali se čitaju.
        suspend fun pokusaj(blok: suspend () -> Unit) {
            try { blok() } catch (e: CancellationException) { throw e } catch (e: Exception) { }
        }
        suspend fun <T : Record> zadnji(tip: KClass<T>): T? =
            c.readRecords(ReadRecordsRequest(tip, TimeRangeFilter.between(od, sada), ascendingOrder = false, pageSize = 1)).records.firstOrNull()

        pokusaj {
            val pocetakDana = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(pocetakDana, sada)))[StepsRecord.COUNT_TOTAL]
                ?.let { out["koraci"] = Ocitanje(it.toDouble(), sada.toEpochMilli()) }
        }
        pokusaj {
            zadnji(HeartRateRecord::class)?.samples?.maxByOrNull { it.time }
                ?.let { out["puls"] = Ocitanje(it.beatsPerMinute.toDouble(), it.time.toEpochMilli()) }
        }
        pokusaj {
            zadnji(BloodPressureRecord::class)
                ?.let { out["tlak"] = Ocitanje(it.systolic.inMillimetersOfMercury, it.time.toEpochMilli(), it.diastolic.inMillimetersOfMercury) }
        }
        pokusaj { zadnji(OxygenSaturationRecord::class)?.let { out["saturacija"] = Ocitanje(it.percentage.value, it.time.toEpochMilli()) } }
        pokusaj { zadnji(BloodGlucoseRecord::class)?.let { out["secer"] = Ocitanje(it.level.inMillimolesPerLiter, it.time.toEpochMilli()) } }
        pokusaj { zadnji(WeightRecord::class)?.let { out["tezina"] = Ocitanje(it.weight.inKilograms, it.time.toEpochMilli()) } }
        pokusaj {
            val sesije = c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(sada.minus(Duration.ofHours(36)), sada))).records
            val sekunde = sesije.sumOf { s ->
                if (s.stages.isEmpty()) Duration.between(s.startTime, s.endTime).seconds
                else s.stages.filter { it.stage in FAZE_SNA }.sumOf { Duration.between(it.startTime, it.endTime).seconds }
            }
            if (sekunde > 0) out["san"] = Ocitanje(sekunde / 3600.0, sesije.maxOf { it.endTime }.toEpochMilli())
        }
        return out
    }
}
