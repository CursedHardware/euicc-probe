package app.septs.euiccprobe.ui.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.septs.euiccprobe.R
import app.septs.euiccprobe.SystemApps
import app.septs.euiccprobe.SystemProperties
import app.septs.euiccprobe.databinding.FragmentSystemBinding
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
    private var systemPropertiesAdapter: SystemPropertiesAdapter? = null
    private var systemProperties: MutableMap<String, String> = mutableMapOf()
    private var systemLPAs: MutableMap<String, String> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSystemBinding.inflate(inflater, container, false)
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
        systemProperties.clear()
        systemPropertiesAdapter?.notifyDataSetChanged()
        viewBinding?.systemLpasLiv?.restoreToUnknownEmpty()

        lifecycleScope.launch {
            loadData()
            systemPropertiesAdapter?.notifyDataSetChanged()
            val key = systemLPAs.keys.first()
            viewBinding?.systemLpasLiv?.headlineText = key
            viewBinding?.systemLpasLiv?.supportingText = systemLPAs[key]
            viewBinding?.systemLpasLiv?.leadingIconDrawable =
                systemLPAs[key]?.let { context?.packageManager?.getApplicationIcon(it) }
        }
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {
        val systemProperties: MutableMap<String, String> = mutableMapOf()
        val properties = arrayOf(
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
        SystemProperties.pick(*properties).let {
            if (it.isEmpty()) return@let
            for (entry in it.entries) {
                systemProperties[entry.key] = entry.value
            }
        }
        this@SystemFragment.systemProperties.clear()
        this@SystemFragment.systemProperties.putAll(systemProperties)

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
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SystemFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SystemFragment()
    }
}