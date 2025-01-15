package app.septs.euiccprobe.ui.widget

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.septs.euiccprobe.R
import app.septs.euiccprobe.SystemApps
import app.septs.euiccprobe.SystemProperties
import app.septs.euiccprobe.SystemService
import app.septs.euiccprobe.databinding.FragmentSystemBinding
import app.septs.euiccprobe.ui.widget.adapter.SystemFeaturesAdapter
import app.septs.euiccprobe.ui.widget.adapter.SystemPropertiesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple [Fragment] subclass.
 * Use the [SystemFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SystemFragment : Fragment() {
    private var viewBinding: FragmentSystemBinding? = null
    private var systemFeaturesAdapter: SystemFeaturesAdapter? = null
    private val systemFeatures: MutableMap<String, Boolean> = mutableMapOf()
    private var systemPropertiesAdapter: SystemPropertiesAdapter? = null
    private val systemProperties: MutableMap<String, String> = mutableMapOf()
    private val systemLPAs: MutableMap<String, String> = mutableMapOf()
    private var eUICCSystemServiceState: SystemService.EuiccState? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSystemBinding.inflate(inflater, container, false)

        systemFeaturesAdapter = SystemFeaturesAdapter(systemFeatures)
        viewBinding?.systemFeaturesRv?.layoutManager = LinearLayoutManager(context)
        viewBinding?.systemFeaturesRv?.adapter = systemFeaturesAdapter

        systemPropertiesAdapter = SystemPropertiesAdapter(systemProperties)
        viewBinding?.systemPropertiesRv?.layoutManager = LinearLayoutManager(context)
        viewBinding?.systemPropertiesRv?.adapter = systemPropertiesAdapter
        return viewBinding!!.root
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            try {
                updateData()
            } catch (e: Throwable) {
                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle(resources.getString(R.string.warning))
                        .setMessage(e.stackTraceToString())
                        .show()
                }
            }
        }
    }

    private fun updateData() {
        eUICCSystemServiceState = null
        viewBinding?.euiccSystemServiceStatusTextView?.text = getString(R.string.unknown)
        context?.let {
            viewBinding?.euiccSystemServiceStatusTextView?.setTextColor(
                ContextCompat.getColor(
                    it,
                    com.google.android.material.R.color.material_on_surface_disabled
                )
            )
        }
        viewBinding?.systemLpasLiv?.restoreToUnknownEmpty()
        systemFeatures.clear()
        systemFeaturesAdapter?.notifyDataSetChanged()
        systemProperties.clear()
        systemPropertiesAdapter?.notifyDataSetChanged()


        lifecycleScope.launch {
            loadData()
            updateView()
        }
    }

    private fun updateView() {
        viewBinding?.euiccSystemServiceStatusTextView?.text = eUICCSystemServiceState.toString()
        if (eUICCSystemServiceState == SystemService.EuiccState.Enabled) {
            context?.let {
                viewBinding?.euiccSystemServiceStatusTextView?.setTextColor(
                    ContextCompat.getColor(it, R.color.md_theme_primary)
                )
            }
        }
        val key = systemLPAs.keys.first()
        viewBinding?.systemLpasLiv?.headlineText = key
        viewBinding?.systemLpasLiv?.supportingText = systemLPAs[key]
        viewBinding?.systemLpasLiv?.leadingIconDrawable =
            systemLPAs[key]?.let { context?.packageManager?.getApplicationIcon(it) }
        systemFeaturesAdapter?.notifyDataSetChanged()
        systemPropertiesAdapter?.notifyDataSetChanged()
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {
        // eUICC System Service Status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            eUICCSystemServiceState =
                context?.let { SystemService.getEuiccServiceState(it.applicationContext) }
        }

        //System LPAs
        SystemApps.getSystemLPAs().let { pkgs ->
            if (pkgs.isEmpty()) {
                return@let
            }
            for (pkg in pkgs) {
                val label = context?.let {
                    SystemApps.getApplicationLabel(
                        it.packageManager,
                        pkg.packageName
                    )
                }
                systemLPAs.clear()
                if (label != null) {
                    systemLPAs[label] = pkg.packageName
                } else {
                    systemLPAs[resources.getString(R.string.unknown)] = pkg.packageName
                }
            }
        }

        //System Features
        context?.let {
            SystemService.getSystemFeatures(it.applicationContext).let {
                systemFeatures.clear()
                for (feature in it.entries) {
                    systemFeatures[feature.key] = feature.value
                }
            }
        }

        //System Properties
        val systemProperties: MutableMap<String, String> = mutableMapOf()
        SystemProperties.pick(
            *arrayOf(
                "esim.enable_esim_system_ui_by_default",
                "ro.telephony.sim_slots.count",
                "ro.setupwizard.esim_cid_ignore",
                // RIL
                "gsm.version.ril-impl",
                // Multi SIM
                "ro.multisim.simslotcount",
                "ro.vendor.multisim.simslotcount",
                "persist.radio.multisim.config",
                // Xiaomi Vendor
                "ro.vendor.miui.support_esim"
            )
        ).let {
            if (it.isEmpty()) return@let
            for (entry in it.entries) {
                systemProperties[entry.key] = entry.value
            }
        }
        this@SystemFragment.systemProperties.clear()
        this@SystemFragment.systemProperties.putAll(systemProperties)
    }
}