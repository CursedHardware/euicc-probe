package app.septs.euiccprobe.ui.widget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.septs.euiccprobe.databinding.CheckboxLabelViewBinding
import com.google.android.material.checkbox.MaterialCheckBox

class SystemFeaturesAdapter(private val systemFeatures: Map<String, Boolean>) :
    RecyclerView.Adapter<SystemFeaturesAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(val binding: CheckboxLabelViewBinding, view: View) :
        RecyclerView.ViewHolder(view) {
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            CheckboxLabelViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val labelText = systemFeatures.keys.elementAt(position)
        val checkBoxState = if (systemFeatures[labelText] == true) {
            MaterialCheckBox.STATE_CHECKED
        } else {
            MaterialCheckBox.STATE_UNCHECKED
        }
        binding.clvCheckbox.checkedState = checkBoxState
        binding.clvLabelTextview.text = labelText
    }

    override fun getItemCount(): Int = systemFeatures.size
}