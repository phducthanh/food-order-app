package com.example.myapp.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.R
import com.example.myapp.screens.api.Address
import com.example.myapp.screens.api.MenuItem
import com.example.myapp.screens.api.OrderCreateRequest
import com.example.myapp.screens.api.OrderItemRequest
import com.example.myapp.screens.api.OrderResponse
import com.example.myapp.screens.api.RetrofitClient
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn hình Đặt hàng (Order)
 * Chức năng: Xử lý quá trình thanh toán, chọn địa chỉ, áp dụng khuyến mãi và tạo đơn hàng.
 * Liên quan đến chức năng: Đặt hàng, Thanh toán.
 */
class order : AppCompatActivity() {
    private var currentAddress: Address? = null
    private var restaurantId: Int = 1
    private val selectedItems = mutableListOf<CartItem>()
    private var reorderSourceOrderId: Int? = null
    private var baseTotalPrice: Int = 0
    private var selectedPromotionCode: String? = null
    private var selectedDiscountType: String? = null
    private var selectedDiscountValue: Int = 0
    private var selectedPaymentMethod: String = "cod"
    private var selectedPaymentMethodLabel: String = "Thanh toán khi nhận hàng"

    // Launcher để nhận kết quả từ màn hình chọn mã giảm giá
    private val voucherPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult

