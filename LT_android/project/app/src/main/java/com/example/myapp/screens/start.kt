package com.example.myapp.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.example.myapp.screens.api.RetrofitClient


// Thêm các bản import này
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.example.myapp.R
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Màn hình Khởi đầu (Start Screen)
 * Chức năng: Màn hình chào mừng, cho phép người dùng lựa chọn Đăng nhập hoặc Đăng ký.
 */
class start : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.start)

        // Khởi tạo Retrofit client với context của activity
        RetrofitClient.init(this)

        // --- ĐOẠN CODE LẤY FCM TOKEN ---
        // Chức năng: Lấy mã định danh thiết bị để hỗ trợ gửi thông báo (Push Notifications) về đơn hàng
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_DEBUG", "Lấy Token thất bại", task.exception)
                return@addOnCompleteListener
            }

            // Lấy token thành công
            val token = task.result
            Log.d("FCM_DEBUG", "Token của máy ảo nè: $token")

            // TODO: Gửi token này lên Server của bạn qua Retrofit để lưu vào DB nhằm gửi thông báo theo người dùng
        }
        // -------------------------------

        // Chuyển sang màn hình Đăng nhập
        val btnSignIn: View = findViewById(R.id.btnSignIn)
        btnSignIn.setOnClickListener {
            startActivity(Intent(this, signin::class.java))
        }

        // Chuyển sang màn hình Đăng ký
        val btnSignUp: View = findViewById(R.id.btnSignUp)
        btnSignUp.setOnClickListener {
            startActivity(Intent(this, signup::class.java))
        }
    }
}
