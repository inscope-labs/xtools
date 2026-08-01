package com.inscopelabs.abx.xtools.ui.store

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.XToolsApplication
import com.inscopelabs.abx.xtools.diagnostics.Logger
import com.inscopelabs.abx.xtools.ui.InputCoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StoreFragment : Fragment() {

    var onPluginClickListener: ((String) -> Unit)? = null

    private lateinit var etSearch: EditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var rvCatalogPlugins: RecyclerView
    private lateinit var adapter: CatalogPluginAdapter

    private var currentQuery: String? = null
    private var currentCategory: String? = null
    private var currentPage = 0
    private var totalCount = 0
    private var isLoading = false
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etSearch)
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup)
        rvCatalogPlugins = view.findViewById(R.id.rvCatalogPlugins)

        adapter = CatalogPluginAdapter { plugin ->
            InputCoordinator.hideKeyboard(requireActivity())
            onPluginClickListener?.invoke(plugin.id)
        }

        val layoutManager = LinearLayoutManager(requireContext())
        rvCatalogPlugins.layoutManager = layoutManager
        rvCatalogPlugins.adapter = adapter

        // Immediate offline-friendly display from cache
        val cached = XToolsApplication.instance.catalogCache.loadCachedSearch()
        if (cached != null && cached.plugins.isNotEmpty()) {
            adapter.submitList(cached.plugins)
        }

        setupSearch()
        setupScrollListener(layoutManager)
        loadCategoriesAndInitialData()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(400)
                    val text = s?.toString()?.trim()
                    currentQuery = if (text.isNullOrEmpty()) null else text
                    performSearch(reset = true)
                }
            }
        })
    }

    private fun setupScrollListener(layoutManager: LinearLayoutManager) {
        rvCatalogPlugins.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || isLoading) return

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (totalItemCount < totalCount && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                    performSearch(reset = false)
                }
            }
        })
    }

    private fun loadCategoriesAndInitialData() {
        lifecycleScope.launch {
            try {
                val categories = XToolsApplication.instance.catalogApi.getCategories()
                populateCategories(categories)

                // Initial search
                performSearch(reset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun populateCategories(categories: List<String>) {
        categoryChipGroup.removeAllViews()

        val allChip = Chip(requireContext()).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                if (currentCategory != null) {
                    currentCategory = null
                    performSearch(reset = true)
                }
            }
        }
        categoryChipGroup.addView(allChip)

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                setOnClickListener {
                    if (currentCategory != category) {
                        currentCategory = category
                        performSearch(reset = true)
                    } else if (!isChecked) {
                        currentCategory = null
                        allChip.isChecked = true
                        performSearch(reset = true)
                    }
                }
            }
            categoryChipGroup.addView(chip)
        }
    }

    private fun performSearch(reset: Boolean) {
        if (isLoading) return
        isLoading = true

        if (reset) {
            currentPage = 0
        } else {
            currentPage++
        }

        Logger.d("StoreFragment", "performSearch: query='$currentQuery', category='$currentCategory', page=$currentPage, reset=$reset")

        lifecycleScope.launch {
            try {
                val result = XToolsApplication.instance.catalogApi.search(
                    query = currentQuery,
                    category = currentCategory,
                    page = currentPage,
                    pageSize = 20
                )

                totalCount = result.totalCount
                Logger.d("StoreFragment", "performSearch success: found ${result.plugins.size} items (total=$totalCount)")

                if (reset) {
                    adapter.submitList(result.plugins)
                    XToolsApplication.instance.catalogCache.cacheSearchResult(result)
                } else {
                    adapter.appendList(result.plugins)
                }
            } catch (e: Exception) {
                Logger.e("StoreFragment", "performSearch error", e)
            } finally {
                isLoading = false
            }
        }
    }
}
