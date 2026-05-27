# food-order-app
BTL Phát triển ứng dụng cho các thiết bị di động

B22DCAT283 Phạm Đức Thành - Nhóm BTL 07 - Nhóm QLDT 02

Dưới đây là mô tả các file liên quan đến nội dung em thực hiện, đồng thời trong các file này cũng đã có đầy đủ các Comments giải thích các lớp, hàm hoặc API nếu có.

LT_android/project/app/src/main/java/com/example/myapp/api/
- ApiService.kt: Định nghĩa các Endpoint quan trọng như đăng nhập, đăng ký, tạo đơn hàng, chi tiết nhà hàng hay chi tiết món ăn.
- RetrofitClient.kt: Thiết lập trình kết nối mạng; tự động đính kèm Token xác thực vào tiêu đề (Header) của mỗi yêu cầu để bảo mật thông tin.

Các file (.kt): LT_android/project/app/src/main/java/com/example/myapp/screens/
Các file (.xml): LT_android/project/app/src/main/res/layout
- start.kt / .xml: Màn hình chào mừng và khởi tạo ứng dụng; xử lý cấp quyền và lấy FCM Token để nhận thông báo đẩy.
- signin.kt / .xml: Xử lý logic và giao diện đăng nhập; thực hiện xác thực với Server, lưu trữ mã truy cập (Token) và thông tin người dùng vào bộ nhớ đệm (SharedPreferences).
- signup.kt / .xml: Xử lý logic và giao diện đăng ký tài khoản mới; kiểm tra tính hợp lệ dữ liệu và gửi yêu cầu tạo người dùng tới hệ thống.
- food_detail.kt / .xml: Hiển thị toàn bộ thông tin món ăn, đánh giá của người dùng; là điểm điều hướng chính để thêm món vào giỏ hàng thường hoặc giỏ hàng đặt trước.
- cart.kt & activity_cart.kt / .xml: Quản lý giỏ hàng hiện tại; tính toán tổng tiền, cho phép thay đổi số lượng, xóa món và chuẩn bị dữ liệu cho quá trình thanh toán.
- item_cart.xml & CartItemAdapter.kt: Định nghĩa cấu trúc hiển thị và cách tương tác (tăng/giảm/xóa) của từng món ăn cụ thể trong danh sách giỏ hàng.
- order.kt & order.xml: Màn hình xác nhận đơn hàng cuối cùng; tích hợp chọn địa chỉ giao hàng, áp dụng mã giảm giá, chọn phương thức thanh toán và tạo đơn hàng chính thức.
- pre_order.kt / .xml: Xử lý chức năng đặc thù cho phép người dùng lên lịch nhận hàng (chọn ngày và giờ trong tương lai) với các ràng buộc về thời gian tối thiểu.
- pre_order_cart.kt / .xml: Quản lý danh sách các món ăn đã được lên lịch đặt trước, tách biệt hoàn toàn với luồng giỏ hàng mua ngay.
- item_pre_order_cart.xml: Hiển thị thông tin món ăn kèm theo thời gian giao hàng dự kiến đã chọn.
- share.kt / .xml: Đóng gói thông tin món ăn (tên, mô tả, hình ảnh) để chia sẻ trực tiếp sang các ứng dụng mạng xã hội (Facebook, Zalo, Messenger, v.v.).
- share_restaurant.kt / .xml: Tương tự như chia sẻ món ăn nhưng dành cho thông tin nhà hàng, bao gồm địa chỉ và đánh giá sao.
