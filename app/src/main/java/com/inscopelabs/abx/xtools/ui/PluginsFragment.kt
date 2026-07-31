package com.inscopelabs.abx.xtools.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.XToolsApplication
import com.inscopelabs.abx.xtools.kernel.registry.PluginState
import com.inscopelabs.abx.xtools.ui.catalogdetail.CatalogDetailFragment
import com.inscopelabs.abx.xtools.ui.category.CategoryContent
import com.inscopelabs.abx.xtools.ui.category.CategoryFragment
import com.inscopelabs.abx.xtools.ui.category.FeatureItem
import com.inscopelabs.abx.xtools.ui.category.FeatureSection
import com.inscopelabs.abx.xtools.ui.feature.FeatureFragment
import com.inscopelabs.abx.xtools.ui.plugindetail.PluginDetailFragment

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
            childFragment.onFeatureLongClickListener = { item ->
                item.registryId?.let { openPluginDetail(it) }
            }
            childFragment.onPluginClickListener = { pluginId ->
                openCatalogDetail(pluginId)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val installedPluginItems = XToolsApplication.instance.pluginRegistry.getAllPlugins().map { entry ->
                FeatureItem(
                    id = entry.installationPath,
                    registryId = entry.id,
                    title = entry.manifest.name,
                    statusText = "${if (entry.state == PluginState.ACTIVE) "Running" else "Inactive"} • v${entry.version}",
                    statusIsPositive = entry.state == PluginState.ACTIVE,
                    iconRes = R.drawable.ic_plugins
                )
            }

            val pluginsData = arrayListOf(
                CategoryContent(
                    categoryName = "Active",
                    sections = listOf(
                        FeatureSection(
                            title = "Installed Plugins",
                            items = installedPluginItems
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
                ),
                CategoryContent(
                    categoryName = "Store",
                    sections = emptyList(),
                    usesCustomContent = true
                )
            )

            val categoryFragment = CategoryFragment.newInstance(pluginsData)
            categoryFragment.onFeatureClickListener = { featureItem ->
                openFeature(featureItem)
            }
            categoryFragment.onFeatureLongClickListener = { item ->
                item.registryId?.let { openPluginDetail(it) }
            }
            categoryFragment.onPluginClickListener = { pluginId ->
                openCatalogDetail(pluginId)
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

    private fun openCatalogDetail(pluginId: String) {
        val detailFragment = CatalogDetailFragment.newInstance(pluginId)
        childFragmentManager.beginTransaction()
            .replace(R.id.childContainer, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openPluginDetail(pluginId: String) {
        val detailFragment = PluginDetailFragment.newInstance(pluginId)
        childFragmentManager.beginTransaction()
            .replace(R.id.childContainer, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}
