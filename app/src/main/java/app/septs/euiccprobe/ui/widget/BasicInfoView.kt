package app.septs.euiccprobe.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import app.septs.euiccprobe.R
import app.septs.euiccprobe.databinding.BasicInfoViewBinding
import app.septs.euiccprobe.ui.widget.tool.CustomTextUtil.orDefault

class BasicInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayoutCompat(
        context,
        attrs,
        defStyleAttr
    ) {
    private val viewBinding =
        BasicInfoViewBinding.inflate(LayoutInflater.from(context), this)

    private var labelText: String?
        get() = viewBinding.labelTextview.text.toString()
        set(value) {
            viewBinding.labelTextview.text = value
        }
    var valueText: String?
        get() = viewBinding.valueTextview.text.toString()
        set(value) {
            viewBinding.valueTextview.text = value
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BasicInfoView,
            0,
            0
        ).apply {
            try {
                orientation = VERTICAL
                val paddingVertical =
                    context.resources.getDimensionPixelSize(R.dimen.basic_margin_half)
                val paddingHorizontal =
                    context.resources.getDimensionPixelSize(R.dimen.basic_margin)
                setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                labelText =
                    getString(R.styleable.BasicInfoView_label).orDefault(context.getString(R.string.default_text_placeholder))
                valueText =
                    getString(R.styleable.BasicInfoView_value).orDefault(context.getString(R.string.default_text_placeholder))
            } finally {
                recycle()
            }
        }
    }
}