package com.dailybudget.app.ui

import androidx.annotation.StringRes
import com.dailybudget.app.R

/** Catégories de dépenses fréquentes. [fixed] = cochée « hors budget du jour » par défaut. */
enum class Category(val key: String, val emoji: String, @StringRes val label: Int, val fixed: Boolean = false) {
    GROCERIES("groceries", "🛒", R.string.cat_groceries),
    CAR("car", "🚗", R.string.cat_car),
    SHOPPING("shopping", "🛍️", R.string.cat_shopping),
    FOOD("food", "🍽️", R.string.cat_food),
    TRANSPORT("transport", "🚌", R.string.cat_transport),
    RENT("rent", "🏠", R.string.cat_rent, fixed = true),
    BILLS("bills", "💡", R.string.cat_bills, fixed = true),
    FAMILY("family", "👨‍👩‍👧", R.string.cat_family, fixed = true),
    HEALTH("health", "💊", R.string.cat_health),
    LEISURE("leisure", "🎉", R.string.cat_leisure),
    OTHER("other", "📦", R.string.cat_other);

    companion object {
        fun of(key: String?): Category? = entries.firstOrNull { it.key == key }
    }
}
