package com.inscopelabs.abx.xtools.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inscopelabs.abx.xtools.R

class CategoryFragment : Fragment() {

    private var categoryContents: ArrayList<CategoryContent> = arrayListOf()
    private var adapter: SectionedFeatureAdapter? = null

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
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        adapter = SectionedFeatureAdapter { featureItem ->
            // Feature row click callback - wired in prompt 3
        }

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
            adapter?.setSections(content.sections)
        }
    }
}
