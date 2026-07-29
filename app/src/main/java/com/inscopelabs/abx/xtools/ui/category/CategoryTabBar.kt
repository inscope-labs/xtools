package com.inscopelabs.abx.xtools.ui.category

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.inscopelabs.abx.xtools.R

class CategoryTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tabScrollView: HorizontalScrollView
    private val tabContainer: LinearLayout
    private val moreButton: ImageButton

    private var labels: List<String> = emptyList()
    private var onSelectedListener: ((Int) -> Unit)? = null
    private var selectedIndex: Int = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.view_category_tab_bar, this, true)

        tabScrollView = findViewById(R.id.tabScrollView)
        tabContainer = findViewById(R.id.tabContainer)
        moreButton = findViewById(R.id.moreButton)

        moreButton.setOnClickListener {
            showOverflowMenu()
        }
    }

    fun setTabs(labels: List<String>, onSelected: (Int) -> Unit) {
        this.labels = labels
        this.onSelectedListener = onSelected
        this.selectedIndex = 0

        tabContainer.removeAllViews()

        val dp16 = (16 * context.resources.displayMetrics.density).toInt()

        labels.forEachIndexed { index, label ->
            val textView = TextView(context).apply {
                text = label
                textSize = 14f
                setPadding(dp16, 0, dp16, 0)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                setOnClickListener {
                    setSelected(index)
                    onSelectedListener?.invoke(index)
                }
            }
            tabContainer.addView(textView)
        }

        setSelected(0)
        checkOverflow()
    }

    fun setSelected(index: Int) {
        if (index !in labels.indices) return
        this.selectedIndex = index

        for (i in 0 until tabContainer.childCount) {
            val child = tabContainer.getChildAt(i) as? TextView ?: continue
            if (i == index) {
                child.setBackgroundResource(R.drawable.bg_category_tab_selected)
                child.setTextColor(ContextCompat.getColor(context, R.color.periwinkle_dark))
            } else {
                child.setBackgroundResource(R.drawable.bg_category_tab_unselected)
                child.setTextColor(ContextCompat.getColor(context, R.color.periwinkle_text))
            }
        }
    }

    private fun checkOverflow() {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                val totalTabWidth = tabContainer.measuredWidth
                val availableWidth = tabScrollView.measuredWidth
                if (totalTabWidth > availableWidth && labels.size > 1) {
                    moreButton.visibility = VISIBLE
                } else {
                    moreButton.visibility = GONE
                }
            }
        })
    }

    private fun showOverflowMenu() {
        val popup = PopupMenu(context, moreButton)
        labels.forEachIndexed { index, label ->
            popup.menu.add(0, index, index, label)
        }
        popup.setOnMenuItemClickListener { menuItem ->
            val index = menuItem.itemId
            setSelected(index)
            onSelectedListener?.invoke(index)
            val child = tabContainer.getChildAt(index)
            if (child != null) {
                tabScrollView.smoothScrollTo(child.left, 0)
            }
            true
        }
        popup.show()
    }
}
