GEMINI GUARD 4.0
- Upload zip nay len GitHub (cung ten GeminiGuard-v2.0.zip de ghi de, repo chi nen co 1 file zip). Actions tu build APK.
- Cai APK DE LEN ban cu (khong go app) de giu quyen ADB.
- Cap quyen 1 lan: pm grant com.gemini.guard android.permission.WRITE_SECURE_SETTINGS
- Phim tat Tro nang: o "Tro nang Gemini Guard" trong Control Center (muc Phim nhanh ung dung),
  hoac nhan giu icon app -> "Bat/tat Tro nang".

BUILD
- Repo co san Gradle wrapper 8.9. Build tay (can JDK 17 + Android SDK 34):
    gradlew.bat assembleDebug        (Windows)
    ./gradlew assembleDebug          (macOS/Linux)
  CI cung dung wrapper nay (xem .github/workflows/build.yml).
- Mat khau ky: dat trong keystore.properties (khong commit, xem keystore.properties.example)
  hoac bien moi truong GUARD_STORE_PASSWORD / GUARD_KEY_ALIAS / GUARD_KEY_PASSWORD.
  Mac dinh van dung guardpass de ban dang cai cap nhat duoc.

CAP QUYEN TREN HYPEROS/MIUI
- Neu "pm grant" bao SecurityException (GRANT_RUNTIME_PERMISSIONS), hay bat:
  Cai dat -> Cai dat bo sung -> Tuy chon nha phat trien -> "Go loi USB (Cai dat bao mat)"
  (co the can dang nhap Mi Account + SIM), roi chay lai lenh pm grant.

CANH BAO KY APP
- guard.keystore va mat khau mac dinh dang nam cong khai trong repo, nen bat ky ai cung ky duoc
  ban "cai de" cung chung thu. Muon an toan: tao keystore moi, dua vao GitHub Secrets, va chap nhan
  go app mot lan (se mat quyen WRITE_SECURE_SETTINGS, phai cap lai bang ADB).
