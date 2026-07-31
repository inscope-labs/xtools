package com.inscopelabs.abx.xtools.ui.category

import java.io.Serializable

data class FeatureSection(
    val title: String,
    val items: List<FeatureItem>
) : Serializable

data class FeatureItem(
    val id: String,
    val title: String,
    val statusText: String,
    val statusIsPositive: Boolean,
    val iconRes: Int
) : Serializable

data class CategoryContent(
    val categoryName: String,
    val sections: List<FeatureSection>,
    val usesCustomContent: Boolean = false
) : Serializable