        selectedPromotionCode = result.data?.getStringExtra("promotion_code")
        selectedDiscountType = result.data?.getStringExtra("discount_type")
        selectedDiscountValue = result.data?.getIntExtra("discount_value", 0) ?: 0
        bindVoucherAndTotal()
    }

    // Launcher để nhận kết quả từ màn hình chọn phương thức thanh toán
    private val paymentMethodPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult

        selectedPaymentMethod = result.data?.getStringExtra("payment_method") ?: selectedPaymentMethod
        selectedPaymentMethodLabel = result.data?.getStringExtra("payment_method_label") ?: selectedPaymentMethodLabel
        bindPaymentMethod()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order)

        // Kiểm tra nếu là đặt lại từ đơn hàng cũ (Reorder)
        reorderSourceOrderId = intent.getIntExtra("reorder_order_id", -1).takeIf { it > 0 }

        if (reorderSourceOrderId != null) {
            bindCartPreview()
            bindPaymentMethod()
            loadReorderSourceOrder(reorderSourceOrderId!!)
        } else {
            selectedItems.clear()
            
            // Lấy danh sách món ăn từ Intent (ví dụ từ chức năng Đặt trước) hoặc từ Giỏ hàng
            val passedItems = intent.getSerializableExtra("selected_items") as? ArrayList<CartItem>
            if (passedItems != null) {
                selectedItems.addAll(passedItems)
            } else {
                // Nếu không truyền qua Intent, mặc định lấy các món được tích chọn trong giỏ hàng
                selectedItems.addAll(cart.cartList.filter { it.isSelected })
                if (selectedItems.isEmpty()) {
                    selectedItems.addAll(cart.cartList)
                }
            }

            bindCartPreview()
            bindPaymentMethod()
            resolveRestaurantId()
        }
        
        // Tải địa chỉ giao hàng của người dùng
        loadUserAddress()

        val cardVoucher = findViewById<View>(R.id.cardVoucher)
        val cardPaymentMethod = findViewById<View>(R.id.cardPaymentMethod)
        val lblAddress = findViewById<TextView>(R.id.lblAddress)
        val valAddress = findViewById<TextView>(R.id.valAddress)
        val btnCancel = findViewById<View>(R.id.btnCancel)
        val btnOrder = findViewById<View>(R.id.btnOrder)
        val btnBack = findViewById<ImageView>(R.id.icBack)

        // Sự kiện chọn Voucher
        cardVoucher.setOnClickListener {
            voucherPickerLauncher.launch(Intent(this, discouts::class.java))
        }

        // Sự kiện chọn Phương thức thanh toán
        cardPaymentMethod.setOnClickListener {
            val paymentIntent = Intent(this, payment_methods::class.java)
            val discountAmount = calculateDiscountAmount(baseTotalPrice)
            val payableTotal = (baseTotalPrice - discountAmount).coerceAtLeast(0)
            paymentIntent.putExtra("order_total", payableTotal)
            paymentIntent.putExtra("select_mode", true)
            paymentIntent.putExtra("selected_payment_method", selectedPaymentMethod)
            paymentMethodPickerLauncher.launch(paymentIntent)
        }

        // Sự kiện thay đổi địa chỉ
        lblAddress.setOnClickListener {
            startActivity(Intent(this, savedeliveryaddress::class.java))
        }

        valAddress.setOnClickListener {
            startActivity(Intent(this, savedeliveryaddress::class.java))
        }

        btnCancel.setOnClickListener {
            finish()
        }

        // Feature: Tạo đơn hàng khi nhấn nút "Đặt hàng"
        btnOrder.setOnClickListener {
            createOrderFromCart()
        }

        btnBack.setOnClickListener { finish() }
    }

    /**
     * Hiển thị danh sách các món ăn trong đơn hàng hiện tại
     */
    private fun bindCartPreview() {
        val tvTotal = findViewById<TextView>(R.id.valTotal)
        val tvRestaurantName = findViewById<TextView>(R.id.tvRestaurantName)
        val cartItemsContainer = findViewById<android.widget.LinearLayout>(R.id.cartItemsContainer)

        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        baseTotalPrice = selectedItems.sumOf { it.qty * it.price }

        cartItemsContainer.removeAllViews()

        if (selectedItems.isEmpty()) {
            // Hiển thị trạng thái trống nếu không có món nào
            return
        }

        // Tạo view cho từng món ăn và thêm vào layout
        for (item in selectedItems) {
            val itemView = android.view.LayoutInflater.from(this)
                .inflate(R.layout.order_cart_item, null, false)
            
            val imgCartItem = itemView.findViewById<ImageView>(R.id.imgCartItem)
            val tvCartItemName = itemView.findViewById<TextView>(R.id.tvCartItemName)
            val tvCartItemQty = itemView.findViewById<TextView>(R.id.tvCartItemQty)
            val tvCartItemPrice = itemView.findViewById<TextView>(R.id.tvCartItemPrice)

            tvCartItemName.text = item.name
            tvCartItemQty.text = "Số lượng: ${item.qty}"
            tvCartItemPrice.text = "Thành tiền: ${fmt.format(item.qty * item.price)}đ"

            if (!item.imageUrl.isNullOrEmpty()) {
                Picasso.get().load(item.imageUrl).into(imgCartItem)
            }

            cartItemsContainer.addView(itemView)
        }

        tvTotal.text = "${fmt.format(baseTotalPrice)}đ"
        bindVoucherAndTotal()
    }

    /**
     * Tính toán và hiển thị giá trị giảm giá và tổng tiền cuối cùng
     */
    private fun bindVoucherAndTotal() {
        val voucherValueText = findViewById<TextView>(R.id.tvVoucherValue)
        val tvTotal = findViewById<TextView>(R.id.valTotal)
        val discountAmount = calculateDiscountAmount(baseTotalPrice)
        val payableTotal = (baseTotalPrice - discountAmount).coerceAtLeast(0)
        val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        voucherValueText.text = if (!selectedPromotionCode.isNullOrBlank() && selectedDiscountValue > 0) {
            "$selectedPromotionCode (-${fmt.format(discountAmount)}đ)"
        } else {
            "Chọn >"
        }

        tvTotal.text = "${fmt.format(payableTotal)}đ"
    }

    /**
     * Hiển thị tên phương thức thanh toán đã chọn
     */
    private fun bindPaymentMethod() {
        findViewById<TextView>(R.id.tvPaymentMethodValue).text = selectedPaymentMethodLabel
    }

    private fun calculateDiscountAmount(total: Int): Int {
        if (selectedDiscountValue <= 0) return 0
        return if ((selectedDiscountType ?: "").equals("percent", ignoreCase = true)) {
            ((total * selectedDiscountValue) / 100.0).toInt()
        } else {
            selectedDiscountValue
        }
    }

    /**
     * Feature: Lấy danh sách địa chỉ của người dùng từ API
     */
    private fun loadUserAddress() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId <= 0) return

        RetrofitClient.apiService.getUserAddresses(userId).enqueue(object : Callback<List<Address>> {
            override fun onResponse(call: Call<List<Address>>, response: Response<List<Address>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    currentAddress = response.body()!!.first()
                    findViewById<TextView>(R.id.valAddress).text = currentAddress!!.detail
                }
            }
            override fun onFailure(call: Call<List<Address>>, t: Throwable) {}
        })
    }

    /**
     * Xác định restaurantId dựa trên các món ăn trong giỏ hàng
     */
    private fun resolveRestaurantId() {
        // ... implementation (giữ nguyên logic gốc)
    }

    /**
     * Feature: Tải thông tin đơn hàng cũ để thực hiện đặt lại (Reorder)
     */
    private fun loadReorderSourceOrder(orderId: Int) {
        RetrofitClient.apiService.getOrderDetail(orderId).enqueue(object : Callback<com.example.myapp.screens.api.OrderDetailResponse> {
            override fun onResponse(call: Call<com.example.myapp.screens.api.OrderDetailResponse>, response: Response<com.example.myapp.screens.api.OrderDetailResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val sourceOrder = response.body()!!
                    selectedItems.clear()
                    selectedItems.addAll(sourceOrder.order_items.map { item ->
                        CartItem(id = item.menuitemid, name = item.menuitem_name ?: "", price = item.price, qty = item.quantity, imageUrl = item.image_url)
                    })
                    restaurantId = sourceOrder.restaurantid
                    bindCartPreview()
                }
            }
            override fun onFailure(call: Call<com.example.myapp.screens.api.OrderDetailResponse>, t: Throwable) {}
        })
    }

    /**
     * Feature: Gọi API để tạo đơn hàng mới
     */
    private fun createOrderFromCart() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId <= 0) {
            Toast.makeText(this, "Bạn cần đăng nhập lại", Toast.LENGTH_SHORT).show()
            return
        }

        val address = currentAddress
        if (address == null) {
            Toast.makeText(this, "Vui lòng thêm địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
            return
        }

        val totalPrice = selectedItems.sumOf { it.qty * it.price }
        val orderItems = selectedItems.map {
            OrderItemRequest(quantity = it.qty, price = it.price, menuitemid = it.id)
        }

        // Chuẩn bị dữ liệu yêu cầu tạo đơn hàng
        val request = OrderCreateRequest(
            status = "pending",
            totalprice = totalPrice,
            restaurantid = restaurantId,
            addressid = address.id,
            userid = userId,
            order_items = orderItems
        )

        // Thực hiện gọi API createOrder
        RetrofitClient.apiService.createOrder(request).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val createdOrder = response.body()!!
                    // Xóa các món đã đặt khỏi giỏ hàng
                    cart.cartList.removeAll(selectedItems.toSet())
                    
                    if (selectedPaymentMethod.equals("online", ignoreCase = true)) {
                        // Chuyển sang thanh toán online nếu chọn
                        val intent = Intent(this@order, payment_methods::class.java)
                        intent.putExtra("order_id", createdOrder.id)
                        intent.putExtra("auto_pay_now", true)
                        startActivity(intent)
                    } else {
                        // Chuyển sang màn hình thông báo thành công cho COD
                        val intent = Intent(this@order, payment_successful::class.java)
                        intent.putExtra("order_id", createdOrder.id)
                        startActivity(intent)
                    }
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                Toast.makeText(this@order, "Lỗi mạng khi tạo đơn", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
