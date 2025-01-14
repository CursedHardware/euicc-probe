package app.septs.euiccprobe

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.septs.euiccprobe.databinding.ActivityMainBinding
import app.septs.euiccprobe.ui.widget.OpenMobileFragment
import app.septs.euiccprobe.ui.widget.SystemFragment
import com.google.android.material.tabs.TabLayoutMediator
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tasklist.TaskListPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    val tabFragments = listOf(
        OpenMobileFragment(),
        SystemFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        loadView()
    }

    private fun loadView() {
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.main) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewBinding.mainTabViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabFragments.size

            override fun createFragment(position: Int): Fragment = tabFragments[position]

        }

        TabLayoutMediator(viewBinding.mainTabLayout, viewBinding.mainTabViewPager) { tab, position ->
            // 使用 TabItem 中的标题
            tab.text = when (position) {
                0 -> resources.getString(R.string.open_mobile)
                1 -> resources.getString(R.string.system)
                else -> ""
            }
        }.attach()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            try {
                init()
            } catch (e: Throwable) {

            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private suspend fun init() = withContext(Dispatchers.Main) {
        val markdown = buildString {
            appendLine(Version.getModelName())
            appendLine()
            appendLine(runBlocking {
                val parts = mutableListOf(
                    "Android ${Build.VERSION.RELEASE}",
                )
                Version.getFirmwareVersion()?.let { parts.add(it) }
                parts.joinToString("; ")
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val state = SystemService.getEuiccServiceState(applicationContext)
                appendLine()
                appendLine("eUICC System Service: $state")
            }
            SystemService.getSystemFeatures(applicationContext).let {
                appendLine()
                appendLine("System Features:")
                for (feature in it.entries) {
                    appendLine("- [${if (feature.value) "x" else " "}] ${feature.key}")
                }
            }
            SystemApps.getSystemLPAs().let { pkgs ->
                if (pkgs.isEmpty()) {
                    return@let
                }
                appendLine()
                appendLine("System LPAs:")
                for (pkg in pkgs) {
                    val label = SystemApps.getApplicationLabel(packageManager, pkg.packageName)
                    if (label != null) {
                        appendLine("- $label (${pkg.packageName})")
                    } else {
                        appendLine("- ${pkg.packageName}")
                    }
                }
            }
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
                appendLine()
                appendLine("System Properties:")
                for (entry in it.entries) {
                    appendLine("- ${entry.key} = ${entry.value}")
                }
            }
            OpenMobileAPI.getSlots(applicationContext).let { result ->
                appendLine()
                appendLine("Open Mobile API:")
                appendLine("- Backend: ${result.backend}")
                appendLine("- State: ${result.state}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val state = OpenMobileAPI.getBypassState(applicationContext)
                    appendLine("- Bypass: $state")
                }
                if (result.state == OpenMobileAPI.State.Available) {
                    for (slot in result.slots) {
                        appendLine("- ${slot.key} Slot: ${slot.value}")
                    }
                }
            }
        }
    }
}