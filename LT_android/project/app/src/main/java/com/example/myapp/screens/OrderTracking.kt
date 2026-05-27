package com.example.myapp.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.LayoutInflater
import com.example.myapp.R
import com.example.myapp.screens.api.OrderDetailResponse
import com.example.myapp.screens.api.OrderResponse
import com.example.myapp.screens.api.RetrofitClient
import com.example.myapp.screens.api.UserProfileSummary
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn hình Theo dõi đơn hàng (Order Tracking)
 * Chức năng: Hiển thị danh sách các đơn hàng hiện tại (đang giao) và các đơn hàng cũ (đã giao).
 * Liên quan đến chức năng: Đặt hàng.
 */
class OrderTracking : AppCompatActivity() {
    private var allPendingOrders = listOf<OrderDetailResponse>()
    private var allCompletedOrders = listOf<OrderDetailResponse>()
    private var displayedPendingCount = 0
    private var displayedCompletedCount = 0
    private val itemsPerPage = 5
    
    private lateinit var ordersContainer: android.widget.LinearLayout
    private lateinit var btnLoadMore: android.widget.Button
    private lateinit var scrollViewOrders: android.widget.ScrollView
    private var isAutoLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_tracking)

        ordersContainer = findViewById(R.id.ordersContainer)
        btnLoadMore = findViewById(R.id.btnLoadMore)
        scrollViewOrders = findViewById(R.id.scrollViewOrders)

        // Tải thông tin tóm tắt (điểm tích lũy) và danh sách đơn hàng
        loadPointsSummary()
        loadAllOrders()

        // Tự động tải thêm đơn hàng khi cuộn xuống cuối danh sách
        scrollViewOrders.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val scrollView = v as android.widget.ScrollView
            val child = scrollView.getChildAt(0)
            if (child != null) {
                if (scrollY + scrollView.height >= child.height - 200) {
                    if (!isAutoLoading) {
                        isAutoLoading = true
                        loadMoreOrders()
                        scrollView.postDelayed({ isAutoLoading = false }, 1000)
                    }
                }
            }
        }

        // Các nút điều hướng cơ bản
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.icHome).setOnClickListener { startActivity(Intent(this, home::class.java)) }
        findViewById<ImageView>(R.id.icCart).setOnClickListener { startActivity(Intent(this, activity_cart::class.java)) }
        findViewById<ImageView>(R.id.icProfile).setOnClickListener { startActivity(Intent(this, profile::class.java)) }

        btnLoadMore.setOnClickListener {
            loadMoreOrders()
        }
    }

    /**
     * Feature: Lấy thông tin điểm tích lũy của người dùng từ API profile summary
     */
    private fun loadPointsSummary() {
        val userId = resolveUserIdForTracking()
        val pointsView = findViewById<TextView>(R.id.tvPointsSummary)
        RetrofitClient.apiService.getProfileSummary(userId).enqueue(object : Callback<UserProfileSummary> {
            override fun onResponse(call: Call<UserProfileSummary>, response: Response<UserProfileSummary>) {
                if (response.isSuccessful) {
                    pointsView.text = "Điểm tích lũy: ${response.body()?.points ?: 0}"
                }
            }
            override fun onFailure(call: Call<UserProfileSummary>, t: Throwable) {
                pointsView.text = "Điểm tích lũy: 0"
            }
        })
    }

    /**
     * Feature: Lấy toàn bộ danh sách đơn hàng của người dùng
     */
    private fun loadAllOrders() {
        val userId = resolveUserIdForTracking()
        RetrofitClient.apiService.getUserOrders(userId).enqueue(object : Callback<List<OrderResponse>> {
            override fun onResponse(call: Call<List<OrderResponse>>, response: Response<List<OrderResponse>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val allOrders = response.body()!!.sortedByDescending { it.createdat }
                    
                    // Phân loại đơn hàng dựa trên trạng thái
                    val pendingOrderIds = allOrders.filter { !isCompletedStatus(it.status) }.map { it.id }
                    val completedOrderIds = allOrders.filter { isCompletedStatus(it.status) }.map { it.id }

                    // Tải chi tiết cho từng đơn hàng để hiển thị đầy đủ thông tin món ăn
                    loadOrdersDetail(pendingOrderIds, completedOrderIds)
                } else {
                    btnLoadMore.visibility = android.view.View.GONE
                }
            }
            override fun onFailure(call: Call<List<OrderResponse>>, t: Throwable) {
                btnLoadMore.visibility = android.view.View.GONE
            }
        })
    }

    /**
     * Feature: Tải thông tin chi tiết (OrderDetail) cho danh sách các ID đơn hàng
     */
    private fun loadOrdersDetail(pendingOrderIds: List<Int>, completedOrderIds: List<Int>) {
        val pending = mutableListOf<OrderDetailResponse>()
        val completed = mutableListOf<OrderDetailResponse>()
        var completed_count = 0
        val total_count = pendingOrderIds.size + completedOrderIds.size

        if (total_count == 0) {
            bindAllOrders(emptyList(), emptyList())
            return
        }

        // Gọi API chi tiết cho từng đơn hàng (vì màn hình này cần hiển thị danh sách món ăn bên trong mỗi card)
        val combinedIds = pendingOrderIds + completedOrderIds
        for (orderId in combinedIds) {
            RetrofitClient.apiService.getOrderDetail(orderId).enqueue(object : Callback<OrderDetailResponse> {
                override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val detail = response.body()!!
                        if (isCompletedStatus(detail.status)) completed.add(detail)
                        else pending.add(detail)
                    }
                    completed_count++
                    if (completed_count == total_count) {
                        bindAllOrders(pending, completed)
                    }
                }
                override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                    completed_count++
                    if (completed_count == total_count) bindAllOrders(pending, completed)
                }
            })
        }
    }

    private fun bindAllOrders(pending: List<OrderDetailResponse>, completed: List<OrderDetailResponse>) {
        allPendingOrders = pending
        allCompletedOrders = completed
        displayedPendingCount = 0
        displayedCompletedCount = 0
        ordersContainer.removeAllViews()
        displayNextOrders()
    }

    /**
     * Hiển thị đợt đơn hàng tiếp theo (Phân trang local)
     */
    private fun displayNextOrders() {
        val inflater = LayoutInflater.from(this)

        // Hiển thị phần "Chưa giao"
        val pendingToDisplay = allPendingOrders.drop(displayedPendingCount).take(itemsPerPage)
        if (pendingToDisplay.isNotEmpty()) {
            if (displayedPendingCount == 0) {
                addSectionLabel("Chưa giao", "#d32f2f")
            }
            pendingToDisplay.forEach { addOrderCard(ordersContainer, it, it.status, inflater) }
            displayedPendingCount += pendingToDisplay.size
        }

        // Hiển thị phần "Đã giao"
        val completedToDisplay = allCompletedOrders.drop(displayedCompletedCount).take(itemsPerPage)
        if (completedToDisplay.isNotEmpty()) {
            if (displayedCompletedCount == 0) {
                addSectionLabel("Đã giao", "#34a853")
            }
            completedToDisplay.forEach { addOrderCard(ordersContainer, it, it.status, inflater) }
            displayedCompletedCount += completedToDisplay.size
        }

        val hasMore = displayedPendingCount < allPendingOrders.size || displayedCompletedCount < allCompletedOrders.size
        btnLoadMore.visibility = if (hasMore) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun addSectionLabel(text: String, colorHex: String) {
        val labelView = android.widget.TextView(this).apply {
            this.text = text
            setTextColor(android.graphics.Color.parseColor(colorHex))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(10, 20, 0, 16)
        }
        ordersContainer.addView(labelView)
    }

    private fun loadMoreOrders() {
        displayNextOrders()
    }

    /**
     * Tạo và thêm một CardView hiển thị tóm tắt đơn hàng vào container
     */
    private fun addOrderCard(container: android.widget.LinearLayout, order: OrderDetailResponse, status: String, inflater: LayoutInflater) {
        val isCompleted = isCompletedStatus(status)
        val statusColor = if (isCompleted) android.graphics.Color.parseColor("#34a853") else android.graphics.Color.parseColor("#d32f2f")
        val statusText = if (isCompleted) "Đã giao ✓" else "Chưa giao"
        
        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val cardView = androidx.cardview.widget.CardView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
            radius = 30f
            cardElevation = 4f
        }

        val mainLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
        }

        // Danh sách các món ăn trong đơn
        val itemSummary = order.order_items.joinToString("\n") { "• ${it.menuitem_name} x${it.quantity}" }
        val dishNameView = android.widget.TextView(this).apply {
            text = itemSummary
            setTextColor(android.graphics.Color.BLACK)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        mainLayout.addView(dishNameView)

        // Tổng số lượng, Nhà hàng, Trạng thái và Tổng tiền
        // ... (phần code tạo UI động tiếp theo)

        cardView.addView(mainLayout)
        cardView.setOnClickListener {
            openOrderDetail(order.id, status, order.restaurant_name)
        }
        container.addView(cardView)
    }

    private fun resolveUserIdForTracking(): Int {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getInt("user_id", 1)
    }

    private fun isCompletedStatus(status: String?): Boolean {
        val normalized = (status ?: "").lowercase(Locale.ROOT)
        return normalized == "completed" || normalized == "delivered" || normalized == "done"
    }

    /**
     * Mở màn hình chi tiết theo dõi đơn hàng
     */
    private fun openOrderDetail(orderId: Int?, status: String, restaurantName: String?) {
        if (orderId == null) return
        val intent = Intent(this, OrderTrackingDetail::class.java)
        intent.putExtra("order_id", orderId)
        intent.putExtra("order_status", status)
        intent.putExtra("order_name", restaurantName ?: "Đơn hàng #$orderId")
        startActivity(intent)
    }
}
