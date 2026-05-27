package com.example.myapp.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import android.widget.Toast
import com.example.myapp.R
import com.example.myapp.screens.api.OrderResponse
import com.example.myapp.screens.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Màn hình Chi tiết điểm thưởng (Points Detail)
 * Chức năng: Hiển thị điểm tích lũy từ các đơn hàng đã hoàn thành và cho phép đi đến chi tiết đánh giá.
 * Liên quan đến chức năng: Đặt hàng.
 */
class PointsDetail : AppCompatActivity() {
    private var firstOrderId: Int? = null
    private var secondOrderId: Int? = null
    private var thirdOrderId: Int? = null
    private var fourthOrderId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.points_detail)

        // Tải danh sách đơn hàng để tính toán điểm
        loadOrders()

        // Nút quay lại
        val btnBack: ImageView = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        // Thiết lập sự kiện click cho các thẻ điểm thưởng
        val pointsCard1: CardView = findViewById(R.id.pointsCard1)
        val tvDishName1: TextView = findViewById(R.id.tvDishName1)
        pointsCard1.setOnClickListener {
            navigateToOrderDetail(tvDishName1.text.toString(), firstOrderId)
        }

        val pointsCard2: CardView = findViewById(R.id.pointsCard2)
        val tvDishName2: TextView = findViewById(R.id.tvDishName2)
        pointsCard2.setOnClickListener {
            navigateToOrderDetail(tvDishName2.text.toString(), secondOrderId)
        }

        val pointsCard3: CardView = findViewById(R.id.pointsCard3)
        val tvDishName3: TextView = findViewById(R.id.tvDishName3)
        pointsCard3.setOnClickListener {
            navigateToOrderDetail(tvDishName3.text.toString(), thirdOrderId)
        }

        val pointsCard4: CardView = findViewById(R.id.pointsCard4)
        val tvDishName4: TextView = findViewById(R.id.tvDishName4)
        pointsCard4.setOnClickListener {
            navigateToOrderDetail(tvDishName4.text.toString(), fourthOrderId)
        }

        // Các nút điều hướng ở thanh dưới
        findViewById<ImageView>(R.id.icHome).setOnClickListener {
            startActivity(Intent(this, home::class.java))
        }

        findViewById<ImageView>(R.id.icCart).setOnClickListener {
            startActivity(Intent(this, activity_cart::class.java))
        }

        findViewById<ImageView>(R.id.icProfile).setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }
    }

    /**
     * Feature: Lấy danh sách đơn hàng đã hoàn thành để hiển thị điểm tích lũy
     */
    private fun loadOrders() {
        val userId = resolveUserIdForPoints()
        RetrofitClient.apiService.getUserOrders(userId).enqueue(object : Callback<List<OrderResponse>> {
            override fun onResponse(call: Call<List<OrderResponse>>, response: Response<List<OrderResponse>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val completedOrders = response.body()!!
                        .filter { it.status == "completed" }
                        .sortedByDescending { it.createdat }

                    firstOrderId = completedOrders.getOrNull(0)?.id
                    secondOrderId = completedOrders.getOrNull(1)?.id
                    thirdOrderId = completedOrders.getOrNull(2)?.id
                    fourthOrderId = completedOrders.getOrNull(3)?.id

                    // Hiển thị thông tin lên các card
                    bindOrderCard(R.id.tvDishName1, R.id.tvPoints1, completedOrders.getOrNull(0), "Đơn hàng #")
                    bindOrderCard(R.id.tvDishName2, R.id.tvPoints2, completedOrders.getOrNull(1), "Đơn hàng #")
                    bindOrderCard(R.id.tvDishName3, R.id.tvPoints3, completedOrders.getOrNull(2), "Đơn hàng #")
                    bindOrderCard(R.id.tvDishName4, R.id.tvPoints4, completedOrders.getOrNull(3), "Đơn hàng #")
                }
            }

            override fun onFailure(call: Call<List<OrderResponse>>, t: Throwable) {
                Toast.makeText(this@PointsDetail, "Không tải được đơn hàng", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bindOrderCard(titleId: Int, pointsId: Int, order: OrderResponse?, fallbackTitlePrefix: String) {
        val titleView: TextView = findViewById(titleId)
        val pointsView: TextView = findViewById(pointsId)

        if (order == null) {
            titleView.text = "Chưa có đơn hàng"
            pointsView.text = "0"
            return
        }

        titleView.text = "$fallbackTitlePrefix${order.id}"
        // Giả sử 1000đ được 1 điểm
        pointsView.text = (order.totalprice / 1000).toString()
    }

    private fun resolveUserIdForPoints(): Int {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getInt("user_id", 1)
    }

    /**
     * Chuyển sang màn hình Chi tiết theo dõi đơn hàng để người dùng có thể xem lại hoặc đánh giá
     */
    private fun navigateToOrderDetail(dishName: String, orderId: Int?) {
        if (orderId == null) return
        val intent = Intent(this, OrderTrackingDetail::class.java)
        intent.putExtra("order_name", dishName)
        intent.putExtra("order_id", orderId)
        startActivity(intent)
    }
}
