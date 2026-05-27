package com.example.myapp.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.squareup.picasso.Picasso
import java.text.NumberFormat
import java.util.Locale

import java.io.Serializable

/**
 * Model cho một món ăn trong giỏ hàng
 */
data class CartItem(
    val id: Int,
    val name: String,
    val price: Int,
    var qty: Int,
    val imageUrl: String? = null,
    var isSelected: Boolean = false
) : Serializable

/**
 * Màn hình Giỏ hàng (Cart)
 * Chức năng: Hiển thị danh sách các món ăn đã chọn, thay đổi số lượng, xóa món và tiến hành thanh toán.
 */
class cart : AppCompatActivity() {
    private lateinit var rvCart: RecyclerView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var cbSelectAll: CheckBox
    private lateinit var btnCheckout: AppCompatButton
    private lateinit var adapter: CartAdapter

    companion object {
        // Danh sách giỏ hàng toàn cục để lưu trữ món ăn được thêm từ các màn hình khác
        val cartList = mutableListOf<CartItem>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Khởi tạo các view
        rvCart = findViewById(R.id.rvCart)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        btnCheckout = findViewById(R.id.btnCheckout)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Xử lý sự kiện khi nhấn nút Thanh toán
        // Feature: Checkout - Chuyển sang màn hình đặt hàng
        btnCheckout.setOnClickListener {
            if (cartList.none { it.isSelected }) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 món", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, order::class.java))
        }

        // Khởi tạo Adapter cho RecyclerView
        adapter = CartAdapter(cartList, { updateSummary() }, { position ->
            // Xử lý xóa món khỏi giỏ hàng
            cartList.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, cartList.size)
            updateSummary()
        })
        rvCart.layoutManager = LinearLayoutManager(this)
        rvCart.adapter = adapter

        // Xử lý chọn/bỏ chọn tất cả các món trong giỏ hàng
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            cartList.forEach { it.isSelected = isChecked }
            adapter.notifyDataSetChanged()
            updateSummary()
        }
        updateSummary()
    }

    /**
     * Cập nhật tổng số lượng và tổng tiền các món được chọn
     */
    private fun updateSummary() {
        val selectedItems = cartList.filter { it.isSelected }
        val totalQty = selectedItems.sumOf { it.qty }
        val totalPrice = selectedItems.sumOf { it.qty * it.price }
        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        
        tvTotalItems.text = totalQty.toString()
        tvTotalPrice.text = fmt.format(totalPrice) + "đ"
        
        // Kích hoạt nút thanh toán nếu có món được chọn
        btnCheckout.isEnabled = selectedItems.isNotEmpty()
        btnCheckout.alpha = if (selectedItems.isNotEmpty()) 1f else 0.5f
    }
}

/**
 * Adapter hiển thị danh sách các món ăn trong giỏ hàng
 */
class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onUpdate: () -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFood: ImageView = view.findViewById(R.id.imgFood)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvQty: TextView = view.findViewById(R.id.tvQty)
        val btnPlus: TextView = view.findViewById(R.id.btnPlus)
        val btnMinus: TextView = view.findViewById(R.id.btnMinus)
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        
        holder.tvName.text = item.name
        holder.tvPrice.text = fmt.format(item.price) + "đ"
        holder.tvQty.text = item.qty.toString()
        holder.cbSelect.isChecked = item.isSelected
        
        // Tải hình ảnh món ăn
        if (!item.imageUrl.isNullOrEmpty()) Picasso.get().load(item.imageUrl).into(holder.imgFood)
        
        // Tăng số lượng món ăn
        holder.btnPlus.setOnClickListener { 
            item.qty++
            notifyItemChanged(holder.adapterPosition)
            onUpdate() 
        }
        
        // Giảm số lượng món ăn
        holder.btnMinus.setOnClickListener { 
            if (item.qty > 1) { 
                item.qty--
                notifyItemChanged(holder.adapterPosition)
                onUpdate() 
            } 
        }
        
        // Chọn/Bỏ chọn món ăn cụ thể
        holder.cbSelect.setOnClickListener { 
            item.isSelected = holder.cbSelect.isChecked
            onUpdate() 
        }
        
        // Xóa món ăn
        holder.btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
    }
    
    override fun getItemCount() = items.size
}
