package app.septs.euiccprobe

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object OpenMobileAPI {
    @OptIn(ExperimentalStdlibApi::class)
    private val ISD_R_APPLET_ID = "A0000005591010FFFFFFFF8900000100".hexToByteArray()

    data class Result(
        val backend: Backend,
        val state: State,
        val slots: Map<String, Boolean>
    )

    enum class Backend {
        Builtin,
        SIMAlliance,
    }

    enum class State {
        Unsupported,
        UnableToConnect,
        Unavailable,
        Available,
    }


    enum class SEBypass {
        Unavailable,
        CannotBeBypassed,
        CanBeBypassed,
        FullAccess,
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getBypassState(context: Context): SEBypass {
        val pkgName = "com.android.se"
        if (!SystemService.hasService(context, pkgName)) {
            return SEBypass.Unavailable
        }
        if (SystemProperties["ro.debuggable"].toIntOrNull() != 1) {
            return SEBypass.CannotBeBypassed
        }
        val isFullAccess = SystemProperties["service.seek"]
            .ifEmpty { SystemProperties["persist.service.seek"] }
            .contains("fullaccess")
        return if (isFullAccess) SEBypass.FullAccess else SEBypass.CanBeBypassed
    }

    suspend fun getSlots(context: Context): Result {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fromBuiltin(context)
        } else {
            fromSIMAlliance(context)
        }
        return Result(
            backend = result.backend,
            state = result.state,
            slots = buildMap {
                putAll(getCardSlots(context))
                putAll(result.slots)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun fromBuiltin(context: Context): Result {
        val pkgName = "com.android.se"
        if (!SystemService.hasService(context, pkgName)) {
            return Result(Backend.Builtin, State.Unsupported, emptyMap())
        }
        val service = suspendCoroutine {
            val executor = Executors.newSingleThreadExecutor()
            lateinit var service: android.se.omapi.SEService
            service = android.se.omapi.SEService(context, executor) {
                it.resume(service)
            }
        }
        if (!service.isConnected) {
            return Result(Backend.Builtin, State.UnableToConnect, emptyMap())
        }
        val slots = buildMap {
            for (reader in service.readers) {
                if (!reader.name.startsWith("SIM")) continue
                try {
                    val session = reader.openSession()
                    val channel = session.openLogicalChannel(ISD_R_APPLET_ID) ?: continue
                    put(reader.name, channel.isOpen)
                    if (channel.isOpen) channel.close()
                    if (!session.isClosed) session.closeChannels()
                } catch (_: SecurityException) {
                    put(reader.name, true)
                } catch (e: Throwable) {
                    Log.e(javaClass.name, "${reader.name} = ${e.message}")
                }
            }
            service.shutdown()
        }
        val state = if (slots.isEmpty()) State.Unavailable else State.Available
        return Result(Backend.Builtin, state, slots)
    }

    private suspend fun fromSIMAlliance(context: Context): Result {
        val pkgName = "org.simalliance.openmobileapi.service"
        if (!SystemService.hasService(context, pkgName)) {
            return Result(Backend.SIMAlliance, State.Unsupported, emptyMap())
        }
        val service = suspendCoroutine {
            org.simalliance.openmobileapi.SEService(context, it::resume)
        }
        if (!service.isConnected) {
            return Result(Backend.SIMAlliance, State.UnableToConnect, emptyMap())
        }
        val slots = buildMap<String, Boolean> {
            for (reader in service.readers) {
                if (!reader.name.startsWith("SIM")) continue
                try {
                    val session = reader.openSession()
                    val channel = session.openLogicalChannel(ISD_R_APPLET_ID)
                    put(reader.name, !channel.isClosed)
                    if (!channel.isClosed) channel.close()
                    if (!session.isClosed) session.closeChannels()
                } catch (_: SecurityException) {
                    put(reader.name, true)
                } catch (e: Throwable) {
                    Log.e(javaClass.name, "${reader.name} = ${e.message}")
                }
            }
            service.shutdown()
        }
        val state = if (slots.isEmpty()) State.Unavailable else State.Available
        return Result(Backend.SIMAlliance, state, slots)
    }

    private fun getCardSlots(context: Context): Map<String, Boolean> {
        val service = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val count = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> service.activeModemCount
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> @Suppress("DEPRECATION") service.phoneCount
            else -> return mapOf(Pair("SIM", false))
        }
        return buildMap {
            for (index in 1..count) {
                put("SIM$index", false)
            }
        }
    }
}
