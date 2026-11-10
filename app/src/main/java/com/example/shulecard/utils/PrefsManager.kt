package com.example.shulecard.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.shulecard.models.Transaction
import org.json.JSONArray
import org.json.JSONObject

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ShuleCardPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TRANSACTIONS = "transactions_json"
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("isLoggedIn", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("isLoggedIn", false)
    }

    fun saveUserRole(role: String) {
        prefs.edit().putString("userRole", role).apply()
    }

    fun getUserRole(): String {
        return prefs.getString("userRole", "") ?: ""
    }

    fun saveUserName(name: String) {
        prefs.edit().putString("userName", name).apply()
    }

    fun getUserName(): String {
        return prefs.getString("userName", "User") ?: "User"
    }

    /** Email of the logged-in user, currently only populated for the parent role. */
    fun saveUserEmail(email: String) {
        prefs.edit().putString("userEmail", email).apply()
    }

    fun getUserEmail(): String {
        return prefs.getString("userEmail", "") ?: ""
    }

    fun saveStudentId(studentId: String) {
        prefs.edit().putString("studentId", studentId).apply()
    }

    fun getStudentId(): String {
        return prefs.getString("studentId", "STU001") ?: "STU001"
    }

    fun saveVendorId(vendorId: String) {
        prefs.edit().putString("vendorId", vendorId).apply()
    }

    fun getVendorId(): String {
        return prefs.getString("vendorId", "") ?: ""
    }

    fun hasBalanceForStudent(studentId: String): Boolean {
        return prefs.contains("balance_$studentId")
    }

    fun saveBalanceForStudent(studentId: String, balance: Float) {
        prefs.edit().putFloat("balance_$studentId", balance).apply()
    }

    fun getBalanceForStudent(studentId: String, defaultBalance: Float): Float {
        return prefs.getFloat("balance_$studentId", defaultBalance)
    }

    fun addTransaction(transaction: Transaction) {
        val current = getAllTransactions().toMutableList()
        current.add(0, transaction) // newest first
        saveAllTransactions(current)
    }

    fun saveAllTransactions(transactions: List<Transaction>) {
        val jsonArray = JSONArray()

        transactions.forEach { transaction ->
            val jsonObject = JSONObject().apply {
                put("transactionId", transaction.transactionId)
                put("studentId", transaction.studentId)
                put("studentName", transaction.studentName)
                put("type", transaction.type)
                put("amount", transaction.amount)
                put("vendor", transaction.vendor)
                put("dateTime", transaction.dateTime)
                put("status", transaction.status)
                put("timestamp", transaction.timestamp)
            }
            jsonArray.put(jsonObject)
        }

        prefs.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
    }

    fun getAllTransactions(): List<Transaction> {
        val jsonString = prefs.getString(KEY_TRANSACTIONS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val transactions = mutableListOf<Transaction>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            transactions.add(
                Transaction(
                    transactionId = obj.optString("transactionId"),
                    studentId = obj.optString("studentId"),
                    studentName = obj.optString("studentName"),
                    type = obj.optString("type"),
                    amount = obj.optDouble("amount"),
                    vendor = obj.optString("vendor"),
                    dateTime = obj.optString("dateTime"),
                    status = obj.optString("status"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        return transactions
    }

    fun clearTransactions() {
        prefs.edit().remove(KEY_TRANSACTIONS).apply()
    }

    /** Updates the status of an existing transaction (e.g. a pending top-up being approved/rejected). */
    fun updateTransactionStatus(transactionId: String, newStatus: String) {
        val updated = getAllTransactions().map { transaction ->
            if (transaction.transactionId == transactionId) {
                transaction.copy(status = newStatus)
            } else {
                transaction
            }
        }
        saveAllTransactions(updated)
    }

    fun logout() {
        prefs.edit()
            .remove("isLoggedIn")
            .remove("userRole")
            .remove("userName")
            .remove("userEmail")
            .remove("studentId")
            .remove("vendorId")
            .apply()
    }
}