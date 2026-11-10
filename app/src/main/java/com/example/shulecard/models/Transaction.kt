package com.example.shulecard.models

data class Transaction(
    val transactionId: String,
    val studentId: String,
    val studentName: String,
    val type: String,     // topup or payment
    val amount: Double,
    val vendor: String,
    val dateTime: String,
    val status: String,   // completed, pending
    val timestamp: Long = System.currentTimeMillis()
)