package com.example.myapp.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.R
import com.example.myapp.screens.api.OrderResponse
import com.example.myapp.screens.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn hình Lịch sử đơn hàng (Order History)
 * Chức năng: Hiển thị danh sách các đơn hàng đã hoàn thành của người dùng.
 * Liên quan đến chức năng: Đặt hàng (Xem lại các đơn đã đặt).
 */
class order_history : AppCompatActivity() {
    private var firstOrderId: Int? = null
    private var secondOrderId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_history)

        // Tải danh sách đơn hàng từ API
        loadOrders()

        // Nút quay lại màn hình trước
        val btnBack: ImageView = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        // Chuyển sang màn hình giỏ hàng
        val btnCart: ImageView = findViewById(R.id.icCart)
        btnCart.setOnClickListener {
            val intent = Intent(this, cart::class.java)
            startActivity(intent)
        }

        // Xem chi tiết đơn hàng thứ nhất trong lịch sử
        val tvViewDetails1: TextView = findViewById(R.id.tvViewDetails1)
        tvViewDetails1.setOnClickListener {
            openOrderDetail(firstOrderId)
        }

        // Xem chi tiết đơn hàng thứ hai trong lịch sử
        val tvViewDetails2: TextView = findViewById(R.id.tvViewDetails2)
        tvViewDetails2.setOnClickListener {
            openOrderDetail(secondOrderId)
        }

        // Chuyển sang màn hình cá nhân
        val icProfile: ImageView = findViewById(R.id.icProfile)
        icProfile.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
        }

        // Quay lại màn hình chính
        val icHome: ImageView = findViewById(R.id.icHome)
        icHome.setOnClickListener {
            val intent = Intent(this, home::class.java)
            startActivity(intent)
        }
    }

    /**
     * Feature: Lấy danh sách đơn hàng của người dùng từ API
     */
    private fun loadOrders() {
        val userId = resolveUserIdForHistory()

        RetrofitClient.apiService.getUserOrders(userId).enqueue(object : Callback<List<OrderResponse>> {
            override fun onResponse(call: Call<List<OrderResponse>>, response: Response<List<OrderResponse>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    // Lọc các đơn hàng đã hoàn thành và sắp xếp theo thời gian mới nhất
                    val sorted = response.body()!!
                        .filter { it.status == "completed" }
                        .sortedByDescending { it.createdat }
                    bindTopTwoOrders(sorted)
                } else {
                    bindTopTwoOrders(emptyList())
                }
            }

            override fun onFailure(call: Call<List<OrderResponse>>, t: Throwable) {
                Toast.makeText(this@order_history, "Không tải được lịch sử đơn", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Hiển thị hai đơn hàng mới nhất lên giao diện
     */
    private fun bindTopTwoOrders(orders: List<OrderResponse>) {
        val tvDate1: TextView = findViewById(R.id.tvDate1)
        val tvDate2: TextView = findViewById(R.id.tvDate2)
        val tvViewDetails1: TextView = findViewById(R.id.tvViewDetails1)
        val tvViewDetails2: TextView = findViewById(R.id.tvViewDetails2)

        val first = orders.getOrNull(0)
        val second = orders.getOrNull(1)

        firstOrderId = first?.id
        secondOrderId = second?.id

        tvDate1.text = first?.let { "${formatDate(it.createdat)} - #${it.id} - ${it.status}" } ?: "Chưa có đơn hàng"
        tvDate2.text = second?.let { "${formatDate(it.createdat)} - #${it.id} - ${it.status}" } ?: "Chưa có đơn hàng"

        tvViewDetails1.alpha = if (first != null) 1f else 0.4f
        tvViewDetails2.alpha = if (second != null) 1f else 0.4f
    }

    /**
     * Mở màn hình chi tiết đơn hàng đã giao
     */
    private fun openOrderDetail(orderId: Int?) {
        if (orderId == null) {
            Toast.makeText(this, "Không có đơn để xem", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, delivered_order_details::class.java)
        intent.putExtra("order_id", orderId)
        startActivity(intent)
    }

    /**
     * Xác định User ID hiện tại từ SharedPreferences để tải đúng lịch sử
     */
    private fun resolveUserIdForHistory(): Int {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        return if (userId > 0) userId else 1 // Trả về 1 làm mặc định nếu chưa đăng nhập (để demo)
    }

    /**
     * Định dạng chuỗi ngày tháng từ API sang định dạng dd/MM/yyyy
     */
    private fun formatDate(raw: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val parsed: Date = parser.parse(raw) ?: return raw
            SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(parsed)
        } catch (e: Exception) {
            raw.take(10)
        }
    }
}
