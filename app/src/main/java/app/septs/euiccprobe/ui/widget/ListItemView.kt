package app.septs.euiccprobe.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import app.septs.euiccprobe.R
import app.septs.euiccprobe.databinding.ListItemViewBinding
import app.septs.euiccprobe.ui.widget.tool.CustomTextUtil.orDefault

class ListItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ListItemViewBinding = ListItemViewBinding.inflate(
        LayoutInflater.from(context), this
    )

    var leadingIcon: Int?
        get() = binding.leadingIcon.tag as? Int
        set(value) {
            if (value != null) {
                binding.leadingIcon.setImageResource(value)
                binding.leadingIcon.tag = value
                binding.leadingIcon.isVisible = true
            } else {
                binding.leadingIcon.isVisible = false
            }
        }

    var leadingIconDrawable: Drawable?
        get() = binding.leadingIcon.drawable
        set(value) {
            if (value != null) {
                binding.leadingIcon.setImageDrawable(value)
                binding.leadingIcon.tag = value
                binding.leadingIcon.isVisible = true
            } else {
                binding.leadingIcon.isVisible = false
            }
        }

    var headlineText: String
        get() = binding.headlineText.text.toString()
        set(value) {
            binding.headlineText.text = value
        }

    var supportingText: String?
        get() = binding.supportingText.text.toString()
        set(value) {
            binding.supportingText.text = value.orDefault("--")
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ListItemView,
            0,
            0
        ).apply {
            try {
                // Initialize leading icon
                val iconRes = getResourceId(R.styleable.ListItemView_leadingIcon, -1)
                leadingIcon = if (iconRes != -1) iconRes else null

                // Initialize headline text
                headlineText = getString(R.styleable.ListItemView_headlineText) ?: ""

                // Initialize supporting text
                supportingText = getString(R.styleable.ListItemView_supportingText)
            } finally {
                recycle()
            }
        }
    }

    fun restoreToEmpty() {
        supportingText = "--"
    }

    fun restoreToUnknownEmpty() {
        leadingIconDrawable = null
        headlineText = context.getString(R.string.unknown)
        supportingText = "--"
    }
}
