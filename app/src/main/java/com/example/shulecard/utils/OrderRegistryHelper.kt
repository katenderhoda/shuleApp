package com.example.shulecard.utils

import android.content.Context
import com.example.shulecard.models.Order
import com.example.shulecard.models.OrderItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Every paid-for order waiting for pickup, stored as one JSON list under "OrderRegistry" —
 * same list-in-SharedPreferences shape as [StudentRegistryHelper]/[VendorItemRegistryHelper].
 */
object OrderRegistryHelper {

    private const val KEY_ORDERS = "orders_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences("OrderRegistry", Context.MODE_PRIVATE)

    fun serializeItems(items: List<OrderItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            jsonArray.put(
                JSONObject().apply {
                    put("itemId", item.itemId)
                    put("name", item.name)
                    put("price", item.price)
                    put("quantity", item.quantity)
                }
            )
        }
        return jsonArray.toString()
    }

    fun parseItems(json: String): List<OrderItem> {
        if (json.isBlank()) return emptyList()
        val jsonArray = JSONArray(json)
        val items = mutableListOf<OrderItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            items.add(
                OrderItem(
                    itemId = obj.optString("itemId"),
                    name = obj.optString("name"),
                    price = obj.optDouble("price", 0.0),
                    quantity = obj.optInt("quantity", 1)
                )
            )
        }

        return items
    }

    fun getAllOrders(context: Context): List<Order> {
        val jsonString = prefs(context).getString(KEY_ORDERS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val orders = mutableListOf<Order>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            orders.add(
                Order(
                    orderId = obj.optString("orderId"),
                    studentId = obj.optString("studentId"),
                    studentName = obj.optString("studentName"),
                    vendorId = obj.optString("vendorId"),
                    vendorName = obj.optString("vendorName"),
                    items = parseItems(obj.optString("items", "[]")),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    transactionId = obj.optString("transactionId"),
                    timestamp = obj.optLong("timestamp", 0L),
                    status = obj.optString("status", Order.STATUS_PENDING),
                    issuedAt = obj.optLong("issuedAt", 0L)
                )
            )
        }

        return orders
    }

    fun saveAllOrders(context: Context, orders: List<Order>) {
        val jsonArray = JSONArray()

        orders.forEach { order ->
            jsonArray.put(
                JSONObject().apply {
                    put("orderId", order.orderId)
                    put("studentId", order.studentId)
                    put("studentName", order.studentName)
                    put("vendorId", order.vendorId)
                    put("vendorName", order.vendorName)
                    put("items", serializeItems(order.items))
                    put("totalAmount", order.totalAmount)
                    put("transactionId", order.transactionId)
                    put("timestamp", order.timestamp)
                    put("status", order.status)
                    put("issuedAt", order.issuedAt)
                }
            )
        }

        prefs(context).edit().putString(KEY_ORDERS, jsonArray.toString()).apply()
    }

    fun getOrderById(context: Context, orderId: String): Order? {
        if (orderId.isBlank()) return null
        return getAllOrders(context).find { it.orderId.equals(orderId.trim(), ignoreCase = true) }
    }

    fun upsertOrder(context: Context, order: Order) {
        val current = getAllOrders(context).toMutableList()
        val index = current.indexOfFirst { it.orderId.equals(order.orderId, ignoreCase = true) }

        if (index >= 0) {
            current[index] = order
        } else {
            current.add(order)
        }

        saveAllOrders(context, current)
    }

    /** Marks an order as handed over to the student. Returns false if it was already issued. */
    fun markIssued(context: Context, orderId: String): Boolean {
        val order = getOrderById(context, orderId) ?: return false
        if (order.status == Order.STATUS_ISSUED) return false

        upsertOrder(context, order.copy(status = Order.STATUS_ISSUED, issuedAt = System.currentTimeMillis()))
        return true
    }

    fun generateOrderId(): String = "ORD-${System.currentTimeMillis()}"
}
