package app.septs.euiccprobe.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import app.septs.euiccprobe.R
import app.septs.euiccprobe.databinding.BasicInfoViewBinding


class BasicInfoView : LinearLayoutCompat {
    private lateinit var viewBinding: BasicInfoViewBinding
    var labelText: String
        get() = viewBinding.labelTextview.text.toString()
        set(value) {
            viewBinding.labelTextview.text = value
        }
    var valueText: String
        get() = viewBinding.valueTextview.text.toString()
        set(value) {
            viewBinding.valueTextview.text = value
        }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) : super(
        context,
        attributeSet,
        style
    ) {
        loadView(attributeSet)
    }

    private fun loadView(attributeSet: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.BasicInfoView)
        val labelTextAttr = ta.getString(R.styleable.BasicInfoView_label)
        val valueTextAttr = ta.getString(R.styleable.BasicInfoView_value)
        ta.recycle()
        viewBinding = BasicInfoViewBinding.inflate(LayoutInflater.from(context), this)
        labelText = labelTextAttr ?: ""
        valueText = valueTextAttr ?: ""
    }
}