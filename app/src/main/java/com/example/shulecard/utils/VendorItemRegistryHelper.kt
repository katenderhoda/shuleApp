package com.example.shulecard.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class VendorItem(
    val itemId: String,
    val vendorId: String,
    val name: String,
    val price: Double,
    val description: String = "",
    val available: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Every vendor's menu/product catalog, stored as one JSON list (each item tagged with its
 * vendorId) under "VendorItemRegistry" — same list-in-SharedPreferences shape as
 * [StudentRegistryHelper]/[ParentRegistryHelper].
 */
object VendorItemRegistryHelper {

    private const val KEY_ITEMS = "items_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences("VendorItemRegistry", Context.MODE_PRIVATE)

    fun getAllItems(context: Context): List<VendorItem> {
        val jsonString = prefs(context).getString(KEY_ITEMS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val items = mutableListOf<VendorItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            items.add(
                VendorItem(
                    itemId = obj.optString("itemId"),
                    vendorId = obj.optString("vendorId"),
                    name = obj.optString("name"),
                    price = obj.optDouble("price", 0.0),
                    description = obj.optString("description"),
                    available = obj.optBoolean("available", true),
                    createdAt = obj.optLong("createdAt", 0L)
                )
            )
        }

        return items
    }

    fun saveAllItems(context: Context, items: List<VendorItem>) {
        val jsonArray = JSONArray()

        items.forEach { item ->
            val jsonObject = JSONObject().apply {
                put("itemId", item.itemId)
                put("vendorId", item.vendorId)
                put("name", item.name)
                put("price", item.price)
                put("description", item.description)
                put("available", item.available)
                put("createdAt", item.createdAt)
            }
            jsonArray.put(jsonObject)
        }

        prefs(context).edit().putString(KEY_ITEMS, jsonArray.toString()).apply()
    }

    fun getItemsForVendor(context: Context, vendorId: String): List<VendorItem> {
        return getAllItems(context)
            .filter { it.vendorId.equals(vendorId, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
    }

    fun getAvailableItemsForVendor(context: Context, vendorId: String): List<VendorItem> {
        return getItemsForVendor(context, vendorId).filter { it.available }
    }

    fun getItemById(context: Context, itemId: String): VendorItem? {
        return getAllItems(context).find { it.itemId.equals(itemId, ignoreCase = true) }
    }

    /** Adds a new item, or replaces the existing one with the same itemId. */
    fun upsertItem(context: Context, item: VendorItem) {
        val current = getAllItems(context).toMutableList()
        val index = current.indexOfFirst { it.itemId.equals(item.itemId, ignoreCase = true) }

        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }

        saveAllItems(context, current)
    }

    fun deleteItem(context: Context, itemId: String) {
        val current = getAllItems(context).filterNot { it.itemId.equals(itemId, ignoreCase = true) }
        saveAllItems(context, current)
    }

    fun generateItemId(): String = "ITM-${System.currentTimeMillis()}"
}
