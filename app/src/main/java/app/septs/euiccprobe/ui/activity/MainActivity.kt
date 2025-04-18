package app.septs.euiccprobe.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.septs.euiccprobe.R
import app.septs.euiccprobe.tool.Version
import app.septs.euiccprobe.databinding.ActivityMainBinding
import app.septs.euiccprobe.ui.fragment.OpenMobileFragment
import app.septs.euiccprobe.ui.fragment.SystemFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
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
        bindViewPagerToTabBar()
    }

    private fun bindViewPagerToTabBar() {
        viewBinding.mainTabViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabFragments.size

            override fun createFragment(position: Int): Fragment = tabFragments[position]

        }

        TabLayoutMediator(
            viewBinding.mainTabLayout,
            viewBinding.mainTabViewPager
        ) { tab, position ->
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
                updateData()
            } catch (e: Throwable) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(resources.getString(R.string.warning))
                    .setMessage(e.stackTraceToString())
                    .show()
            }
        }
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {
        val deviceInfoSet: MutableMap<String, String?> = mutableMapOf()
        deviceInfoSet["device"] = Version.getModelName()
        val firmwareVersion = Version.getFirmwareVersion() ?: ""
        val displayFirmwareVersion = if (firmwareVersion.isEmpty()) {
            ""
        } else {
            " $firmwareVersion"
        }
        deviceInfoSet["version"] =
            runBlocking { "${Build.VERSION.RELEASE ?: resources.getString(R.string.unknown)}${displayFirmwareVersion}" }
        deviceInfoSet
    }

    private fun updateData() {
        viewBinding.deviceName.restore()
        viewBinding.androidVersion.restore()
        lifecycleScope.launch {
            val data = loadData()
            viewBinding.deviceName.valueText = data["device"]
            viewBinding.androidVersion.valueText = data["version"]
        }
    }
}