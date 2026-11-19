# Gemini Guard

Ứng dụng Android giữ **Gemini (Google)** luôn là trợ lý mặc định trên máy Xiaomi / HyperOS, đồng thời thêm một số tiện ích quanh thao tác gọi trợ lý: thanh điều hướng nổi, tự gọi Gemini khi Mi AI xuất hiện, hiệu ứng âm thanh / rung và phím tắt bật–tắt Trợ năng.

- **Không cần root.**
- **Không thu thập hay gửi dữ liệu ra ngoài.** Mọi cài đặt và nhật ký đều lưu cục bộ trên máy.
- Yêu cầu Android 8.0 trở lên (`minSdk 26`), biên dịch với Android SDK 34.

> **Lưu ý:** nhiều ứng dụng ngân hàng và VNeID sẽ cảnh báo hoặc chặn khi dịch vụ Trợ năng đang bật. Hãy tắt mục Trợ năng của Gemini Guard trước khi dùng các ứng dụng đó.

---

## Tính năng

### 1. Giữ Gemini làm trợ lý hệ thống
- Tự ghi lại các cài đặt `assistant` và `voice_interaction_service` nếu HyperOS xóa hoặc đổi sang Mi AI.
- Tùy chọn ghi đè hành vi **giữ nút nguồn** để luôn mở Gemini.
- Tùy chọn **tắt → bật lại Google** sau khi khởi động máy.
- Nút khôi phục ngay và nút khởi động lại trợ lý khi cần.

### 2. Thanh điều hướng AI
- Thanh nổi nhỏ ở đáy màn hình, có thể **kéo lên/xuống** để đổi vị trí; vị trí được lưu lại.
- Tùy chỉnh chiều rộng, chiều cao, độ đậm và khoảng cách tới đáy.
- Gán hành động cho **chạm đúp** và **giữ**:
  - Gọi Gemini
  - Mở MiCTS (Circle to Search)
  - Dịch màn hình Xiaomi
  - Không làm gì

### 3. Gọi Gemini khi Mi AI xuất hiện
- Phát hiện Mi AI qua Trợ năng, đóng cửa sổ Mi AI rồi gọi trợ lý thay thế.
- Lọc theo tên cửa sổ (chỉ gọi khi là lớp phủ giọng nói, bỏ qua khi mở app / dịch màn hình).
- Điều chỉnh vị trí chạm, thời gian chờ và độ trễ gọi.
- Hỗ trợ chọn **Gemini** hoặc **ChatGPT** làm trợ lý đích cho thao tác tự gọi.

### 4. Âm thanh và rung
- Phát hiệu ứng âm thanh / rung nhẹ khi cửa sổ trợ lý hiện lên.
- Tùy chỉnh âm lượng, khuếch đại, bỏ qua đoạn đầu và chống lặp.
- Tự bỏ qua hiệu ứng khi máy đang ở chế độ **Không làm phiền** (DND). Nút “Thử” vẫn phát để kiểm tra loa.

### 5. Phím tắt Trợ năng
- Ô **Trợ năng Gemini Guard** trong Control Center.
- Hoặc nhấn giữ icon app → **Bật/tắt Trợ năng**.
- Chỉ tắt dịch vụ Trợ năng của Gemini Guard, không ảnh hưởng dịch vụ Trợ năng khác.

---

## Cài đặt

### Bước 1 — Cài APK
Tải APK từ mục **Actions** (artifact `GeminiGuard-apk`) hoặc từ **Releases** nếu có, rồi cài lên máy.

- Nếu đang dùng bản cũ: **cài đè, không gỡ ứng dụng**, để giữ quyền ADB đã cấp.
- Gỡ ứng dụng sẽ làm mất quyền `WRITE_SECURE_SETTINGS`; khi cài lại phải cấp lại bằng ADB.

### Bước 2 — Cấp quyền ghi cài đặt bảo mật
Bật **Gỡ lỗi USB**, kết nối máy với máy tính rồi chạy:

```bash
adb shell pm grant com.gemini.guard android.permission.WRITE_SECURE_SETTINGS
```

### Bước 3 — Mở ứng dụng và bật các mục cần dùng
Tùy nhu cầu, cấp thêm:
- **Hiển thị trên ứng dụng khác** (cho thanh điều hướng).
- **Trợ năng** (cho tính năng theo dõi Mi AI).
- **Thông báo** (để thấy trạng thái canh nền).

