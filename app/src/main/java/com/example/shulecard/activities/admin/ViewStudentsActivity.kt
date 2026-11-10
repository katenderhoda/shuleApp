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
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentProfile
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class ViewStudentsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvNoStudents: TextView
    private lateinit var lvStudents: ListView
    private lateinit var fabAddStudent: FloatingActionButton

    private lateinit var prefsManager: PrefsManager
    private var students: List<StudentProfile> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_students)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarViewStudents)
        tvNoStudents = findViewById(R.id.tvNoStudents)
        lvStudents = findViewById(R.id.lvStudents)
        fabAddStudent = findViewById(R.id.fabAddStudent)

        toolbar.setNavigationOnClickListener { finish() }

        fabAddStudent.setOnClickListener {
            startActivity(Intent(this, RegisterStudentActivity::class.java))
        }

        loadStudents()
    }

    override fun onResume() {
        super.onResume()
        loadStudents()
    }

    private fun loadStudents() {
        students = StudentRegistryHelper.getAllStudents(this).sortedBy { it.studentName }

        if (students.isEmpty()) {
            tvNoStudents.visibility = View.VISIBLE
            lvStudents.visibility = View.GONE
            return
        }

        tvNoStudents.visibility = View.GONE
        lvStudents.visibility = View.VISIBLE

        val rows = students.map { student ->
            "${student.studentName}\n${student.studentId} • ${student.studentClass.ifBlank { "No class set" }}\nParent: ${student.parentName.ifBlank { "N/A" }}"
        }

        lvStudents.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        lvStudents.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            showStudentOptions(students[position])
        }
    }

    private fun showStudentOptions(student: StudentProfile) {
        val options = arrayOf("View Details", "Edit Student", "Delete Student")

        AlertDialog.Builder(this)
            .setTitle(student.studentName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewStudentDetails(student)
                    1 -> editStudent(student)
                    2 -> confirmDeleteStudent(student)
                }
            }
            .show()
    }

    private fun viewStudentDetails(student: StudentProfile) {
        val balance = prefsManager.getBalanceForStudent(student.studentId, student.initialBalance)
        val cardStatus = getSharedPreferences("CardPrefs", MODE_PRIVATE)
            .getString("cardStatus_${student.studentId}", null)
        val hasCard = StudentRegistryHelper.hasIssuedCardForStudent(this, student.studentId)

        val details = """
            Student ID: ${student.studentId}
            Name: ${student.studentName}
            Class: ${student.studentClass.ifBlank { "N/A" }}

            Parent/Guardian: ${student.parentName.ifBlank { "N/A" }}
            Phone: ${student.parentPhone.ifBlank { "N/A" }}
            Email: ${student.parentEmail.ifBlank { "N/A" }}

            Wallet Balance: UGX ${NumberFormat.getNumberInstance(Locale.US).format(balance)}
            Card: ${if (hasCard) (cardStatus ?: "Active") else "Not issued yet"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Student Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun editStudent(student: StudentProfile) {
        startActivity(
            Intent(this, RegisterStudentActivity::class.java).apply {
                putExtra("editStudentId", student.studentId)
            }
        )
    }

    private fun confirmDeleteStudent(student: StudentProfile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete ${student.studentName} (${student.studentId})? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                StudentRegistryHelper.deleteStudent(this, student.studentId)
                Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show()
                loadStudents()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
