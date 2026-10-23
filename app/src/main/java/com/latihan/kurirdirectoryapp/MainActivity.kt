package com.latihan.kurirdirectoryapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var editTextSearchName: EditText
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var textViewErrorMessage: TextView
    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var textViewEmptyMessage: TextView

    private lateinit var userAdapter: UserAdapter
    private var originalUserList: List<UserResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editTextSearchName = findViewById(R.id.editTextSearchName)
        progressBarLoading = findViewById(R.id.progressBarLoading)
        textViewErrorMessage = findViewById(R.id.textViewErrorMessage)
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
        textViewEmptyMessage = findViewById(R.id.textViewEmptyMessage)

        setupRecyclerView()
        setupSearchListener()
        muatDataPengguna()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter()
        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        recyclerViewUsers.adapter = userAdapter
    }

    private fun muatDataPengguna() {
        progressBarLoading.visibility = View.VISIBLE
        textViewErrorMessage.visibility = View.GONE
        recyclerViewUsers.visibility = View.GONE
        textViewEmptyMessage.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllUsers()
                withContext(Dispatchers.Main) {
                    progressBarLoading.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        originalUserList = response.body()!!
                        if (originalUserList.isEmpty()) {
                            textViewEmptyMessage.visibility = View.VISIBLE
                            recyclerViewUsers.visibility = View.GONE
                        } else {
                            recyclerViewUsers.visibility = View.VISIBLE
                            userAdapter.perbaruiDaftar(originalUserList)
                        }
                    } else {
                        textViewErrorMessage.text = "Gagal memuat data dari server (${response.code()})"
                        textViewErrorMessage.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarLoading.visibility = View.GONE
                    textViewErrorMessage.text = "Gagal terhubung ke server: ${e.localizedMessage}"
                    textViewErrorMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupSearchListener() {
        editTextSearchName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterUsers(query: String) {
        val trimmedQuery = query.trim()
        val daftarTersaring = if (trimmedQuery.isEmpty()) {
            originalUserList
        } else {
            originalUserList.filter { user ->
                user.name.contains(trimmedQuery, ignoreCase = true) ||
                        user.company.companyName.contains(trimmedQuery, ignoreCase = true)
            }
        }

        userAdapter.perbaruiDaftar(daftarTersaring)

        if (daftarTersaring.isEmpty()) {
            recyclerViewUsers.visibility = View.GONE
            textViewEmptyMessage.visibility = View.VISIBLE
        } else {
            recyclerViewUsers.visibility = View.VISIBLE
            textViewEmptyMessage.visibility = View.GONE
        }
    }
}