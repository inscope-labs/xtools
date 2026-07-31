package com.inscopelabs.abx.xtools.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.diagnostics.Logger
import com.inscopelabs.abx.xtools.ui.store.StoreFragment

class CategoryFragment : Fragment() {

    private var categoryContents: ArrayList<CategoryContent> = arrayListOf()
    private var adapter: SectionedFeatureAdapter? = null
    var onFeatureClickListener: ((FeatureItem) -> Unit)? = null
    var onFeatureLongClickListener: ((FeatureItem) -> Unit)? = null
    var onPluginClickListener: ((String) -> Unit)? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var customContentContainer: FrameLayout

    companion object {
        private const val ARG_CATEGORIES = "arg_categories"

        fun newInstance(categories: ArrayList<CategoryContent>): CategoryFragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CATEGORIES, categories)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        val data = arguments?.getSerializable(ARG_CATEGORIES) as? ArrayList<CategoryContent>
        categoryContents = data ?: arrayListOf()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (childFragment is StoreFragment) {
            childFragment.onPluginClickListener = { pluginId ->
                onPluginClickListener?.invoke(pluginId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabBar = view.findViewById<CategoryTabBar>(R.id.categoryTabBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        customContentContainer = view.findViewById(R.id.customContentContainer)

        adapter = SectionedFeatureAdapter(
            onFeatureClick = { featureItem -> onFeatureClickListener?.invoke(featureItem) },
            onFeatureLongClick = { featureItem -> onFeatureLongClickListener?.invoke(featureItem) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val categoryNames = categoryContents.map { it.categoryName }
        tabBar.setTabs(categoryNames) { selectedIndex ->
            showCategory(selectedIndex)
        }

        if (categoryContents.isNotEmpty()) {
            tabBar.setSelected(0)
            showCategory(0)
        }
    }

    private fun showCategory(index: Int) {
        if (index in categoryContents.indices) {
            val content = categoryContents[index]
            Logger.d("CategoryFragment", "Selected category index=$index, name='${content.categoryName}'")
            if (content.usesCustomContent) {
                recyclerView.visibility = View.GONE
                customContentContainer.visibility = View.VISIBLE

                val existing = childFragmentManager.findFragmentById(R.id.customContentContainer)
                if (existing == null) {
                    val storeFragment = StoreFragment()
                    storeFragment.onPluginClickListener = { pluginId ->
                        onPluginClickListener?.invoke(pluginId)
                    }
                    childFragmentManager.beginTransaction()
                        .add(R.id.customContentContainer, storeFragment)
                        .commit()
                } else if (existing is StoreFragment) {
                    existing.onPluginClickListener = { pluginId ->
                        onPluginClickListener?.invoke(pluginId)
                    }
                }
            } else {
                customContentContainer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter?.setSections(content.sections)
            }
        }
    }
}
