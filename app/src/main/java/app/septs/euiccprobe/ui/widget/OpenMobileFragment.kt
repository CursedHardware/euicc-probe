package app.septs.euiccprobe.ui.widget

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import app.septs.euiccprobe.OpenMobileAPI
import app.septs.euiccprobe.R
import app.septs.euiccprobe.databinding.FragmentOpenMobileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OpenMobileFragment : Fragment() {
    private var viewBinding: FragmentOpenMobileBinding? = null

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentOpenMobileBinding.inflate(inflater, container, false)
        return viewBinding!!.root
    }

    private fun updateData() {
        viewBinding?.openMobileBackendLiv?.restoreToEmpty()
        viewBinding?.openMobileStateLiv?.restoreToEmpty()
        viewBinding?.openMobileBypassLiv?.restoreToEmpty()
        viewBinding?.openMobileSimslotsLiv?.restoreToEmpty()
        lifecycleScope.launch {
            val data = loadData()
            viewBinding?.openMobileBackendLiv?.supportingText = data["backend"].toString()
            viewBinding?.openMobileStateLiv?.supportingText = data["state"].toString()
            viewBinding?.openMobileBypassLiv?.supportingText = data["bypass"].toString()
            viewBinding?.openMobileSimslotsLiv?.supportingText = buildString {
                for (slot in data["simSlots"] as Map<*, *>) {
                    appendLine("${slot.key}: ${slot.value}")
                }
            }
        }
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {
        val openMobileSet: MutableMap<String, Any> = mutableMapOf()
        context?.let {
            val applicationContext = it.applicationContext
            OpenMobileAPI.getSlots(it.applicationContext).let { result ->
                when (result.backend) {
                    OpenMobileAPI.Backend.Builtin -> openMobileSet["backend"] = "Built-in"
                    OpenMobileAPI.Backend.SIMAlliance -> openMobileSet["backend"] = "SIM Alliance"
                }

                when (result.state) {
                    OpenMobileAPI.State.Unavailable -> openMobileSet["state"] = "Unavailable"
                    OpenMobileAPI.State.Available -> openMobileSet["state"] = "Available"
                    OpenMobileAPI.State.UnableToConnect -> openMobileSet["state"] =
                        "Unable to connect"

                    OpenMobileAPI.State.Unsupported -> openMobileSet["state"] = "Unsupported"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    when (OpenMobileAPI.getBypassState(applicationContext)) {
                        OpenMobileAPI.SEBypass.CanBeBypassed -> openMobileSet["bypass"] =
                            "Can be bypassed"

                        OpenMobileAPI.SEBypass.CannotBeBypassed -> openMobileSet["bypass"] =
                            "Cannot be bypassed"

                        OpenMobileAPI.SEBypass.TemporaryFullAccess -> openMobileSet["bypass"] =
                            "Temporary full access"

                        OpenMobileAPI.SEBypass.PersistentFullAccess -> openMobileSet["bypass"] =
                            "Persistent full access"

                        OpenMobileAPI.SEBypass.Unavailable -> openMobileSet["bypass"] =
                            "Unavailable"
                    }
                }
                openMobileSet["simSlots"] = result.slots
            }
        }
        openMobileSet
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding = null;
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment OpenMobileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OpenMobileFragment()
    }
}