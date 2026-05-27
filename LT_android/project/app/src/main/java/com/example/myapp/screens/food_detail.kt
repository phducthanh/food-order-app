package com.example.myapp.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.myapp.R
import com.example.myapp.screens.api.RetrofitClient
import com.example.myapp.screens.api.MenuItem
import com.example.myapp.screens.api.ReviewDetail
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn hình Chi tiết món ăn (Food Detail)
 * Chức năng: Hiển thị thông tin chi tiết về món ăn, cho phép thêm vào giỏ hàng, đặt hàng trước và chia sẻ.
 */
class food_detail : AppCompatActivity() {
    private var qty = 1
    private var price = 0
    private var foodId = -1
    private var foodName = ""
    private var foodImageUrl = ""
    private var foodDescription = ""
    private var reviews: List<ReviewDetail>? = null
    private var avgRating: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_detail)

        // Xử lý Deep Link khi người dùng nhấn vào một liên kết chia sẻ món ăn
        val data: Uri? = intent.data
        if (data != null && data.host == "yourapp.com" && data.path?.startsWith("/food") == true) {
            val idStr = data.getQueryParameter("id")
            foodId = idStr?.toIntOrNull() ?: -1
            if (foodId != -1) {
                fetchFoodDetails(foodId)
            }
        } else {
            // Lấy dữ liệu từ Intent thông thường
            foodId = intent.getIntExtra("food_id", -1)
            foodName = intent.getStringExtra("food_name") ?: ""
            price = intent.getIntExtra("food_price", 0)
            foodDescription = intent.getStringExtra("food_description") ?: ""
            foodImageUrl = intent.getStringExtra("food_image_url") ?: ""
            
            // Tải thông tin chi tiết và đánh giá từ API
            if (foodId != -1) {
                fetchFoodDetailsWithReviews(foodId)
            } else {
                setupUI()
            }
        }
    }

    /**
     * Feature: Lấy thông tin món ăn từ API (cho trường hợp Deep Link)
     */
    private fun fetchFoodDetails(id: Int) {
        RetrofitClient.apiService.getMenuItemDetail(id).enqueue(object : Callback<MenuItem> {
            override fun onResponse(call: Call<MenuItem>, response: Response<MenuItem>) {
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) {
                        foodId = item.id
                        foodName = item.name
                        price = item.price
                        foodDescription = item.description ?: ""
                        foodImageUrl = item.image_url ?: ""
                        reviews = item.reviews ?: emptyList()
                        avgRating = item.avg_rating ?: 0f
                        setupUI()
                    } else {
                        Toast.makeText(this@food_detail, "Không tìm thấy món ăn", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<MenuItem>, t: Throwable) {
                Toast.makeText(this@food_detail, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Feature: Tải dữ liệu đầy đủ bao gồm các đánh giá của người dùng
     */
    private fun fetchFoodDetailsWithReviews(id: Int) {
        RetrofitClient.apiService.getMenuItemDetail(id).enqueue(object : Callback<MenuItem> {
            override fun onResponse(call: Call<MenuItem>, response: Response<MenuItem>) {
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) {
                        foodId = item.id
                        foodName = item.name
                        price = item.price
                        foodDescription = item.description ?: ""
                        foodImageUrl = item.image_url ?: ""
                        reviews = item.reviews ?: emptyList()
                        avgRating = item.avg_rating ?: 0f
                        setupUI()
                    } else {
                        setupUI()
                    }
                }
            }
            override fun onFailure(call: Call<MenuItem>, t: Throwable) {
                setupUI()
            }
        })
    }

    /**
     * Thiết lập giao diện người dùng và gán các sự kiện
     */
    private fun setupUI() {
        // Feature: Chia sẻ món ăn
        val btnshare: ImageView = findViewById(R.id.btn_share)
        btnshare.setOnClickListener {
            val intent = Intent(this, share::class.java).apply {
                putExtra("food_id", foodId)
                putExtra("food_name", foodName)
                putExtra("food_price", price)
                putExtra("food_description", foodDescription)
                putExtra("food_image_url", foodImageUrl)
            }
            startActivity(intent)
        }

        val tvQty: TextView = findViewById(R.id.tvQty)
        val tvTotal: TextView = findViewById(R.id.tvTotalValue)
        val tvFoodName: TextView = findViewById(R.id.tvFoodName)
        val tvUnitPrice: TextView = findViewById(R.id.tvUnitPrice)
        val tvDescription: TextView = findViewById(R.id.tvDescription)
        val imgFood: ImageView = findViewById(R.id.imgFood)
        val btnDecrease: TextView = findViewById(R.id.btnDecrease)
        val btnIncrease: TextView = findViewById(R.id.btnIncrease)
        val btnAddCart: AppCompatButton = findViewById(R.id.btnAddCart)
        val btnPreOrder: AppCompatButton = findViewById(R.id.btnPreOrder)
        val btnBack: ImageView = findViewById(R.id.btnBack)

        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        tvFoodName.text = foodName
        tvUnitPrice.text = fmt.format(price) + "đ"
        tvDescription.text = foodDescription

        if (foodImageUrl.isNotEmpty()) {
            Picasso.get().load(foodImageUrl).placeholder(R.drawable.placeholder_loading).error(R.drawable.pngwing).into(imgFood)
        }

        // Hiển thị đánh giá trung bình
        val ratingBar: android.widget.RatingBar? = try { findViewById(R.id.ratingBar) } catch (e: Exception) { null }
        val tvRating: TextView? = try { findViewById(R.id.tvRating) } catch (e: Exception) { null }
        
        if (avgRating > 0) {
            ratingBar?.visibility = android.view.View.VISIBLE
            ratingBar?.rating = avgRating
            tvRating?.text = String.format("(%.1f)", avgRating)
            tvRating?.visibility = android.view.View.VISIBLE
        } else {
            ratingBar?.visibility = android.view.View.GONE
            tvRating?.text = "Chưa có đánh giá"
        }

        displayReviews()

        fun refresh() {
            tvQty.text = qty.toString()
            val totalPrice = qty * price
            tvTotal.text = fmt.format(totalPrice) + "đ"
        }

        btnDecrease.setOnClickListener { if (qty > 1) { qty--; refresh() } }
        btnIncrease.setOnClickListener { qty++; refresh() }
        
        // Feature: Thêm món vào giỏ hàng
        btnAddCart.setOnClickListener { addToCart() }

        // Feature: Chuyển sang màn hình Đặt hàng trước
        btnPreOrder.setOnClickListener {
            val intent = Intent(this, pre_order::class.java).apply {
                putExtra("food_id", foodId)
                putExtra("food_name", foodName)
                putExtra("food_price", price)
                putExtra("food_image_url", foodImageUrl)
            }
            startActivity(intent)
        }

        btnBack.setOnClickListener { finish() }
        refresh()
    }

    /**
     * Hiển thị danh sách nhận xét của người dùng
     */
    private fun displayReviews() {
        // ... (giữ nguyên logic hiển thị nhận xét)
    }

    /**
     * Feature: Thêm món ăn vào giỏ hàng (Cart)
     */
    private fun addToCart() {
        if (foodId == -1) {
            Toast.makeText(this, "Lỗi dữ liệu món ăn", Toast.LENGTH_SHORT).show()
            return
        }
        val existingItem = cart.cartList.find { it.id == foodId }
        if (existingItem != null) {
            existingItem.qty += qty
        } else {
            // Thêm item mới vào cartList toàn cục
            cart.cartList.add(CartItem(id = foodId, name = foodName, price = price, qty = qty, imageUrl = foodImageUrl, isSelected = true))
        }
        Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
        finish()
    }
}