---

## Cấp quyền trên HyperOS / MIUI

Nếu `pm grant` báo `SecurityException` hoặc `GRANT_RUNTIME_PERMISSIONS`, hãy bật:

**Cài đặt → Cài đặt bổ sung → Tùy chọn nhà phát triển → Gỡ lỗi USB (Cài đặt bảo mật)**

Có thể cần đăng nhập Mi Account và lắp SIM. Sau đó chạy lại lệnh `pm grant`.

---

## Build từ mã nguồn

### Yêu cầu
- JDK 17
- Android SDK Platform 34
- Gradle wrapper có sẵn trong repo (Gradle 8.9)

### Lệnh build

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

APK đầu ra:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Nếu Gradle không tìm thấy SDK, tạo `local.properties`:

```properties
sdk.dir=D:\\AppData
```

hoặc đặt biến môi trường `ANDROID_HOME` / `ANDROID_SDK_ROOT`.

### CI
Workflow `.github/workflows/build.yml` tự build APK khi push, pull request hoặc chạy thủ công. Khi push tag `v*`, workflow tạo GitHub Release kèm APK.

---

## Ký ứng dụng

Để bản mới cài đè được bản cũ, **phải dùng cùng keystore**. Repo có sẵn `app/guard.keystore` và mật khẩu mặc định để bảo đảm điều này.

Có thể cấu hình riêng bằng `keystore.properties` (không commit):

```properties
storeFile=guard.keystore
storePassword=...
keyAlias=guard
keyPassword=...
```

Hoặc dùng biến môi trường:

```text
GUARD_STORE_PASSWORD
GUARD_KEY_ALIAS
GUARD_KEY_PASSWORD
```

Thứ tự ưu tiên: `keystore.properties` → biến môi trường → giá trị mặc định.

> **Cảnh báo bảo mật:** keystore và mật khẩu mặc định đang nằm công khai trong repo, nên bất kỳ ai cũng có thể ký một bản “cài đè” cùng chữ ký. Nếu cần an toàn hơn, hãy tạo keystore riêng, đưa mật khẩu vào GitHub Secrets và chấp nhận gỡ ứng dụng một lần (sau đó cấp lại quyền ADB).

---

## Quyền riêng tư

Gemini Guard chỉ đọc thông tin cửa sổ / tên gói để quyết định có gọi trợ lý hay không. Dữ liệu này được dùng cục bộ, không gửi lên máy chủ nào. Ứng dụng không có quyền truy cập mạng để gửi dữ liệu ra ngoài.

---

## Xử lý sự cố

| Hiện tượng | Cách xử lý |
| --- | --- |
| `pm grant` báo `SecurityException` | Bật **Gỡ lỗi USB (Cài đặt bảo mật)** trên HyperOS rồi chạy lại. |
| Trạng thái báo trợ lý mặc định chưa đúng | Kiểm tra đã cấp `WRITE_SECURE_SETTINGS`, bật “Canh nền”, rồi mở lại ứng dụng. |
| Thanh điều hướng không hiện | Cấp quyền **Hiển thị trên ứng dụng khác**. |
| Mi AI không tự đóng | Bật Trợ năng, tăng “Chờ trước khi chạm đóng Mi AI” và kiểm tra vị trí chạm. |
| Không có âm thanh | Kiểm tra “Phát âm thanh”, âm lượng, và chế độ Không làm phiền. |
| Sau khi cài lại mất quyền ADB | Gỡ ứng dụng sẽ mất quyền; cấp lại bằng lệnh `pm grant`. |

---

## Cấu trúc chính

```text
app/src/main/java/com/gemini/guard/
├── MainActivity.kt      # Giao diện chính
├── Restorer.kt          # Ghi / khôi phục cài đặt hệ thống
├── GuardService.kt      # Foreground service và thông báo trạng thái
├── MiAiWatcher.kt       # Trợ năng: phát hiện Mi AI / trợ lý
├── BarOverlay.kt        # Thanh điều hướng nổi
├── GeminiSound.kt       # Hiệu ứng âm thanh
├── AccToggle.kt         # Bật/tắt riêng dịch vụ Trợ năng
├── AccTileService.kt    # Ô Quick Settings
└── L.kt                 # Chuỗi giao diện Việt / Anh
```
