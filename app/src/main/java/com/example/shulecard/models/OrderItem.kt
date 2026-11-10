package com.example.shulecard.models

/** One line of a multi-item order — the quantity of a single vendor item a student picked. */
data class OrderItem(
    val itemId: String,
    val name: String,
    val price: Double,
    val quantity: Int
) {
    val lineTotal: Double get() = price * quantity
}
