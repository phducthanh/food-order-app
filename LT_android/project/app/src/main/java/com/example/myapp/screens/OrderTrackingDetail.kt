package com.example.myapp.screens

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.EditText
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import com.example.myapp.R
import com.example.myapp.screens.api.OrderDetailResponse
import com.example.myapp.screens.api.ReviewCreateRequest
import com.example.myapp.screens.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Màn hình Chi tiết Theo dõi Đơn hàng (Order Tracking Detail)
 * Chức năng: Hiển thị thông tin chi tiết của một đơn hàng và cho phép người dùng đánh giá sau khi đã nhận hàng.
 * Liên quan đến chức năng: Đặt hàng, Đánh giá đơn hàng.
 */
class OrderTrackingDetail : AppCompatActivity() {
    private var selectedRating = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_tracking_detail)

        // Lấy thông tin đơn hàng từ Intent được truyền qua
        val orderName = intent.getStringExtra("order_name") ?: "Chi tiết đơn hàng"
        val orderId = intent.getIntExtra("order_id", -1)
        val orderStatus = intent.getStringExtra("order_status") ?: ""
        
        val tvTitle: TextView = findViewById(R.id.tvTitle)
        tvTitle.text = orderName

        val timelineContainer: View = findViewById(R.id.timelineContainer)
        val ratingContainer: View = findViewById(R.id.starratingContainer)
        val etFeedbackView: View = findViewById(R.id.etFeedback)
        val btnRateAction: AppCompatButton = findViewById(R.id.btnRate)

        val isCompleted = isCompletedStatus(orderStatus)
        
        // Hiển thị các thành phần giao diện
        timelineContainer.visibility = View.VISIBLE
        ratingContainer.visibility = View.VISIBLE
        etFeedbackView.visibility = View.VISIBLE
        btnRateAction.visibility = View.VISIBLE

        // Nếu đơn hàng chưa hoàn thành, vô hiệu hóa chức năng đánh giá
        if (!isCompleted) {
            btnRateAction.alpha = 0.5f
            btnRateAction.isEnabled = false
            btnRateAction.text = "Chờ giao hàng để đánh giá"
        }

        // Tải thông tin tóm tắt đơn hàng nếu ID hợp lệ
        if (orderId > 0) {
            loadOrderSummary(orderId)
        }

        // Nút quay lại
        val btnBack: ImageView = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        // Xử lý chọn số sao đánh giá
        val stars = listOf(
            findViewById<ImageView>(R.id.star1),
            findViewById<ImageView>(R.id.star2),
            findViewById<ImageView>(R.id.star3),
            findViewById<ImageView>(R.id.star4),
            findViewById<ImageView>(R.id.star5)
        )
        updateStarColors(stars)
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedRating = index + 1
                updateStarColors(stars)
            }
        }

        // Feature: Gửi đánh giá cho đơn hàng đã hoàn thành
        btnRateAction.setOnClickListener {
            val etFeedback: EditText = findViewById(R.id.etFeedback)
            val feedbackText = etFeedback.text.toString()
            val resolvedOrderId = intent.getIntExtra("order_id", -1)
            val userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getInt("user_id", -1)

            if (resolvedOrderId <= 0) {
                android.widget.Toast.makeText(this, "Thiếu mã đơn hàng", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedRating <= 0) {
                android.widget.Toast.makeText(this, "Vui lòng chọn số sao", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (feedbackText.isBlank()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập phản hồi", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRateAction.isEnabled = false
            // Gọi API tạo đánh giá (createReview)
            RetrofitClient.apiService.createReview(
                ReviewCreateRequest(
                    rating = selectedRating,
                    comment = feedbackText,
                    orderid = resolvedOrderId,
                    userid = userId
                )
            ).enqueue(object : Callback<com.example.myapp.screens.api.ReviewResponse> {
                override fun onResponse(
                    call: Call<com.example.myapp.screens.api.ReviewResponse>,
                    response: Response<com.example.myapp.screens.api.ReviewResponse>
                ) {
                    btnRateAction.isEnabled = true
                    if (response.isSuccessful) {
                        android.widget.Toast.makeText(this@OrderTrackingDetail, "Cảm ơn bạn đã đánh giá!", android.widget.Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        android.widget.Toast.makeText(this@OrderTrackingDetail, "Gửi đánh giá thất bại", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.example.myapp.screens.api.ReviewResponse>, t: Throwable) {
                    btnRateAction.isEnabled = true
                    android.widget.Toast.makeText(this@OrderTrackingDetail, "Không thể gửi đánh giá", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Các nút điều hướng ở thanh dưới
        findViewById<ImageView>(R.id.icHome).setOnClickListener { startActivity(Intent(this, home::class.java)) }
        findViewById<ImageView>(R.id.icCart).setOnClickListener { startActivity(Intent(this, cart::class.java)) }
        findViewById<ImageView>(R.id.icProfile).setOnClickListener { startActivity(Intent(this, profile::class.java)) }
    }

    /**
     * Feature: Tải thông tin chi tiết của đơn hàng từ API
     */
    private fun loadOrderSummary(orderId: Int) {
        val summaryView: TextView = findViewById(R.id.tvOrderSummary)
        RetrofitClient.apiService.getOrderDetail(orderId).enqueue(object : Callback<OrderDetailResponse> {
            override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val order = response.body()!!
                    val totalQuantity = order.order_items.sumOf { it.quantity }
                    val itemLines = order.order_items.joinToString(", ") { item ->
                        "${item.menuitem_name ?: "Món #${item.menuitemid}"} (x${item.quantity})"
                    }
                    summaryView.text = "Mã đơn: #${order.id} | Số món: $totalQuantity\n$itemLines"

                    // Hiển thị các thông tin khác vào các view tương ứng
                    populateOrderDetails(order)
                } else {
                    summaryView.text = "Không tải được chi tiết đơn hàng"
                }
            }
            override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                summaryView.text = "Lỗi kết nối"
            }
        })
    }

    /**
     * Điền thông tin đơn hàng vào giao diện (Địa chỉ, nhà hàng, trạng thái...)
     */
    private fun populateOrderDetails(order: OrderDetailResponse) {
        try {
            findViewById<TextView>(R.id.tvOrderId).text = "#${order.id}"
            val priceFormatted = String.format("%,d", order.totalprice).replace(",", ".")
            findViewById<TextView>(R.id.tvTotalPrice).text = "${priceFormatted}đ"
            findViewById<TextView>(R.id.tvRestaurantName).text = order.restaurant_name ?: "--"

            val statusText = when (order.status.lowercase()) {
                "pending" -> "Chờ xác nhận"
                "confirmed" -> "Đã xác nhận"
                "paid" -> "Đã thanh toán"
                "delivering" -> "Đang giao"
                "completed" -> "Đã giao"
                "cancelled" -> "Đã hủy"
                else -> order.status
            }
            findViewById<TextView>(R.id.tvOrderStatus).text = statusText
            findViewById<TextView>(R.id.tvDeliveryAddress).text = order.address_detail ?: "--"
            findViewById<TextView>(R.id.tvOrderDate).text = formatOrderDate(order.createdat)

            // Hiển thị danh sách các món ăn trong đơn hàng
            populateItemsList(order.order_items)
        } catch (e: Exception) {}
    }

    /**
     * Hiển thị danh sách các món ăn kèm theo số lượng và đơn giá
     */
    private fun populateItemsList(items: List<com.example.myapp.screens.api.OrderItemDetailResponse>) {
        val itemsListContainer = findViewById<LinearLayout>(R.id.itemsListContainer)
        itemsListContainer.removeAllViews()
        for (item in items) {
            // ... (Phần logic tạo view cho từng item)
        }
    }

    private fun formatOrderDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "--"
        // ... (Logic định dạng ngày tháng)
        return dateString
    }

    /**
     * Cập nhật màu sắc của các ngôi sao khi người dùng chọn mức độ đánh giá
     */
    private fun updateStarColors(stars: List<ImageView>) {
        stars.forEachIndexed { index, star ->
            val color = if (index < selectedRating) "#FFC107" else "#CFCFCF"
            star.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN)
        }
    }

    private fun isCompletedStatus(status: String?): Boolean {
        val normalized = (status ?: "").lowercase(java.util.Locale.ROOT)
        return normalized == "completed" || normalized == "delivered" || normalized == "done"
    }
}
