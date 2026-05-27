package com.example.myapp.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.R
import com.example.myapp.screens.api.OrderResponse
import com.example.myapp.screens.api.OrderStatusUpdateRequest
import com.example.myapp.screens.api.RetrofitClient
import com.example.myapp.screens.home
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Màn hình Thông báo đặt hàng/thanh toán thành công (Payment Successful)
 * Chức năng: Xác nhận đơn hàng đã được tạo thành công và cho phép người dùng quay lại trang chủ.
 * Liên quan đến chức năng: Đặt hàng.
 */
class payment_successful : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.payment_successful)

        val orderId = intent.getIntExtra("order_id", -1)
        
        // Feature: Cập nhật trạng thái đơn hàng sau khi hoàn tất thanh toán/đặt hàng
        // Nếu là COD (thanh toán khi nhận hàng), cập nhật status thành "confirmed"
        if (orderId > 0) {
            updateOrderStatusToCODConfirmed(orderId)
        }

        // Nút quay về trang chủ
        val btnReturnHome: Button = findViewById(R.id.btnReturnHome)
        btnReturnHome.setOnClickListener {
            val intent = Intent(this, home::class.java)
            // Xóa ngăn xếp các Activity cũ để tránh quay lại màn hình thành công khi nhấn Back
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Các nút điều hướng cơ bản
        findViewById<ImageView>(R.id.icProfile).setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }

        findViewById<ImageView>(R.id.icHome).setOnClickListener {
            startActivity(Intent(this, home::class.java))
        }

        findViewById<ImageView>(R.id.icCart).setOnClickListener {
            startActivity(Intent(this, cart::class.java))
        }
    }

    /**
     * Feature: Gọi API cập nhật trạng thái đơn hàng sang "confirmed"
     */
    private fun updateOrderStatusToCODConfirmed(orderId: Int) {
        RetrofitClient.apiService.updateOrderStatus(orderId, OrderStatusUpdateRequest("confirmed"))
            .enqueue(object : Callback<OrderResponse> {
                override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                    if (response.isSuccessful) {
                        // Trạng thái đã được cập nhật thành công trên server
                    } else {
                        Toast.makeText(this@payment_successful, "Cập nhật đơn hàng thất bại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                    Toast.makeText(this@payment_successful, "Lỗi mạng", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
