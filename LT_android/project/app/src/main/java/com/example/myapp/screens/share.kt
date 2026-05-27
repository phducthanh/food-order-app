package com.example.myapp.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.R
import com.squareup.picasso.Picasso
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn hình Chia sẻ món ăn (Share)
 * Chức năng: Cho phép người dùng chia sẻ thông tin món ăn lên các nền tảng mạng xã hội.
 */
class share : AppCompatActivity() {
    private var foodId: Int = -1
    private var foodName: String = ""
    private var foodDescription: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        // Lấy dữ liệu món ăn được truyền từ màn hình chi tiết
        foodId = intent.getIntExtra("food_id", -1)
        foodName = intent.getStringExtra("food_name") ?: ""
        val foodPrice = intent.getIntExtra("food_price", 0)
        foodDescription = intent.getStringExtra("food_description") ?: ""
        val foodImageUrl = intent.getStringExtra("food_image_url") ?: ""

        val tvFoodName: TextView = findViewById(R.id.tvFoodName)
        val tvFoodDescription: TextView = findViewById(R.id.tvFoodDescription)
        val tvFoodPrice: TextView = findViewById(R.id.tvFoodPrice)
        val imgFood: ImageView = findViewById(R.id.imgFood)

        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        tvFoodName.text = foodName
        tvFoodDescription.text = foodDescription
        tvFoodPrice.text = "Giá: " + fmt.format(foodPrice) + "đ"

        if (foodImageUrl.isNotEmpty()) {
            Picasso.get()
                .load(foodImageUrl)
                .placeholder(R.drawable.placeholder_loading)
                .error(R.drawable.pngwing)
                .into(imgFood)
        }

        // Feature: Chia sẻ lên các ứng dụng cụ thể
        findViewById<LinearLayout>(R.id.btnFacebook).setOnClickListener { shareToApp("com.facebook.katana") }
        findViewById<LinearLayout>(R.id.btnZalo).setOnClickListener { shareToApp("com.zing.zalo") }
        findViewById<LinearLayout>(R.id.btnTwitter).setOnClickListener { shareToApp("com.twitter.android") }
        findViewById<LinearLayout>(R.id.btnMessenger).setOnClickListener { shareToApp("com.facebook.orca") }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    /**
     * Thực hiện chia sẻ nội dung văn bản tới một ứng dụng được chỉ định bằng package name.
     * Nếu ứng dụng không được cài đặt, hiển thị trình chọn ứng dụng mặc định của hệ thống.
     */
    private fun shareToApp(packageName: String) {
        val shareLink = "https://yourapp.com/food?id=$foodId"
        val shareText = "Thử ngay món $foodName cực ngon tại MyApp!\n$foodDescription\nXem chi tiết tại: $shareLink"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            setPackage(packageName)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Trường hợp ứng dụng cụ thể chưa cài đặt, dùng trình chọn mặc định
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }, "Chia sẻ qua")
            startActivity(chooser)
        }
    }
}
