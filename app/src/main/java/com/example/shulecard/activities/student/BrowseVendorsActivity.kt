package com.example.shulecard.activities.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.adapters.VendorListAdapter
import com.example.shulecard.adapters.VendorListEntry
import com.google.android.material.appbar.MaterialToolbar

class BrowseVendorsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvNoVendors: View
    private lateinit var rvVendors: RecyclerView
    private lateinit var vendorAdapter: VendorListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_vendors)

        toolbar = findViewById(R.id.toolbarBrowseVendors)
        tvNoVendors = findViewById(R.id.tvNoVendorsToBrowse)
        rvVendors = findViewById(R.id.rvBrowseVendors)

        toolbar.setNavigationOnClickListener { finish() }

        rvVendors.layoutManager = LinearLayoutManager(this)
        vendorAdapter = VendorListAdapter(emptyList()) { vendor ->
            startActivity(
                Intent(this, VendorMenuActivity::class.java).apply {
                    putExtra("vendorId", vendor.vendorId)
                    putExtra("vendorName", vendor.vendorName)
                }
            )
        }
        rvVendors.adapter = vendorAdapter

        loadVendors()
    }

    override fun onResume() {
        super.onResume()
        loadVendors()
    }

    private fun loadVendors() {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val vendorIds = vendorPrefs.getString("vendorIds", "") ?: ""
        val idList = if (vendorIds.isEmpty()) emptyList() else vendorIds.split(",")

        val vendors = idList.mapNotNull { id ->
            val status = vendorPrefs.getString("vendorStatus_$id", "Active") ?: "Active"
            if (!status.equals("Active", ignoreCase = true)) return@mapNotNull null

            VendorListEntry(
                vendorId = id,
                vendorName = vendorPrefs.getString("vendorName_$id", "Vendor") ?: "Vendor",
                category = vendorPrefs.getString("vendorCategory_$id", "") ?: "",
                location = vendorPrefs.getString("vendorLocation_$id", "") ?: ""
            )
        }

        vendorAdapter.updateList(vendors)
        tvNoVendors.visibility = if (vendors.isEmpty()) View.VISIBLE else View.GONE
        rvVendors.visibility = if (vendors.isEmpty()) View.GONE else View.VISIBLE
    }
}
