package com.example.shulecard.models

/**
 * A paid-for basket of items waiting for the vendor to hand over in person. Created once
 * [Transaction] payment succeeds; the vendor "redeems" it by looking up [orderId] (typed in
 * manually, or decoded from the QR shown to the student) and marking it issued.
 */
data class Order(
    val orderId: String,
    val studentId: String,
    val studentName: String,
    val vendorId: String,
    val vendorName: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val transactionId: String,
    val timestamp: Long,
    val status: String,
    val issuedAt: Long = 0L
) {
    companion object {
        const val STATUS_PENDING = "pending_pickup"
        const val STATUS_ISSUED = "issued"
    }
}
