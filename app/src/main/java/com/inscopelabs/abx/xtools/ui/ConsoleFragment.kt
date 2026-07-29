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

class ConsoleFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_console_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val consoleData = arrayListOf(
                CategoryContent(
                    categoryName = "Logs",
                    sections = listOf(
                        FeatureSection(
                            title = "System Diagnostics",
                            items = listOf(
                                FeatureItem(
                                    id = "console-logs",
                                    title = "Console Logs Stream",
                                    statusText = "Connected • 120 msgs/s",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_catalog
                                ),
                                FeatureItem(
                                    id = "crash-reports",
                                    title = "Crash & Diagnostics Logs",
                                    statusText = "0 errors reported",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_catalog
                                )
                            )
                        )
                    )
                ),
                CategoryContent(
                    categoryName = "SQL DB",
                    sections = listOf(
                        FeatureSection(
                            title = "Storage & Database",
                            items = listOf(
                                FeatureItem(
                                    id = "sqlite-crud",
                                    title = "SQLite Database CRUD",
                                    statusText = "Active connection",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_catalog
                                ),
                                FeatureItem(
                                    id = "db-inspector",
                                    title = "Schema Inspector",
                                    statusText = "4 tables indexed",
                                    statusIsPositive = true,
                                    iconRes = R.drawable.ic_catalog
                                )
                            )
                        )
                    )
                )
            )

            childFragmentManager.beginTransaction()
                .replace(R.id.childContainer, CategoryFragment.newInstance(consoleData))
                .commit()
        }
    }
}
