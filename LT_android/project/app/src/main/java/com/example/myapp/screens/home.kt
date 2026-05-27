package com.example.myapp.screens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapters.MenuItemAdapter
import com.example.myapp.screens.api.MenuItem
import com.example.myapp.screens.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Màn hình Chính (Home)
 * Chức năng: Hiển thị danh sách món ăn, tìm kiếm và điều hướng đến các chức năng khác như Giỏ hàng, Hồ sơ.
 */
class home : AppCompatActivity() {
    private var menuItems: MutableList<MenuItem> = mutableListOf()
    private lateinit var rvMenuItems: RecyclerView
    private lateinit var menuItemAdapter: MenuItemAdapter
    private lateinit var edtSearch: EditText
    private var searchJob: Call<List<MenuItem>>? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val SEARCH_DELAY = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        // Mở màn hình Hồ sơ cá nhân
        val btnProfile: ImageView = findViewById(R.id.icProfile)
        btnProfile.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }
        
        // Mở danh sách nhà hàng
        val tvDanhSachNhaHang: TextView = findViewById(R.id.tvDanhSachNhaHang)
        tvDanhSachNhaHang.setOnClickListener {
            startActivity(Intent(this, list_restaurant::class.java))
        }

        // Mở màn hình Thông báo
        val btnNotification: ImageView = findViewById(R.id.imgNotification)
        btnNotification.setOnClickListener {
            startActivity(Intent(this, notification::class.java))
        }

        // Feature: Mở màn hình Giỏ hàng (Cart)
        val btnCart: ImageView = findViewById(R.id.icCart)
        btnCart.setOnClickListener {
            startActivity(Intent(this, cart::class.java))
        }

        // Thiết lập danh sách hiển thị món ăn
        rvMenuItems = findViewById(R.id.rvMenuItems)
        rvMenuItems.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        menuItemAdapter = MenuItemAdapter(this, menuItems)
        rvMenuItems.adapter = menuItemAdapter

        // Thiết lập chức năng Tìm kiếm món ăn
        edtSearch = findViewById(R.id.edtSearch)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Debounce search để tránh gọi API quá nhiều lần khi người dùng đang nhập
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val query = s?.toString()?.trim() ?: ""
                    if (query.isEmpty()) {
                        fetchMenuItems()
                    } else {
                        searchMenuItems(query)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, SEARCH_DELAY)
            }
        })

        // Tải danh sách món ăn mặc định khi vào app
        fetchMenuItems()
    }

    /**
     * Feature: Tải toàn bộ danh sách món ăn từ API
     */
    private fun fetchMenuItems() {
        searchJob?.cancel()
        RetrofitClient.apiService.getAllMenuItems().enqueue(object : Callback<List<MenuItem>> {
            override fun onResponse(call: Call<List<MenuItem>>, response: Response<List<MenuItem>>) {
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    menuItems.clear()
                    menuItems.addAll(results)
                    menuItemAdapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<List<MenuItem>>, t: Throwable) {
                if (!call.isCanceled) {
                    Toast.makeText(this@home, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * Feature: Tìm kiếm món ăn theo tên qua API
     */
    private fun searchMenuItems(query: String) {
        searchJob?.cancel()
        RetrofitClient.apiService.searchMenuItemsByName(query).enqueue(object : Callback<List<MenuItem>> {
            override fun onResponse(call: Call<List<MenuItem>>, response: Response<List<MenuItem>>) {
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    if (results.isNotEmpty()) {
                        menuItems.clear()
                        menuItems.addAll(results)
                        menuItemAdapter.notifyDataSetChanged()
                    } else {
                        // Nếu không tìm thấy theo tên, thử tìm theo danh mục
                        searchByCategory(query)
                    }
                }
            }
            override fun onFailure(call: Call<List<MenuItem>>, t: Throwable) {}
        })
    }

    /**
     * Feature: Tìm kiếm món ăn theo danh mục qua API
     */
    private fun searchByCategory(query: String) {
        RetrofitClient.apiService.searchMenuItemsByCategory(query).enqueue(object : Callback<List<MenuItem>> {
            override fun onResponse(call: Call<List<MenuItem>>, response: Response<List<MenuItem>>) {
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    menuItems.clear()
                    menuItems.addAll(results)
                    menuItemAdapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<List<MenuItem>>, t: Throwable) {}
        })
    }
}
