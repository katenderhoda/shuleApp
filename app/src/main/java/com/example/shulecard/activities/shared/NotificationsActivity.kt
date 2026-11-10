package com.example.shulecard.activities.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.utils.AppNotification
import com.example.shulecard.utils.NotificationCategory
import com.example.shulecard.utils.NotificationHelper
import com.google.android.material.appbar.MaterialToolbar

/**
 * One notifications screen shared by every role, instead of four near-identical copies.
 * Which feed it shows is decided entirely by the intent extras it's launched with.
 */
class NotificationsActivity : AppCompatActivity() {

    enum class Role { ADMIN, PARENT, STUDENT, VENDOR }

    companion object {
        const val EXTRA_ROLE = "role"
        /** Parent only: comma-separated linked student IDs. */
        const val EXTRA_PARENT_EMAIL = "parentEmail"
        const val EXTRA_STUDENT_IDS = "studentIds"
        const val EXTRA_STUDENT_ID = "studentId"
        const val EXTRA_VENDOR_ID = "vendorId"
        const val EXTRA_VENDOR_NAME = "vendorName"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        toolbar = findViewById(R.id.toolbarNotifications)
        rvNotifications = findViewById(R.id.rvNotifications)
        tvEmpty = findViewById(R.id.tvEmptyNotifications)

        toolbar.setNavigationOnClickListener { finish() }
        rvNotifications.layoutManager = LinearLayoutManager(this)

        val notifications = loadNotifications()
        rvNotifications.adapter = NotificationAdapter(notifications)

        rvNotifications.visibility = if (notifications.isEmpty()) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE

        markSeen()
    }

    private fun role(): Role {
        val raw = intent.getStringExtra(EXTRA_ROLE)
        return Role.entries.firstOrNull { it.name == raw } ?: Role.ADMIN
    }

    private fun loadNotifications(): List<AppNotification> = when (role()) {
        Role.ADMIN -> NotificationHelper.getAdminNotifications(this)
        Role.PARENT -> {
            val email = intent.getStringExtra(EXTRA_PARENT_EMAIL).orEmpty()
            val studentIds = intent.getStringExtra(EXTRA_STUDENT_IDS)?.split(",")?.filter { it.isNotBlank() }.orEmpty()
            NotificationHelper.getParentNotifications(this, email, studentIds)
        }
        Role.STUDENT -> {
            val studentId = intent.getStringExtra(EXTRA_STUDENT_ID).orEmpty()
            NotificationHelper.getStudentNotifications(this, studentId)
        }
        Role.VENDOR -> {
            val vendorId = intent.getStringExtra(EXTRA_VENDOR_ID).orEmpty()
            val vendorName = intent.getStringExtra(EXTRA_VENDOR_NAME).orEmpty()
            NotificationHelper.getVendorNotifications(this, vendorId, vendorName)
        }
    }

    private fun markSeen() {
        when (role()) {
            Role.ADMIN -> NotificationHelper.markAdminSeen(this)
            Role.PARENT -> NotificationHelper.markParentSeen(this, intent.getStringExtra(EXTRA_PARENT_EMAIL).orEmpty())
            Role.STUDENT -> NotificationHelper.markStudentSeen(this, intent.getStringExtra(EXTRA_STUDENT_ID).orEmpty())
            Role.VENDOR -> NotificationHelper.markVendorSeen(this, intent.getStringExtra(EXTRA_VENDOR_ID).orEmpty())
        }
    }

    private class NotificationAdapter(
        private val items: List<AppNotification>
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
            val title: TextView = itemView.findViewById(R.id.tvNotificationTitle)
            val message: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.message.text = item.message

            val context = holder.itemView.context
            val (tintRes, iconRes) = when (item.category) {
                NotificationCategory.INFO -> R.color.brand_primary to R.drawable.ic_info
                NotificationCategory.SUCCESS -> R.color.status_success to R.drawable.ic_check
                NotificationCategory.WARNING -> R.color.status_warning to R.drawable.ic_alert
                NotificationCategory.CRITICAL -> R.color.status_danger to R.drawable.ic_alert
            }
            holder.icon.backgroundTintList =
                android.content.res.ColorStateList.valueOf(context.getColor(tintRes))
            holder.icon.setImageResource(iconRes)
        }

        override fun getItemCount(): Int = items.size
    }
}
