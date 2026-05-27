package com.example.myapp.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.example.myapp.R
import com.example.myapp.screens.api.LoginResponse
import com.example.myapp.screens.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Màn hình Đăng nhập (SignIn)
 * Chức năng: Cho phép người dùng đăng nhập vào hệ thống bằng username và password.
 */
class signin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signin)

        // Nút quay lại màn hình trước đó
        val btnBack: ImageView = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish() // quay lại màn hình trước (start)
        }

        // Xử lý sự kiện khi nhấn nút Đăng nhập
        val btnSignIn: View = findViewById(R.id.btn_signin)
        btnSignIn.setOnClickListener {
            val etUsername: EditText = findViewById(R.id.et_username)
            val etPassword: EditText = findViewById(R.id.et_password)

            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Kiểm tra tính hợp lệ của thông tin nhập vào
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi API đăng nhập thông qua Retrofit
            RetrofitClient.apiService.login(username, password).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            // Đăng nhập thành công
                            Toast.makeText(this@signin, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                            
                            // Lưu access token vào SharedPreferences để sử dụng cho các yêu cầu sau
                            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("access_token", loginResponse.access_token)
                                apply()
                            }
                            
                            // Sau khi đăng nhập thành công, lấy thông tin chi tiết của user hiện tại
                            RetrofitClient.apiService.getCurrentUser().enqueue(object : Callback<com.example.myapp.screens.api.RegisterResponse> {
                                override fun onResponse(call: Call<com.example.myapp.screens.api.RegisterResponse>, response: Response<com.example.myapp.screens.api.RegisterResponse>) {
                                    if (response.isSuccessful) {
                                        val user = response.body()
                                        if (user != null) {
                                            // Lưu user ID và tên vào SharedPreferences để quản lý phiên làm việc
                                            with(sharedPref.edit()) {
                                                putInt("user_id", user.id)
                                                putString("user_name", user.name)
                                                apply()
                                            }
                                        }
                                    }
                                    // Chuyển đến màn hình chính (Home) sau khi hoàn tất lưu thông tin
                                    val intent = Intent(this@signin, home::class.java)
                                    startActivity(intent)
                                    finish()
                                }

                                override fun onFailure(call: Call<com.example.myapp.screens.api.RegisterResponse>, t: Throwable) {
                                    // Vẫn chuyển đến màn hình chính ngay cả khi lấy thông tin user thất bại
                                    val intent = Intent(this@signin, home::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            })
                        } else {
                            // Phản hồi từ server không có dữ liệu
                            Toast.makeText(this@signin, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Xử lý các mã lỗi HTTP từ server (ví dụ: 401 Unauthorized)
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (errorBody != null) {
                            try {
                                val jsonObject = org.json.JSONObject(errorBody)
                                if (jsonObject.has("detail")) {
                                    jsonObject.getString("detail")
                                } else {
                                    "Sai tên đăng nhập hoặc mật khẩu"
                                }
                            } catch (e: Exception) {
                                "Sai tên đăng nhập hoặc mật khẩu"
                            }
                        } else {
                            "Sai tên đăng nhập hoặc mật khẩu"
                        }
                        Toast.makeText(this@signin, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // Lỗi kết nối mạng hoặc lỗi hệ thống khác
                    Toast.makeText(this@signin, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
