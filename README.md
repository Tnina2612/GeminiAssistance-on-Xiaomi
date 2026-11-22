# Gemini Guard / Gemini Assistant on Xiaomi

[![Build APK](https://github.com/Tnina2612/GeminiAssistant-on-Xiaomi/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/Tnina2612/GeminiAssistant-on-Xiaomi/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/Tnina2612/GeminiAssistant-on-Xiaomi?display_name=tag&sort=semver)](https://github.com/Tnina2612/GeminiAssistant-on-Xiaomi/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Platform-Xiaomi%20%7C%20HyperOS%20%7C%20Honor-orange)](https://github.com/Tnina2612/GeminiAssistant-on-Xiaomi)

**[Tiếng Việt](#tiếng-việt) · [English](#english)**

> **Nguồn gốc / Fork notice**
>
> **Tiếng Việt:** Đây là bản fork được phát triển tiếp từ **[minhdat5122010-dev/GeminiGuard](https://github.com/minhdat5122010-dev/GeminiGuard)**. Repo gốc không có file license; xem [Giấy phép](#giấy-phép) để biết phạm vi áp dụng của MIT trong bản fork này.
>
> **English:** This project is a fork of **[minhdat5122010-dev/GeminiGuard](https://github.com/minhdat5122010-dev/GeminiGuard)**. The original repository does not include a license file; see [License](#license) for the scope of the MIT license in this fork.

---

## Tiếng Việt

### Giới thiệu

Gemini Guard giữ **Google/Gemini** làm trợ lý mặc định trên Xiaomi/HyperOS, đồng thời thêm các tiện ích quanh thao tác gọi trợ lý:

- Thanh điều hướng nổi.
- Tự đóng Mi AI/XiaoAI và gọi Gemini thay thế.
- Hiệu ứng âm thanh và rung.
- Phím tắt Control Center để bật/tắt Trợ năng hoặc gọi trợ lý.
- Chẩn đoán, sao lưu cấu hình và API điều khiển ngoài.

**Không cần root. Không thu thập hay gửi dữ liệu ra ngoài.** Mọi cài đặt và nhật ký đều lưu cục bộ trên máy.

- Yêu cầu Android 8.0 trở lên (`minSdk 26`), biên dịch với Android SDK 34.
- Hỗ trợ Xiaomi/HyperOS, Honor/MagicOS và Android khác (tự nhận diện hãng).
- Giao diện tiếng Việt / tiếng Anh.

### Tính năng

#### 1. Giữ Gemini làm trợ lý hệ thống
- Tự ghi lại `assistant` và `voice_interaction_service` nếu HyperOS xóa hoặc đổi sang Mi AI.
- Bật/tắt **riêng từng khóa** cần canh; sau mỗi lần ghi app **đọc lại để xác nhận**, nếu hệ thống đổi ngược lại thì báo rõ trong Trạng thái và bằng thông báo.
- Watchdog 2 tầng: `ContentObserver` + kiểm tra định kỳ + `JobScheduler` vẫn chạy khi service bị HyperOS kill.
- Tùy chọn ghi đè hành vi **giữ nút nguồn** để mở Gemini (lưu giá trị gốc và trả lại được menu nguồn).
- Tùy chọn **tắt → bật lại Google** sau khi khởi động máy.
- Nút khôi phục ngay, nút khởi động lại trợ lý và nút **thử gọi trợ lý** không cần giữ nút nguồn.
- **Giữ Google sẵn sàng**: bind vào service trong process `:search` của Google để giảm độ trễ khi mở trợ lý; có thể tắt nếu muốn tiết kiệm pin.

#### 2. Thanh điều hướng AI
- Thanh nổi nhỏ ở đáy màn hình, có thể **kéo lên/xuống** để đổi vị trí; vị trí được lưu lại.
- Tùy chỉnh chiều rộng, chiều cao, độ đậm và khoảng cách tới đáy.
- Gán hành động cho **chạm đúp** và **giữ**:
  - Gọi Gemini
  - Mở MiCTS (Circle to Search)
  - Dịch màn hình Xiaomi
  - Không làm gì

#### 3. Gọi Gemini khi Mi AI/XiaoAI xuất hiện
- Phát hiện Mi AI qua Trợ năng, đóng cửa sổ Mi AI rồi gọi trợ lý thay thế.
- Lọc theo tên cửa sổ (chỉ gọi khi là lớp phủ giọng nói, bỏ qua khi mở app / dịch màn hình).
- Điều chỉnh vị trí chạm, thời gian chờ và độ trễ gọi.
- Hỗ trợ chọn **Gemini**, **ChatGPT** hoặc **nhập tên gói trợ lý khác**.
- Có nút **thử gọi trợ lý** để kiểm tra cấu hình mà không cần giữ nút nguồn.
- **Tự tắt Trợ năng khi mở app ngân hàng / VNeID** (tùy chọn, mặc định tắt): dùng quyền “Truy cập sử dụng”, tự bật lại khi rời app. Có nút tự tìm app ngân hàng đã cài.
- Nếu Google được gọi nhưng không hiện lên, app gửi thêm `ACTION_ASSIST` để đưa giao diện trợ lý lên foreground.

#### 4. Âm thanh và rung
- Phát hiệu ứng âm thanh / rung nhẹ khi cửa sổ trợ lý hiện lên.
- Tùy chỉnh âm lượng, khuếch đại, bỏ qua đoạn đầu và chống lặp.
- Chọn được **âm thanh riêng** từ file của bạn.
- Tự bỏ qua hiệu ứng khi máy đang ở chế độ **Không làm phiền** (DND). Nút “Thử” vẫn phát để kiểm tra loa.

#### 5. Phím tắt và API
- Ô **Trợ năng Gemini Guard** trong Control Center.
- Ô **Gọi trợ lý** trong Control Center.
- Hoặc nhấn giữ icon app → **Bật/tắt Trợ năng**.
- Chỉ tắt dịch vụ Trợ năng của Gemini Guard, không ảnh hưởng dịch vụ khác.
- **API điều khiển ngoài** cho ứng dụng tích hợp qua broadcast; app gửi phải khai báo quyền `com.gemini.guard.permission.SEND_COMMAND` và gửi kèm token sinh ngẫu nhiên.

#### 6. Onboarding, chẩn đoán và sao lưu
- **Thiết lập lần đầu**: danh sách bước còn thiếu (quyền ADB, overlay, Trợ năng, tự khởi động) kèm nút sửa từng bước.
- **Chia sẻ chẩn đoán**: gộp phiên bản, thiết bị, trạng thái, cài đặt đã lưu, cửa sổ gần nhất và cài đặt hệ thống liên quan thành một tin nhắn để gửi hỗ trợ.
- **Xuất / nhập cấu hình JSON** để chuyển máy hoặc hỗ trợ từ xa (không kèm log và token).

#### 7. Hỗ trợ Honor / MagicOS
- Tự nhận diện Xiaomi / Honor / hãng khác và chỉ hiện các mục phù hợp (không ghi khóa nút nguồn của Xiaomi trên Honor).
- Gợi ý tên gói YOYO, mở cài đặt trợ lý mặc định và trang tự khởi động của Honor.

### Cài đặt

#### Bước 1 — Cài APK
Tải APK từ mục **Releases** (khuyến nghị bản `GeminiGuard-v<phiên bản>-release.apk`) hoặc từ **Actions** (artifact `GeminiGuard-v<phiên bản>`), rồi cài lên máy.

- Nếu đang dùng bản cũ: **cài đè, không gỡ ứng dụng**, để giữ quyền ADB đã cấp.
- Gỡ ứng dụng sẽ làm mất quyền `WRITE_SECURE_SETTINGS`; khi cài lại phải cấp lại bằng ADB.

```bash
adb install -r GeminiGuard-v4.3-release.apk
```

#### Bước 2 — Cấp quyền ghi cài đặt bảo mật
Bật **Gỡ lỗi USB**, kết nối máy với máy tính rồi chạy:

```bash
adb shell pm grant com.gemini.guard android.permission.WRITE_SECURE_SETTINGS
```

#### Bước 3 — Mở ứng dụng và bật các mục cần dùng
Tùy nhu cầu, cấp thêm:
- **Hiển thị trên ứng dụng khác** (cho thanh điều hướng).
- **Trợ năng** (cho tính năng theo dõi Mi AI).
- **Thông báo** (để thấy trạng thái canh nền).
- **Truy cập sử dụng** (chỉ khi bật tự tắt Trợ năng cho app ngân hàng).

### Cấp quyền trên HyperOS / MIUI

Nếu `pm grant` báo `SecurityException` hoặc `GRANT_RUNTIME_PERMISSIONS`, hãy bật:

**Cài đặt → Cài đặt bổ sung → Tùy chọn nhà phát triển → Gỡ lỗi USB (Cài đặt bảo mật)**

Có thể cần đăng nhập Mi Account và lắp SIM. Sau đó chạy lại lệnh `pm grant`.

Nên bật thêm cho Gemini Guard và Google:
- **Tự khởi động / Autostart**.
- **Tiết kiệm pin → Không hạn chế**.
- Khóa ứng dụng trong đa nhiệm gần đây nếu HyperOS hay kill.

### Build từ mã nguồn

#### Yêu cầu
- JDK 17
- Android SDK Platform 34
- Gradle wrapper có sẵn trong repo (Gradle 8.9)

#### Lệnh build

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleDebug
./gradlew assembleRelease
```

APK đầu ra:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

Số phiên bản nằm trong `version.properties` (`versionCode`, `versionName`) nên build lại vẫn ra cùng số, không phụ thuộc đồng hồ máy. Có thể ghi đè tạm thời bằng `-PversionCode=... -PversionName=...`.

Kiểm thử và lint:

```bash
./gradlew testDebugUnitTest lintDebug
```

### Ký ứng dụng

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

### CI / Release

Workflow `.github/workflows/build.yml`:

- Chạy **unit test + lint**.
- Build cả **debug** và **release**.
- Đặt tên artifact theo `versionName` (ví dụ `GeminiGuard-v4.3`).
- Khi push tag `v*` (ví dụ `v4.3`), tự tạo GitHub Release kèm APK debug và release.

```bash
git tag v4.3
git push origin v4.3
```

### Quyền riêng tư

Gemini Guard chỉ đọc thông tin cửa sổ / tên gói để quyết định có gọi trợ lý hay không. Tính năng tự tắt Trợ năng cho app ngân hàng (nếu bật) chỉ đọc tên gói app đang mở bằng Usage Access, xử lý cục bộ. Dữ liệu này không gửi lên máy chủ nào. Ứng dụng **không có quyền truy cập mạng** để gửi dữ liệu ra ngoài.

> Wake-word “小爱同学” do XiaoAI/VoiceTrigger của Xiaomi xử lý, không phải Gemini Guard. Chất lượng nhận diện phụ thuộc voiceprint, cách phát âm, mic và tính năng Smart wake response của Xiaomi.

### Xử lý sự cố

| Hiện tượng | Cách xử lý |
| --- | --- |
| `pm grant` báo `SecurityException` | Bật **Gỡ lỗi USB (Cài đặt bảo mật)** trên HyperOS rồi chạy lại. |
| Trạng thái báo trợ lý mặc định chưa đúng | Kiểm tra đã cấp `WRITE_SECURE_SETTINGS`, bật “Canh nền”, rồi mở lại ứng dụng. |
| Thông báo “không giữ được cài đặt” | Hệ thống liên tục đổi ngược lại; xem mục Trạng thái, bật đủ hai khóa canh, hoặc dùng “Chia sẻ chẩn đoán”. |
| Thanh điều hướng không hiện | Cấp quyền **Hiển thị trên ứng dụng khác**. |
| Mi AI không tự đóng | Bật Trợ năng, tăng “Chờ trước khi chạm đóng Mi AI” và kiểm tra vị trí chạm. |
| Không có âm thanh | Kiểm tra “Phát âm thanh”, âm lượng, và chế độ Không làm phiền. |
| Gọi trợ lý chậm | Bật “Giữ Google sẵn sàng”, đặt Google/Gemini Guard thành **Không hạn chế** pin và bật Autostart. |
| Wake-word XiaoAI nhận kém | Thu lại **Record greeting again** trong XiaoAI, kiểm tra voiceprint/cách phát âm, tắt voice wake trên thiết bị Xiaomi khác nếu có. |
| Sau khi cài lại mất quyền ADB | Gỡ ứng dụng sẽ mất quyền; cấp lại bằng lệnh `pm grant`. |

### Cấu trúc chính

```text
app/src/main/java/com/gemini/guard/
├── MainActivity.kt      # Giao diện chính
├── Restorer.kt          # Ghi / khôi phục cài đặt hệ thống, verify sau khi ghi
├── GuardService.kt      # Foreground service và thông báo trạng thái
├── WatchdogJob.kt       # Watchdog tầng 2 bằng JobScheduler
├── MiAiWatcher.kt       # Trợ năng: phát hiện Mi AI / trợ lý
├── Filters.kt           # Lọc cửa sổ (có unit test)
├── GoogleWarmup.kt      # Giữ process :search của Google ấm
├── BarOverlay.kt        # Thanh điều hướng nổi
├── GeminiSound.kt       # Hiệu ứng âm thanh
├── Diagnostics.kt       # Tổng hợp và chia sẻ thông tin chẩn đoán
├── Backup.kt            # Xuất / nhập cấu hình JSON
├── CommandReceiver.kt   # API broadcast cho app tích hợp (permission + token)
├── BankGuard.kt         # Tự tắt Trợ năng cho app ngân hàng/VNeID
├── DeviceProfile.kt     # Nhận diện Xiaomi / Honor / khác
├── AccToggle.kt         # Bật/tắt riêng dịch vụ Trợ năng
├── AccTileService.kt    # Ô Quick Settings bật/tắt Trợ năng
├── AssistTileService.kt # Ô Quick Settings gọi trợ lý
└── L.kt                 # Chuỗi giao diện Việt / Anh
```

### Giấy phép

Bản fork này phát hành theo **MIT License** — xem file [LICENSE](LICENSE).

Repo gốc `minhdat5122010-dev/GeminiGuard` **không có file license**. MIT trong repo này áp dụng cho các thay đổi của bản fork; quyền đối với mã gốc vẫn thuộc tác giả gốc. Nếu bạn là tác giả gốc và muốn điều chỉnh cách ghi công hoặc giấy phép, hãy mở issue.

---

## English

### About

Gemini Guard keeps **Google/Gemini** as the default assistant on Xiaomi/HyperOS and adds useful tools around invoking it:

- A floating navigation bar.
- Automatically closes Mi AI/XiaoAI and opens Gemini instead.
- Sound and vibration feedback.
- Quick Settings tiles to toggle Accessibility or call the assistant.
- Diagnostics, JSON backup/restore and an external control API.

**No root. No data collection or upload.** All settings and logs stay on the device.

- Android 8.0+ (`minSdk 26`), compiled with Android SDK 34.
- Xiaomi/HyperOS, Honor/MagicOS and other Android devices (vendor auto-detection).
- Vietnamese / English UI.

### Features

#### 1. Keep Gemini as the system assistant
- Restores `assistant` and `voice_interaction_service` if HyperOS clears them or switches to Mi AI.
- Per-key watch toggles; after every write the app **reads the value back**, reports failures, and sends a notification when the system keeps reverting it.
- Two-layer watchdog: `ContentObserver` + periodic checks + `JobScheduler` that survives HyperOS killing the service.
- Optional **power-button long-press override** to open Gemini (the original value is saved and can be restored, including the power menu).
- Optional **Google off → on** cycle after boot.
- Restore-now, restart-assistant and **test assistant** actions that do not require holding the power button.
- **Keep Google warm**: binds to a service in Google's `:search` process to reduce assistant launch delay; can be disabled to save battery.

#### 2. Floating AI bar
- Small floating bar at the bottom; **drag up/down** to change position, persisted across restarts.
- Custom width, height, opacity and bottom margin.
- Assign actions to **double tap** and **long press**:
  - Call Gemini
  - Open MiCTS (Circle to Search)
  - Xiaomi screen translation
  - Do nothing

#### 3. Replace Mi AI/XiaoAI with Gemini
- Detects Mi AI via Accessibility, closes its window and invokes the selected assistant.
- Window-class filters (only voice overlays; skips the full app / translation screen).
- Adjustable tap position, wait time and call delay.
- Target **Gemini**, **ChatGPT**, or a **custom assistant package**.
- **Test assistant** button for checking the setup without the power button.
- Optional **auto-disable Accessibility for banking / VNeID apps** using Usage Access; re-enables it when you leave. Can auto-find installed banking apps.
- If Google is invoked but its window does not appear, the app sends `ACTION_ASSIST` to bring the assistant UI to the foreground.

#### 4. Sound and vibration
- Plays a sound / short vibration when the assistant window appears.
- Adjustable volume, gain, initial skip and repeat guard.
- Supports a **custom sound file**.
- Skips feedback in **Do Not Disturb**; the “Test” buttons still play.

#### 5. Tiles and API
- **Gemini Guard Accessibility** Quick Settings tile.
- **Call assistant** Quick Settings tile.
- Long-press the app icon → **Toggle Accessibility**.
- Only toggles Gemini Guard's Accessibility service, not other services.
- **External control API** for integration apps via broadcasts; the sender must declare `com.gemini.guard.permission.SEND_COMMAND` and include a random token.

#### 6. Onboarding, diagnostics and backup
- **First-time setup** checklist with actions for missing permissions (ADB, overlay, Accessibility, autostart).
- **Share diagnostics**: version, device, status, saved settings, recent windows and related system settings in one shareable message.
- **JSON export/import** for moving devices or remote support (logs and tokens excluded).

#### 7. Honor / MagicOS support
- Auto-detects Xiaomi / Honor / other vendors and hides Xiaomi-only options (e.g. the power-key override on Honor).
- Suggests YOYO package names, opens default-assistant settings and Honor autostart pages.

### Install

#### Step 1 — Install the APK
Download the APK from **Releases** (recommended: `GeminiGuard-v<version>-release.apk`) or from **Actions** (artifact `GeminiGuard-v<version>`).

- Upgrading: **install over the existing app, do not uninstall**, so the ADB permission is preserved.
- Uninstalling removes `WRITE_SECURE_SETTINGS`; you must grant it again with ADB.

```bash
adb install -r GeminiGuard-v4.3-release.apk
```

#### Step 2 — Grant secure-settings permission
Enable **USB debugging**, connect the phone and run:

```bash
adb shell pm grant com.gemini.guard android.permission.WRITE_SECURE_SETTINGS
```

#### Step 3 — Open the app and enable what you need
Optionally grant:
- **Display over other apps** (for the floating bar).
- **Accessibility** (for Mi AI detection).
- **Notifications** (for the foreground status).
- **Usage access** (only for the banking auto-disable feature).

### HyperOS / MIUI permissions

If `pm grant` fails with `SecurityException` or `GRANT_RUNTIME_PERMISSIONS`, enable:

**Settings → Additional settings → Developer options → USB debugging (Security settings)**

You may need to sign in to a Mi Account and insert a SIM. Then run `pm grant` again.

Also recommended for Gemini Guard and Google:
- **Autostart**.
- **Battery saver → No restrictions**.
- Lock them in recents if HyperOS keeps killing them.

### Build from source

#### Requirements
- JDK 17
- Android SDK Platform 34
- Gradle wrapper included (Gradle 8.9)

#### Commands

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleDebug
./gradlew assembleRelease
```

Outputs:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

The version lives in `version.properties` (`versionCode`, `versionName`), so rebuilds are deterministic. Override temporarily with `-PversionCode=... -PversionName=...`.

Tests and lint:

```bash
./gradlew testDebugUnitTest lintDebug
```

### Signing

To install over an existing build, **use the same keystore**. The repo includes `app/guard.keystore` and default credentials for this purpose.

Override with a local `keystore.properties` (not committed):

```properties
storeFile=guard.keystore
storePassword=...
keyAlias=guard
keyPassword=...
```

or environment variables:

```text
GUARD_STORE_PASSWORD
GUARD_KEY_ALIAS
GUARD_KEY_PASSWORD
```

Priority: `keystore.properties` → environment variables → defaults.

> **Security warning:** the keystore and default password are public in this repo, so anyone can sign an “update” with the same certificate. For stronger security, create your own keystore, move the password to GitHub Secrets, and accept one uninstall/reinstall (which requires granting ADB permission again).

### CI / Release

`.github/workflows/build.yml`:

- Runs **unit tests + lint**.
- Builds both **debug** and **release** APKs.
- Names the artifact after `versionName` (e.g. `GeminiGuard-v4.3`).
- On a `v*` tag (e.g. `v4.3`), creates a GitHub Release with both APKs.

```bash
git tag v4.3
git push origin v4.3
```

### Privacy

Gemini Guard only reads window/package information to decide whether to invoke the assistant. The optional banking auto-disable feature reads the foreground package through Usage Access and processes it locally. Nothing is sent to a server. The app has **no network permission**.

> The “小爱同学” wake word is handled by Xiaomi's XiaoAI/VoiceTrigger, not by Gemini Guard. Recognition quality depends on the voiceprint, pronunciation, microphone and Xiaomi's Smart wake response.

### Troubleshooting

| Symptom | Fix |
| --- | --- |
| `pm grant` throws `SecurityException` | Enable **USB debugging (Security settings)** on HyperOS and retry. |
| Default assistant reported as wrong | Check `WRITE_SECURE_SETTINGS`, enable “Background watch”, then reopen the app. |
| “Cannot keep the settings” notification | The system keeps reverting them; check Status, enable both watch keys, or use “Share diagnostics”. |
| Floating bar not visible | Grant **Display over other apps**. |
| Mi AI is not closed | Enable Accessibility, increase “Wait before tapping Mi AI” and check the tap position. |
| No sound | Check “Play sound”, volume and Do Not Disturb. |
| Assistant is slow to appear | Enable “Keep Google warm”, set Google/Gemini Guard battery to **No restrictions** and enable Autostart. |
| XiaoAI wake word is unreliable | Re-record the greeting in XiaoAI, check voiceprint/pronunciation, and disable voice wake on other Xiaomi devices. |
| ADB permission lost after reinstall | Uninstalling removes it; grant it again with `pm grant`. |

### Project structure

```text
app/src/main/java/com/gemini/guard/
├── MainActivity.kt      # Main UI
├── Restorer.kt          # System setting writes/restores + verification
├── GuardService.kt      # Foreground service and status notification
├── WatchdogJob.kt       # Layer-2 watchdog via JobScheduler
├── MiAiWatcher.kt       # Accessibility: Mi AI / assistant detection
├── Filters.kt           # Window filters (unit tested)
├── GoogleWarmup.kt      # Keeps Google's :search process warm
├── BarOverlay.kt        # Floating navigation bar
├── GeminiSound.kt       # Sound effects
├── Diagnostics.kt       # Diagnostics collection/sharing
├── Backup.kt            # JSON export/import
├── CommandReceiver.kt   # Broadcast API for integration apps (permission + token)
├── BankGuard.kt         # Auto-disable Accessibility for banking/VNeID
├── DeviceProfile.kt     # Xiaomi / Honor / other detection
├── AccToggle.kt         # Toggle only Gemini Guard Accessibility
├── AccTileService.kt    # Accessibility Quick Settings tile
├── AssistTileService.kt # Assistant Quick Settings tile
└── L.kt                 # Vietnamese / English strings
```

### License

This fork is released under the **MIT License** — see [LICENSE](LICENSE).

The original project `minhdat5122010-dev/GeminiGuard` **does not include a license file**. The MIT license here applies to this fork's changes; rights to the original code remain with its original author. If you are the original author and want different attribution or licensing, please open an issue.
