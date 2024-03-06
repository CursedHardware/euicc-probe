package app.septs.euiccprobe

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.euicc.EuiccManager
import androidx.annotation.RequiresApi

object SystemService {

    enum class EuiccState {
        Unsupported,
        Unimplemented,
        Enabled,
        Disabled,
    }

    fun getSystemFeatures(context: Context): Map<String, Boolean> {
        val features = arrayOf(
            "android.hardware.telephony",
            "android.hardware.telephony.subscription",
            "android.hardware.telephony.euicc",
            "android.hardware.telephony.euicc.mep",
            "android.hardware.se.omapi.uicc",
        )
        return buildMap {
            for (feature in features) {
                put(feature, context.packageManager.hasSystemFeature(feature))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getEuiccServiceState(context: Context): EuiccState {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC)) {
            return EuiccState.Unsupported
        }
        val service = context.getSystemService(Context.EUICC_SERVICE) as EuiccManager?
            ?: return EuiccState.Unimplemented
        return if (service.isEnabled) EuiccState.Enabled else EuiccState.Disabled
    }

    fun hasService(context: Context, name: String): Boolean {
        val pm = context.packageManager
        val flags = 0
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(name, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
            } else {
                pm.getApplicationInfo(name, flags)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}