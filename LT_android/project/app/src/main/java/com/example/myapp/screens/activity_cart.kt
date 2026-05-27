package com.example.myapp.screens

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity Giỏ hàng (Bản thay thế hoặc bổ trợ)
 * Chức năng: Quản lý danh sách các món ăn đã thêm vào giỏ hàng thường.
 */
class activity_cart : AppCompatActivity() {
    private lateinit var rvCart: RecyclerView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var cbSelectAll: CheckBox
    private lateinit var btnCheckout: AppCompatButton
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Ánh xạ View
        rvCart = findViewById(R.id.rvCart)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        btnCheckout = findViewById(R.id.btnCheckout)
        val btnBack: ImageView = findViewById(R.id.btnBack)

        // Nút quay lại
        btnBack.setOnClickListener { finish() }

        // Feature: Chuyển sang màn hình đặt hàng (Checkout)
        btnCheckout.setOnClickListener {
            val intent = Intent(this, order::class.java)
            startActivity(intent)
        }

        // Thiết lập Adapter sử dụng danh sách giỏ hàng tĩnh từ class cart
        adapter = CartAdapter(
            cart.cartList,
            onUpdate = { updateSummary() },
            onDelete = { position ->
                // Xử lý xóa món khỏi danh sách
                cart.cartList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, cart.cartList.size)
                updateSummary()
            }
        )
        rvCart.layoutManager = LinearLayoutManager(this)
        rvCart.adapter = adapter

        // Xử lý chọn/bỏ chọn tất cả các món trong giỏ hàng
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            cart.cartList.forEach { it.isSelected = isChecked }
            adapter.notifyDataSetChanged()
            updateSummary()
        }

        updateSummary()
    }

    /**
     * Cập nhật thông tin tổng hợp của giỏ hàng
     */
    private fun updateSummary() {
        val selectedItems = cart.cartList.filter { it.isSelected }
        val totalQty = selectedItems.sumOf { it.qty }
        val totalPrice = selectedItems.sumOf { it.qty * it.price }

        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        tvTotalItems.text = totalQty.toString()
        tvTotalPrice.text = fmt.format(totalPrice) + "đ"

        // Kích hoạt nút thanh toán
        btnCheckout.isEnabled = true
        btnCheckout.alpha = 1.0f
    }
}
