GEMINI GUARD 4.0
- Upload zip nay len GitHub (cung ten GeminiGuard-v2.0.zip de ghi de, repo chi nen co 1 file zip). Actions tu build APK.
- Cai APK DE LEN ban cu (khong go app) de giu quyen ADB.
- Cap quyen 1 lan: pm grant com.gemini.guard android.permission.WRITE_SECURE_SETTINGS
- Phim tat Tro nang: o "Tro nang Gemini Guard" trong Control Center (muc Phim nhanh ung dung),
  hoac nhan giu icon app -> "Bat/tat Tro nang".

BUILD
- Repo khong kem gradlew; CI dung Gradle 8.9 (xem .github/workflows/build.yml).
  Build tay: cai Gradle 8.9 + Android SDK 34 roi chay: gradle assembleDebug
- Mat khau ky: dat trong keystore.properties (khong commit, xem keystore.properties.example)
  hoac bien moi truong GUARD_STORE_PASSWORD / GUARD_KEY_ALIAS / GUARD_KEY_PASSWORD.
  Mac dinh van dung guardpass de ban dang cai cap nhat duoc.

CANH BAO KY APP
- guard.keystore va mat khau mac dinh dang nam cong khai trong repo, nen bat ky ai cung ky duoc
  ban "cai de" cung chung thu. Muon an toan: tao keystore moi, dua vao GitHub Secrets, va chap nhan
  go app mot lan (se mat quyen WRITE_SECURE_SETTINGS, phai cap lai bang ADB).
