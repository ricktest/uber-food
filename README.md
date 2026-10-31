# Uber 畫面讀取（UberReader）

用 Android 無障礙服務（AccessibilityService）讀取 **Uber / Uber Driver / Uber Eats** 畫面上的文字和通知，
存在手機本機，方便先看清楚「訂單進來時畫面上到底有哪些內容」。

目前只做「看」：不朗讀、不幫你點按鈕、不上傳任何資料。

## 功能

- 監聽 `com.ubercab`、`com.ubercab.driver`、`com.ubercab.eats` 三個 App，其他 App 一律不讀
- 畫面變化穩定 0.8 秒後擷取一次，相同畫面不重複存
- 每行格式：`文字 〔無障礙描述〕  #元件id  [可點]`，縮排代表畫面階層
- 同時記錄 Uber 發出的通知（標題、內容）
- 主畫面可「重新整理 / 分享 / 清除」紀錄；紀錄檔超過 2MB 自動砍掉舊的一半

## 怎麼 build

### 方法 A：GitHub 雲端 build（本機什麼都不用裝，推薦）

專案已附 `.github/workflows/build.yml`，推上 GitHub 就會自動編譯出 APK。

1. 到 https://github.com/new 建一個 **Private** repo，例如 `uber-reader`（不要勾選加 README）
2. 在 `C:\專案\uber-reader` 執行：
   ```bash
   git init
   git add .
   git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/<你的帳號>/uber-reader.git
   git push -u origin main
   ```
3. 到 repo 頁面 → **Actions** 分頁 → 點最新一次「Build APK」→ 等約 3～5 分鐘變綠勾
4. 頁面最下方 **Artifacts** → 下載 `uber-reader-apk`（zip，裡面是 `app-debug.apk`）
5. 把 `app-debug.apk` 傳到手機（LINE 傳給自己、Google Drive 都可以），點開安裝；
   手機會問「允許安裝未知來源的應用程式」→ 允許

之後改了程式，再 `git add . && git commit -m "..." && git push`，Actions 會自動重 build。

### 方法 B：本機只裝命令列工具（約 1GB，比 Android Studio 小很多）

1. 裝 JDK 17：https://adoptium.net/ 下載 Temurin 17 安裝
2. 下載 Android「Command line tools only」：https://developer.android.com/studio#command-line-tools-only
   解壓到 `C:\android-sdk\cmdline-tools\latest\`
3. 安裝 SDK 套件：
   ```bash
   C:\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```
4. 在專案資料夾建 `local.properties`，內容：`sdk.dir=C\:\\android-sdk`
5. 裝 Gradle 8.7（https://gradle.org/releases/ 解壓後加進 PATH），然後：
   ```bash
   gradle :app:assembleDebug
   ```
6. APK 在 `app\build\outputs\apk\debug\app-debug.apk`，傳到手機安裝

### 4. 開啟無障礙權限

1. 打開「Uber 畫面讀取」App，按「開啟權限」
2. 在無障礙設定裡找 **Uber 畫面讀取服務** → 開啟
3. **Android 13 以上** 如果開關是灰色、顯示「受限制的設定」：
   到 **設定 → 應用程式 → Uber 畫面讀取 → 右上角 ⋮ → 允許受限制的設定**，再回去開

### 5. 使用

1. 打開 Uber（或 Uber Driver），正常使用，等訂單進來
2. 切回「Uber 畫面讀取」，就能看到擷取的內容
3. 按「分享」可以把紀錄傳到 LINE / 信箱，方便貼給我分析要抓哪些欄位

## 下一步（看完內容之後）

找出訂單畫面上關鍵欄位的 `#元件id`（例如上車地點、金額、距離），之後可以：

- 訂單進來時用 TTS 語音大聲唸出重點
- 用懸浮視窗以超大字顯示
- 震動／音效提醒

## 注意

- Uber 改版時元件 id 可能會變，屆時要重新看紀錄調整
- 這個服務只讀畫面；若要做自動接單之類的自動操作，可能違反 Uber 使用條款，帳號有被停權風險
- 若要上架 Google Play，使用無障礙 API 需要通過 Google 的額外審查，自己側載使用則不受影響
