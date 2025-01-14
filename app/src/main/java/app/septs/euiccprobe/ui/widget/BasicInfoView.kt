package app.septs.euiccprobe.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import app.septs.euiccprobe.R
import app.septs.euiccprobe.databinding.BasicInfoViewBinding

class BasicInfoView : LinearLayout {
    private var labelText: String? = null
    private var valueText: String? = null
    private var alignmentValue: Int? = null

    private lateinit var viewBinding: BasicInfoViewBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) : super(
        context,
        attributeSet,
        style
    ) {
        obtainAttributes(attributeSet)
        loadView()
    }

    private fun obtainAttributes(attributeSet: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.BasicInfoView)
        labelText = ta.getString(R.styleable.BasicInfoView_label)
        valueText = ta.getString(R.styleable.BasicInfoView_value)
        alignmentValue = ta.getInt(R.styleable.BasicInfoView_alignment, 0)
        ta.recycle()
    }

    private fun loadView() {
        viewBinding = BasicInfoViewBinding.inflate(LayoutInflater.from(context), this)
    }
}