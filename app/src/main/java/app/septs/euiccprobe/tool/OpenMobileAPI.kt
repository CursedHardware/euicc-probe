package app.septs.euiccprobe.tool

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object OpenMobileAPI {
    enum class AppletID(val standard: String, val aid: String) {
        SGP22("SGP.22", "A0000005591010FFFFFFFF8900000100"),
        ESTKme("eSTK.me", "A06573746B6D65FFFFFFFF4953442D52"),
        ESIMme("eSIM.me", "A0000005591010000000008900000300"),
        FiveBer("5ber.eSIM", "A0000005591010FFFFFFFF8900050500");

        @OptIn(ExperimentalStdlibApi::class)
        val aidBytes: ByteArray
            get() = aid.hexToByteArray()
    }

    data class Result(
        val backend: Backend,
        val state: State,
        val slots: Map<String, Map<AppletID, SlotState>>
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

    enum class SlotState {
        NotConnectable,
        Connectable,
        Available,
        Unavailable,
    }

    enum class SEBypass {
        Unavailable,
        CannotBeBypassed,
        CanBeBypassed,
        TemporaryFullAccess,
        PersistentFullAccess
    }

    @Suppress("SpellCheckingInspection")
    @RequiresApi(Build.VERSION_CODES.P)
    fun getBypassState(context: Context): SEBypass {
        if (!SystemService.hasService(context, "com.android.se")) {
            return SEBypass.Unavailable
        }
        val isDebuggable = SystemProperties.isEnabled("ro.debuggable")
        val isFullAccess = SystemProperties.boolean("service.seek") {
            it.contains("fullaccess")
        }
        val isPersistFullAccess = SystemProperties.boolean("persist.service.seek") {
            it.contains("fullaccess")
        }
        if (!isDebuggable) return SEBypass.CannotBeBypassed
        if (isFullAccess) return SEBypass.TemporaryFullAccess
        if (isPersistFullAccess) return SEBypass.PersistentFullAccess
        return SEBypass.CanBeBypassed
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
        fun test(reader: android.se.omapi.Reader, aid: ByteArray): SlotState {
            var session: android.se.omapi.Session? = null
            var channel: android.se.omapi.Channel? = null
            return try {
                session = reader.openSession()
                channel = session.openLogicalChannel(aid) ?: return SlotState.Connectable
                if (channel.isOpen) SlotState.Available else SlotState.Unavailable
            } catch (_: SecurityException) {
                SlotState.Available
            } catch (e: Throwable) {
                Log.e(javaClass.name, "${reader.name} = ${e.message}", e)
                SlotState.Connectable
            } finally {
                if (channel != null && channel.isOpen) channel.close()
                if (session != null && !session.isClosed) session.closeChannels()
            }
        }
        val slots = buildMap {
            for (reader in service.readers) {
                if (!reader.name.startsWith("SIM")) continue
                put(normalizeName(reader.name), buildMap {
                    for (applet in AppletID.entries) {
                        put(applet, test(reader, applet.aidBytes))
                    }
                })
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
        fun test(reader: org.simalliance.openmobileapi.Reader, aid: ByteArray): SlotState {
            var session: org.simalliance.openmobileapi.Session? = null
            var channel: org.simalliance.openmobileapi.Channel? = null
            return try {
                session = reader.openSession()
                channel = session.openLogicalChannel(aid) ?: return SlotState.Connectable
                if (channel.isClosed) SlotState.Unavailable else SlotState.Available
            } catch (_: SecurityException) {
                SlotState.Available
            } catch (e: Throwable) {
                Log.e(javaClass.name, "${reader.name} = ${e.message}", e)
                SlotState.Connectable
            } finally {
                if (channel != null && !channel.isClosed) channel.close()
                if (session != null && !session.isClosed) session.closeChannels()
            }
        }
        val slots = buildMap {
            for (reader in service.readers) {
                if (!reader.name.startsWith("SIM")) continue
                put(normalizeName(reader.name), buildMap {
                    for (applet in AppletID.entries) {
                        put(applet, test(reader, applet.aidBytes))
                    }
                })
            }
            service.shutdown()
        }
        val state = if (slots.isEmpty()) State.Unavailable else State.Available
        return Result(Backend.SIMAlliance, state, slots)
    }

    private fun getCardSlots(context: Context): Map<String, Map<AppletID, SlotState>> {
        val service = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val broken = buildMap { put(AppletID.SGP22, SlotState.NotConnectable) }
        val count = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> service.activeModemCount
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> @Suppress("DEPRECATION") service.phoneCount
            else -> return mapOf(Pair("SIM1", broken))
        }
        return buildMap {
            for (index in 1..count) {
                put("SIM$index", broken)
            }
        }
    }

    private fun normalizeName(name: String) = if (name == "SIM") "SIM1" else name
}
