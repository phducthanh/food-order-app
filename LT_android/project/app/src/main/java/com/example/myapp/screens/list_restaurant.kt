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
import com.example.myapp.adapters.RestaurantAdapter
import com.example.myapp.screens.api.RetrofitClient
import com.example.myapp.screens.api.Restaurant
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class list_restaurant: AppCompatActivity() {
    private var restaurants: MutableList<Restaurant> = mutableListOf()
    private lateinit var rvRestaurants: RecyclerView
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var edtSearch: EditText
    private var searchJob: Call<List<Restaurant>>? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val SEARCH_DELAY = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_restaurant)

        // Initialize RetrofitClient context if not already done
        RetrofitClient.init(applicationContext)

        val icProfile: ImageView = findViewById(R.id.icProfile)
        icProfile.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }
        val tvMonAn: TextView = findViewById(R.id.tvMonAn)
        tvMonAn.setOnClickListener {
            startActivity(Intent(this, home::class.java))
        }
        val icHome: ImageView = findViewById(R.id.icHome)
        icHome.setOnClickListener {
            startActivity(Intent(this, home::class.java))
        }

        val btnCart: ImageView = findViewById(R.id.icCart)
        btnCart.setOnClickListener {
            startActivity(Intent(this, cart::class.java))
        }
        val imgNotification: ImageView = findViewById(R.id.imgNotification)
        imgNotification.setOnClickListener {
            startActivity(Intent(this, notification::class.java))
        }

        // Setup RecyclerView immediately with an empty list
        rvRestaurants = findViewById(R.id.rvRestaurants)
        rvRestaurants.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        restaurantAdapter = RestaurantAdapter(this, restaurants)
        rvRestaurants.adapter = restaurantAdapter

        // Setup Search
        edtSearch = findViewById(R.id.edtSearch)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val query = s?.toString()?.trim() ?: ""
                    if (query.isEmpty()) {
                        fetchRestaurants()
                    } else {
                        searchRestaurants(query)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, SEARCH_DELAY)
            }
        })

        fetchRestaurants()
    }

    private fun fetchRestaurants() {
        searchJob?.cancel()
        Log.d("API_DEBUG", "Fetching restaurants...")
        
        RetrofitClient.apiService.getRestaurants().enqueue(object : Callback<List<Restaurant>> {
            override fun onResponse(call: Call<List<Restaurant>>, response: Response<List<Restaurant>>) {
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    Log.d("API_DEBUG", "Success: Received ${results.size} restaurants")
                    restaurants.clear()
                    restaurants.addAll(results)
                    restaurantAdapter.notifyDataSetChanged()
                    
                    if (results.isEmpty()) {
                        Toast.makeText(this@list_restaurant, "Không có nhà hàng nào", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("API_DEBUG", "Error: ${response.code()} ${response.message()}")
                    Toast.makeText(this@list_restaurant, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Restaurant>>, t: Throwable) {
                if (!call.isCanceled) {
                    Log.e("API_DEBUG", "Failure: ${t.message}", t)
                    Toast.makeText(this@list_restaurant, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun searchRestaurants(query: String) {
        searchJob?.cancel()
        RetrofitClient.apiService.searchRestaurantsByName(query).enqueue(object : Callback<List<Restaurant>> {
            override fun onResponse(call: Call<List<Restaurant>>, response: Response<List<Restaurant>>) {
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    restaurants.clear()
                    restaurants.addAll(results)
                    restaurantAdapter.notifyDataSetChanged()
                    if (results.isEmpty()) {
                        Toast.makeText(this@list_restaurant, "Không tìm thấy nhà hàng", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@list_restaurant, "Lỗi khi tìm kiếm", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Restaurant>>, t: Throwable) {
                if (!call.isCanceled) {
                    Toast.makeText(this@list_restaurant, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
