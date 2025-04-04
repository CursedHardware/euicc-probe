package app.septs.euiccprobe.tool

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
            "android.hardware.usb.host",
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
        return try {
            context.packageManager.getApplicationInfo(name, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}