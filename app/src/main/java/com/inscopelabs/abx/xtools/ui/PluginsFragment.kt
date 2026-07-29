package com.inscopelabs.abx.xtools.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.ui.category.CategoryContent
import com.inscopelabs.abx.xtools.ui.category.CategoryFragment
import com.inscopelabs.abx.xtools.ui.category.FeatureItem
import com.inscopelabs.abx.xtools.ui.category.FeatureSection
import com.inscopelabs.abx.xtools.ui.feature.FeatureFragment

class PluginsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plugins_container, container, false)
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (childFragment is CategoryFragment) {
            childFragment.onFeatureClickListener = { featureItem ->
                openFeature(featureItem)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val pluginsData = arrayListOf(
                CategoryContent(
                    categoryName = "Active",
                    sections = listOf(
                        FeatureSection(
                            title = "Installed Plugins",
                            items = listOf(
                                FeatureItem(
                                    id = "database",
                                    title = "SQLite Database CRUD",
                                    statusText = "Running • v1.0.0",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_plugins
                                ),
                                FeatureItem(
                                    id = "system-info",
                                    title = "System Information",
                                    statusText = "Running • v1.0.0",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_plugins
                                ),
                                FeatureItem(
                                    id = "sample",
                                    title = "Sample Plugin",
                                    statusText = "Ready • v1.0.0",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_plugins
                                )
                            )
                        )
                    )
                ),
                CategoryContent(
                    categoryName = "Settings",
                    sections = listOf(
                        FeatureSection(
                            title = "General",
                            items = listOf(
                                FeatureItem(
                                    id = "plugin-security",
                                    title = "Plugin Security & Permissions",
                                    statusText = "Enforced",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_plugins
                                ),
                                FeatureItem(
                                    id = "storage-quota",
                                    title = "Storage Quotas",
                                    statusText = "Unrestricted",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_plugins
                                )
                            )
                        )
                    )
                ),
                CategoryContent(
                    categoryName = "Dashboard",
                    sections = listOf(
                        FeatureSection(
                            title = "Overview",
                            items = listOf(
                                FeatureItem(
                                    id = "active-count",
                                    title = "Active Plugins Overview",
                                    statusText = "3 plugins loaded",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_plugins
                                )
                            )
                        )
                    )
                )
            )

            val categoryFragment = CategoryFragment.newInstance(pluginsData)
            categoryFragment.onFeatureClickListener = { featureItem ->
                openFeature(featureItem)
            }

            childFragmentManager.beginTransaction()
                .replace(R.id.childContainer, categoryFragment)
                .commit()
        }
    }

    private fun openFeature(item: FeatureItem) {
        val featureFragment = FeatureFragment.newInstance(item.id, item.title)
        childFragmentManager.beginTransaction()
            .replace(R.id.childContainer, featureFragment)
            .addToBackStack(null)
            .commit()
    }
}
