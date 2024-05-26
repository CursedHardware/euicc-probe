package app.septs.euiccprobe

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tasklist.TaskListPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var markwon: Markwon
    private lateinit var scrollView: HorizontalScrollView
    private lateinit var report: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        scrollView = findViewById(R.id.scroll_view)
        report = findViewById(R.id.report)
        markwon = Markwon.builder(this)
            .usePlugin(TaskListPlugin.create(this))
            .build()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            try {
                init()
            } catch (e: Throwable) {
                report.text = e.stackTraceToString()
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private suspend fun init() = withContext(Dispatchers.Main) {
        val markdown = buildString {
            appendLine(runBlocking {
                val parts = mutableListOf(
                    Build.BRAND,
                    Build.MODEL.removePrefix(Build.BRAND).trim(),
                )
                if (!Build.BRAND.contentEquals(Build.MANUFACTURER, ignoreCase = true)) {
                    parts.add("(${Build.MANUFACTURER})")
                }
                parts.joinToString(" ")
            })
            appendLine()
            appendLine(runBlocking {
                val parts = mutableListOf(
                    "Android ${Build.VERSION.RELEASE}",
                )
                Version.getVersion()?.let { parts.add(it) }
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
            SystemApps.getSystemLPAs(applicationContext).let { apps ->
                if (apps.isEmpty()) {
                    return@let
                }
                appendLine()
                appendLine("System LPAs:")
                for (app in apps) {
                    val label = packageManager.getApplicationLabel(app).let {
                        if (it.contentEquals(app.packageName)) {
                            it
                        } else {
                            "$it (${app.packageName})"
                        }
                    }
                    appendLine("- $label")
                }
            }
            val properties = arrayOf(
                "esim.enable_esim_system_ui_by_default",
                "ro.telephony.sim_slots.count",
                "ro.setupwizard.esim_cid_ignore",
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
                        val state = if (slot.value) "Available" else "Unavailable"
                        appendLine("- ${slot.key} Slot: $state")
                    }
                }
            }
        }
        markwon.setMarkdown(report, markdown)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }
}