package com.example.shulecard.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ViewVendorsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvNoVendors: TextView
    private lateinit var lvVendors: ListView
    private lateinit var fabAddVendor: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_vendors)

        toolbar = findViewById(R.id.toolbarViewVendors)
        tvNoVendors = findViewById(R.id.tvNoVendors)
        lvVendors = findViewById(R.id.lvVendors)
        fabAddVendor = findViewById(R.id.fabAddVendor)

        toolbar.setNavigationOnClickListener { finish() }

        fabAddVendor.setOnClickListener {
            startActivity(Intent(this, RegisterVendorActivity::class.java))
        }

        loadVendors()
    }

    override fun onResume() {
        super.onResume()
        loadVendors()
    }

    private fun loadVendors() {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val vendorIds = vendorPrefs.getString("vendorIds", "") ?: ""

        if (vendorIds.isEmpty()) {
            tvNoVendors.visibility = View.VISIBLE
            lvVendors.visibility = View.GONE
            return
        }

        val idList = vendorIds.split(",")
        val vendorList = mutableListOf<String>()

        for (id in idList) {
            val name = vendorPrefs.getString("vendorName_$id", "Unknown") ?: "Unknown"
            val category = vendorPrefs.getString("vendorCategory_$id", "N/A") ?: "N/A"
            val status = vendorPrefs.getString("vendorStatus_$id", "Active") ?: "Active"
            val contactPerson = vendorPrefs.getString("vendorContactPerson_$id", "N/A") ?: "N/A"

            vendorList.add("$name\n$category • $status\nContact: $contactPerson")
        }

        if (vendorList.isEmpty()) {
            tvNoVendors.visibility = View.VISIBLE
            lvVendors.visibility = View.GONE
        } else {
            tvNoVendors.visibility = View.GONE
            lvVendors.visibility = View.VISIBLE

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                vendorList
            )
            lvVendors.adapter = adapter

            lvVendors.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val vendorId = idList[position]
                showVendorOptions(vendorId)
            }
        }
    }

    private fun showVendorOptions(vendorId: String) {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val vendorName = vendorPrefs.getString("vendorName_$vendorId", "Unknown") ?: "Unknown"
        val status = vendorPrefs.getString("vendorStatus_$vendorId", "Active") ?: "Active"

        val options = arrayOf(
            "View Details",
            if (status == "Active") "Deactivate Vendor" else "Activate Vendor",
            "Delete Vendor"
        )

        AlertDialog.Builder(this)
            .setTitle(vendorName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewVendorDetails(vendorId)
                    1 -> toggleVendorStatus(vendorId)
                    2 -> confirmDeleteVendor(vendorId)
                }
            }
            .show()
    }

    private fun viewVendorDetails(vendorId: String) {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)

        val name = vendorPrefs.getString("vendorName_$vendorId", "N/A")
        val category = vendorPrefs.getString("vendorCategory_$vendorId", "N/A")
        val contactPerson = vendorPrefs.getString("vendorContactPerson_$vendorId", "N/A")
        val phone = vendorPrefs.getString("vendorContactPhone_$vendorId", "N/A")
        val email = vendorPrefs.getString("vendorContactEmail_$vendorId", "N/A")
        val location = vendorPrefs.getString("vendorLocation_$vendorId", "N/A")
        val status = vendorPrefs.getString("vendorStatus_$vendorId", "Active")

        val details = """
            Vendor ID: $vendorId
            Name: $name
            Category: $category
            Status: $status

            Contact Person: $contactPerson
            Phone: $phone
            Email: $email
            Location: $location
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Vendor Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleVendorStatus(vendorId: String) {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val currentStatus = vendorPrefs.getString("vendorStatus_$vendorId", "Active") ?: "Active"
        val newStatus = if (currentStatus == "Active") "Inactive" else "Active"

        vendorPrefs.edit()
            .putString("vendorStatus_$vendorId", newStatus)
            .apply()

        Toast.makeText(this, "Vendor status updated to $newStatus", Toast.LENGTH_SHORT).show()
        loadVendors()
    }

    private fun confirmDeleteVendor(vendorId: String) {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val vendorName = vendorPrefs.getString("vendorName_$vendorId", "Unknown") ?: "Unknown"

        AlertDialog.Builder(this)
            .setTitle("Delete Vendor")
            .setMessage("Are you sure you want to delete $vendorName? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteVendor(vendorId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVendor(vendorId: String) {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val editor = vendorPrefs.edit()

        // Remove all vendor data
        editor.remove("vendorName_$vendorId")
        editor.remove("vendorPasswordHash_$vendorId")
        editor.remove("vendorSecurityQuestion_$vendorId")
        editor.remove("vendorSecurityAnswerHash_$vendorId")
        editor.remove("vendorCategory_$vendorId")
        editor.remove("vendorContactPerson_$vendorId")
        editor.remove("vendorContactPhone_$vendorId")
        editor.remove("vendorContactEmail_$vendorId")
        editor.remove("vendorLocation_$vendorId")
        editor.remove("vendorStatus_$vendorId")
        editor.remove("vendorRegistrationDate_$vendorId")

        // Update vendor IDs list
        val vendorIds = vendorPrefs.getString("vendorIds", "") ?: ""
        val updatedIds = vendorIds.split(",")
            .filter { it != vendorId }
            .joinToString(",")

        editor.putString("vendorIds", updatedIds)
        editor.apply()

        Toast.makeText(this, "Vendor deleted successfully", Toast.LENGTH_SHORT).show()
        loadVendors()
    }
}