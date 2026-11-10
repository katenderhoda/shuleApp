package com.example.shulecard.models

data class Student(
    val studentId: String,
    val name: String,
    val className: String,
    var balance: Double,
    val cardNumber: String,
    var cardActive: Boolean,
    val parentName: String,
    val parentPhone: String,
    val parentEmail: String
)