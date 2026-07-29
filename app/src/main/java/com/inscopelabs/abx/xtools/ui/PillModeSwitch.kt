package com.inscopelabs.abx.xtools.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.inscopelabs.abx.xtools.R

class PillModeSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onToggleListener: ((Boolean) -> Unit)? = null
    private var isActive: Boolean = true

    private val leftContainer: LinearLayout
    private val leftIcon: ImageView
    private val leftText: TextView

    private val rightContainer: LinearLayout
    private val rightIcon: ImageView
    private val rightText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_pill_mode_switch, this, true)

        leftContainer = findViewById(R.id.leftContainer)
        leftIcon = findViewById(R.id.leftIcon)
        leftText = findViewById(R.id.leftText)

        rightContainer = findViewById(R.id.rightContainer)
        rightIcon = findViewById(R.id.rightIcon)
        rightText = findViewById(R.id.rightText)

        setOnClickListener {
            onToggleListener?.invoke(!isActive)
        }
    }

    fun setOnToggleListener(listener: (Boolean) -> Unit) {
        this.onToggleListener = listener
    }

    fun setState(active: Boolean, activeLabel: String, activeIcon: Int, inactiveIcon: Int) {
        this.isActive = active

        val leftParams = leftContainer.layoutParams as LinearLayout.LayoutParams
        val rightParams = rightContainer.layoutParams as LinearLayout.LayoutParams

        if (active) {
            leftParams.weight = 0.6f
            rightParams.weight = 0.4f

            leftContainer.setBackgroundResource(R.drawable.bg_pill_switch_thumb)
            leftText.visibility = VISIBLE
            leftText.text = activeLabel
            leftText.setTextColor(ContextCompat.getColor(context, R.color.periwinkle_dark))
            leftIcon.setImageResource(activeIcon)
            leftIcon.setColorFilter(ContextCompat.getColor(context, R.color.periwinkle_dark))

            rightContainer.background = null
            rightText.visibility = GONE
            rightIcon.setImageResource(inactiveIcon)
            rightIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))
        } else {
            leftParams.weight = 0.4f
            rightParams.weight = 0.6f

            leftContainer.background = null
            leftText.visibility = GONE
            leftIcon.setImageResource(activeIcon)
            leftIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))

            rightContainer.setBackgroundResource(R.drawable.bg_pill_switch_thumb)
            rightText.visibility = VISIBLE
            rightText.text = activeLabel
            rightText.setTextColor(ContextCompat.getColor(context, R.color.periwinkle_dark))
            rightIcon.setImageResource(inactiveIcon)
            rightIcon.setColorFilter(ContextCompat.getColor(context, R.color.periwinkle_dark))
        }

        leftContainer.layoutParams = leftParams
        rightContainer.layoutParams = rightParams
    }
}
