package kr.nanoha.buscard.caller

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import kr.nanoha.buscard.caller.auth.MobileTokenStore
import kr.nanoha.buscard.caller.core.PhoneNumberNormalizer
import kr.nanoha.buscard.caller.data.BusinessCardDatabase
import kr.nanoha.buscard.caller.data.CachedBusinessCard
import kr.nanoha.buscard.caller.data.ContactHistoryReader
import kr.nanoha.buscard.caller.data.SyncResult
import kr.nanoha.buscard.caller.overlay.BusinessCardNotification
import kr.nanoha.buscard.caller.sync.BusinessCardSyncManager
import kr.nanoha.buscard.caller.sync.SyncProgress

class MainActivity : Activity() {
    private lateinit var db: BusinessCardDatabase
    private lateinit var syncManager: BusinessCardSyncManager
    private lateinit var tokenStore: MobileTokenStore
    private lateinit var statusText: TextView
    private lateinit var cardCountText: TextView
    private lateinit var loginBadgeText: TextView
    private lateinit var lastSyncText: TextView
    private lateinit var callScreeningStateText: TextView
    private lateinit var contactsPermissionStateText: TextView
    private lateinit var phoneStatePermissionStateText: TextView
    private lateinit var callLogPermissionStateText: TextView
    private lateinit var smsPermissionStateText: TextView
    private lateinit var progressPanel: LinearLayout
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText

    private val actionButtons = mutableListOf<TextView>()
    private var currentMessage = ""
    private var renderedLoggedIn: Boolean? = null
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Palette.Page
        window.navigationBarColor = Palette.Page
        db = BusinessCardDatabase(this)
        syncManager = BusinessCardSyncManager(this)
        tokenStore = MobileTokenStore(this)
        renderUi(force = true)
        requestRequiredRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        renderUi(force = tokenStore.isLoggedIn())
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CALL_SCREENING_ROLE) return

        renderUi(force = true)
        if (resultCode == RESULT_OK || isCallScreeningRoleHeld()) {
            showStatus("발신자 표시 권한이 활성화되었습니다.")
        } else {
            showStatus("발신자 표시 권한이 선택되지 않았습니다.")
        }
    }

    private fun renderUi(force: Boolean = false) {
        val loggedIn = tokenStore.isLoggedIn()
        if (!force && renderedLoggedIn == loggedIn && ::statusText.isInitialized) {
            updateSummary()
            return
        }

        actionButtons.clear()
        renderedLoggedIn = loggedIn
        setContentView(createContentView(loggedIn))
        updateSummary()
        showStatus(currentMessage.ifBlank { defaultStatus(loggedIn) })
    }

    private fun createContentView(loggedIn: Boolean): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        root.addView(header(loggedIn), blockParams(bottom = 18))
        statusText = statusBanner()
        root.addView(statusText, blockParams(bottom = 14))
        progressPanel = createProgressPanel()
        root.addView(progressPanel, blockParams(bottom = 14))

        if (loggedIn) addSettingsScreen(root) else addLoginScreen(root)

        return ScrollView(this).apply {
            setBackgroundColor(Palette.Page)
            isFillViewport = true
            addView(root)
        }
    }

    private fun header(loggedIn: Boolean): LinearLayout {
        val titleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBlock.addView(TextView(this).apply {
            text = "명함장 Caller"
            textSize = 26f
            setTextColor(Palette.Ink)
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        })
        titleBlock.addView(TextView(this).apply {
            text = if (loggedIn) "기능 설정" else "계정 로그인"
            textSize = 14f
            setTextColor(Palette.Muted)
            setPadding(0, dp(7), 0, 0)
        })

        loginBadgeText = badge(if (loggedIn) "연결됨" else "로그인 필요", loggedIn)

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(titleBlock)
            addView(loginBadgeText)
        }
    }

    private fun addLoginScreen(root: LinearLayout) {
        val loginCard = card()
        loginCard.addView(sectionTitle("로그인"), blockParams(bottom = 12))
        loginCard.addView(fieldLabel("이메일"))
        emailInput = inputField("user@example.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }
        loginCard.addView(emailInput, blockParams(bottom = 12))

        loginCard.addView(fieldLabel("비밀번호"))
        passwordInput = inputField("비밀번호", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).apply {
            transformationMethod = PasswordTransformationMethod.getInstance()
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginAndSync()
                    true
                } else {
                    false
                }
            }
        }
        loginCard.addView(passwordInput, blockParams(bottom = 16))
        loginCard.addView(actionButton("로그인하고 동기화", ButtonStyle.Primary) { loginAndSync() })
        root.addView(loginCard)

        val statusCard = card()
        statusCard.addView(sectionTitle("앱 상태"), blockParams(bottom = 10))
        cardCountText = valueText("")
        statusCard.addView(infoRow("로컬 명함", cardCountText))
        root.addView(statusCard)
    }

    private fun addSettingsScreen(root: LinearLayout) {
        val syncCard = card()
        syncCard.addView(sectionTitle("동기화"), blockParams(bottom = 10))
        cardCountText = valueText("")
        lastSyncText = valueText("")
        syncCard.addView(infoRow("로컬 명함", cardCountText))
        syncCard.addView(infoRow("마지막 동기화", lastSyncText))
        syncCard.addView(actionButton("지금 동기화", ButtonStyle.Primary) { syncNow() }, blockParams(top = 8, bottom = 8))
        syncCard.addView(actionButton("이미지 다시 동기화", ButtonStyle.Secondary) { repairImages() }, blockParams(bottom = 8))
        syncCard.addView(horizontal(
            actionButton("로그아웃", ButtonStyle.Secondary) { logout() },
            actionButton("웹 명함장", ButtonStyle.Secondary) { openWebApp() },
        ))
        root.addView(syncCard)

        val displayCard = card()
        displayCard.addView(sectionTitle("표시 방식"), blockParams(bottom = 10))
        displayCard.addView(infoRow("현재 방식", valueText("알림")))
        displayCard.addView(supportText("전화 수신 시 알림 영역에 명함 이미지와 최근 기록을 표시합니다."))
        root.addView(displayCard)

        val permissionCard = card()
        permissionCard.addView(sectionTitle("권한"), blockParams(bottom = 10))
        callScreeningStateText = valueText("")
        permissionCard.addView(infoRow("발신자 표시 기본 앱", callScreeningStateText))
        permissionCard.addView(supportText("후스콜 등 기존 스팸 표시 앱과 동시에 기본 앱으로 사용할 수 없습니다. 기존 앱을 유지하려면 이 설정은 건너뛰세요."), blockParams(top = 4, bottom = 10))
        permissionCard.addView(actionButton("기본 앱으로 설정", ButtonStyle.Secondary) { requestCallScreeningRole() }, blockParams(bottom = 8))
        contactsPermissionStateText = valueText("")
        permissionCard.addView(infoRow("연락처 접근", contactsPermissionStateText))
        permissionCard.addView(supportText("휴대폰 연락처에 저장된 번호도 수신 감지하려면 필요합니다."), blockParams(top = 4, bottom = 8))
        if (!hasContactPermission()) {
            permissionCard.addView(actionButton("연락처 권한 허용", ButtonStyle.Secondary) { requestContactPermission() }, blockParams(bottom = 8))
        }
        phoneStatePermissionStateText = valueText("")
        permissionCard.addView(infoRow("전화 상태 감지", phoneStatePermissionStateText))
        permissionCard.addView(supportText("기본 앱 콜백이 제한되는 기기에서 수신 전화를 fallback으로 감지할 때 필요합니다."), blockParams(top = 4, bottom = 8))
        if (!hasPhoneStatePermission()) {
            permissionCard.addView(actionButton("전화 상태 권한 허용", ButtonStyle.Secondary) { requestPhoneStatePermissions() }, blockParams(bottom = 8))
        }
        callLogPermissionStateText = valueText("")
        smsPermissionStateText = valueText("")
        permissionCard.addView(infoRow("통화 기록", callLogPermissionStateText))
        permissionCard.addView(infoRow("문자 기록", smsPermissionStateText))
        permissionCard.addView(supportText("최근 통화 및 문자 기록과 최근 한 달 건수를 알림에 함께 표시할 때 필요합니다."), blockParams(top = 4, bottom = 8))
        if (!hasCallLogPermission() || !hasSmsPermission()) {
            permissionCard.addView(actionButton("기록 권한 허용", ButtonStyle.Secondary) { requestHistoryPermissions() }, blockParams(bottom = 8))
        }
        root.addView(permissionCard)

        val testCard = card()
        testCard.addView(sectionTitle("표시 테스트"), blockParams(bottom = 10))
        testCard.addView(actionButton("표시 테스트", ButtonStyle.Primary) { testDisplay() })
        root.addView(testCard)
    }

    private fun loginAndSync() {
        if (!::emailInput.isInitialized || !::passwordInput.isInitialized) return
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        if (email.isBlank() || password.isBlank()) {
            showStatus("이메일과 비밀번호를 입력해주세요.")
            return
        }
        runBackground(
            progress = "로그인 중입니다.",
            successMessage = { syncResult -> "로그인되었습니다. 변경 ${syncResult.changedCards}건, 이미지 ${syncResult.downloadedImages}건을 동기화했습니다." },
        ) { onProgress -> syncManager.loginAndSync(email, password, onProgress) }
    }

    private fun syncNow() {
        if (!tokenStore.isLoggedIn()) {
            showStatus("로그인이 필요합니다.")
            renderUi(force = true)
            return
        }
        runBackground(
            progress = "명함을 동기화 중입니다.",
            successMessage = { syncResult -> "동기화 완료: 변경 ${syncResult.changedCards}건, 이미지 ${syncResult.downloadedImages}건" },
        ) { onProgress -> syncManager.syncNow(onProgress) }
    }

    private fun repairImages() {
        if (!tokenStore.isLoggedIn()) {
            showStatus("로그인이 필요합니다.")
            renderUi(force = true)
            return
        }
        runBackground(
            progress = "명함 이미지를 다시 동기화 중입니다.",
            successMessage = { syncResult -> "이미지 동기화 완료: 명함 ${syncResult.changedCards}건, 이미지 ${syncResult.downloadedImages}건" },
        ) { onProgress -> syncManager.repairImagesNow(onProgress) }
    }

    private fun logout() {
        setBusy(true, "로그아웃 중입니다.")
        Thread {
            runCatching { syncManager.logout() }
            runOnUiThread {
                isBusy = false
                currentMessage = "로그아웃했습니다."
                renderUi(force = true)
            }
        }.start()
    }

    private fun runBackground(progress: String, successMessage: (SyncResult) -> String, block: ((SyncProgress) -> Unit) -> SyncResult) {
        setBusy(true, progress)
        updateProgress(SyncProgress(progress))
        Thread {
            val result = runCatching {
                block { syncProgress ->
                    runOnUiThread { updateProgress(syncProgress) }
                }
            }
            runOnUiThread {
                isBusy = false
                hideProgress()
                result.onSuccess { syncResult ->
                    currentMessage = successMessage(syncResult)
                    renderUi(force = true)
                }.onFailure { error ->
                    applyBusyState()
                    showStatus(error.message ?: "요청에 실패했습니다.")
                }
            }
        }.start()
    }

    private fun requestCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            openDefaultAppsSettings("이 기기에서 자동 권한 요청을 열 수 없어 기본 앱 설정을 열었습니다.")
            return
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            openDefaultAppsSettings("현재 기본 앱 설정을 열었습니다.")
            return
        }
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        if (intent.resolveActivity(packageManager) == null) {
            openDefaultAppsSettings("발신자 표시 권한 화면을 찾을 수 없어 기본 앱 설정을 열었습니다.")
            return
        }

        showStatus("발신자 표시 권한 화면을 여는 중입니다.")
        try {
            startActivityForResult(intent, REQUEST_CALL_SCREENING_ROLE)
        } catch (_: ActivityNotFoundException) {
            openDefaultAppsSettings("발신자 표시 권한 화면을 열 수 없어 기본 앱 설정을 열었습니다.")
        } catch (_: SecurityException) {
            openDefaultAppsSettings("발신자 표시 권한 요청이 제한되어 기본 앱 설정을 열었습니다.")
        }
    }

    private fun openDefaultAppsSettings(message: String) {
        showStatus(message)
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: SecurityException) {
            showStatus("기본 앱 설정을 열 수 없습니다.")
        }
    }

    private fun openWebApp() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.WEB_BASE_URL)))
    }

    private fun testDisplay() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            showStatus("알림 권한을 먼저 허용해주세요.")
            requestNotificationPermission()
            return
        }

        val firstCard = db.firstCard()
        if (firstCard != null && firstCard.imagePath.isNullOrBlank() && tokenStore.isLoggedIn()) {
            repairImagesThenShowTest()
            return
        }

        showTestNotification("테스트 알림을 표시했습니다.")
    }

    private fun repairImagesThenShowTest() {
        setBusy(true, "명함 이미지를 다시 동기화 중입니다.")
        updateProgress(SyncProgress("명함 이미지를 다시 동기화 중입니다."))
        Thread {
            val result = runCatching {
                syncManager.repairImagesNow { syncProgress ->
                    runOnUiThread { updateProgress(syncProgress) }
                }
            }
            runOnUiThread {
                isBusy = false
                hideProgress()
                result.onSuccess {
                    renderUi(force = true)
                    showTestNotification("이미지 동기화 후 테스트 알림을 표시했습니다.")
                }.onFailure { error ->
                    applyBusyState()
                    showStatus(error.message ?: "이미지 동기화에 실패했습니다.")
                }
            }
        }.start()
    }

    private fun showTestNotification(message: String) {
        val card = db.firstCard() ?: sampleCard()
        val history = ContactHistoryReader.summary(this, PhoneNumberNormalizer.aliases(card.displayPhone))
        BusinessCardNotification.show(this, card, history)
        showStatus(message)
    }

    private fun sampleCard(): CachedBusinessCard {
        return CachedBusinessCard(
            id = "sample-notification",
            name = "홍길동",
            company = "명함장",
            department = "모바일",
            position = "담당자",
            email = "hello@bus.sub.nanoha.kr",
            mobile = "010-1234-5678",
            phone = "02-123-4567",
            address = "Seoul, Korea",
            website = "bus.sub.nanoha.kr",
            tags = "",
            memoPreview = "",
            overlayImageKind = "generated",
            imagePath = null,
        )
    }

    private fun updateSummary() {
        val loggedIn = tokenStore.isLoggedIn()
        if (::loginBadgeText.isInitialized) {
            loginBadgeText.text = if (loggedIn) "연결됨" else "로그인 필요"
            loginBadgeText.background = badgeBackground(loggedIn)
            loginBadgeText.setTextColor(if (loggedIn) Palette.SuccessText else Palette.WarningText)
        }
        if (::cardCountText.isInitialized) cardCountText.text = "${db.activeCardCount()}건"
        if (::lastSyncText.isInitialized) lastSyncText.text = tokenStore.lastSyncAt().ifBlank { "없음" }
        if (::callScreeningStateText.isInitialized) callScreeningStateText.text = callScreeningStatusLabel()
        if (::contactsPermissionStateText.isInitialized) contactsPermissionStateText.text = if (hasContactPermission()) "허용됨" else "미허용"
        if (::phoneStatePermissionStateText.isInitialized) phoneStatePermissionStateText.text = if (hasPhoneStatePermission()) "허용됨" else "미허용"
        if (::callLogPermissionStateText.isInitialized) callLogPermissionStateText.text = if (hasCallLogPermission()) "허용됨" else "미허용"
        if (::smsPermissionStateText.isInitialized) smsPermissionStateText.text = if (hasSmsPermission()) "허용됨" else "미허용"
    }

    private fun callScreeningStatusLabel(): String {
        val roleManager = getSystemService(RoleManager::class.java)
        return when {
            !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) -> "사용 불가"
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) -> "활성화"
            else -> "미설정"
        }
    }

    private fun isCallScreeningRoleHeld(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestRequiredRuntimePermissions() {
        val permissions = buildList {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
            if (!hasContactPermission()) add(Manifest.permission.READ_CONTACTS)
            if (!hasPhoneStatePermission()) {
                add(Manifest.permission.READ_PHONE_STATE)
                add(Manifest.permission.READ_PHONE_NUMBERS)
            }
            if (!hasCallLogPermission()) add(Manifest.permission.READ_CALL_LOG)
            if (!hasSmsPermission()) add(Manifest.permission.READ_SMS)
        }
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), REQUEST_RUNTIME_PERMISSIONS)
    }

    private fun requestNotificationPermission() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
        }
    }

    private fun requestContactPermission() {
        if (!hasContactPermission()) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CONTACTS_PERMISSION)
        }
    }

    private fun requestPhoneStatePermissions() {
        val permissions = buildList {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_PHONE_STATE)
            if (checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), REQUEST_PHONE_STATE_PERMISSIONS)
    }

    private fun requestHistoryPermissions() {
        val permissions = buildList {
            if (!hasCallLogPermission()) add(Manifest.permission.READ_CALL_LOG)
            if (!hasSmsPermission()) add(Manifest.permission.READ_SMS)
        }
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), REQUEST_HISTORY_PERMISSIONS)
    }

    private fun hasContactPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPhoneStatePermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCallLogPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasSmsPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RUNTIME_PERMISSIONS || requestCode == REQUEST_NOTIFICATION_PERMISSION || requestCode == REQUEST_CONTACTS_PERMISSION || requestCode == REQUEST_PHONE_STATE_PERMISSIONS || requestCode == REQUEST_HISTORY_PERMISSIONS) {
            renderUi(force = true)
        }
    }

    private fun setBusy(value: Boolean, message: String) {
        isBusy = value
        applyBusyState()
        showStatus(message)
    }

    private fun applyBusyState() {
        actionButtons.forEach { button ->
            button.isEnabled = !isBusy
            button.alpha = if (isBusy) 0.48f else 1f
        }
    }

    private fun updateProgress(progress: SyncProgress) {
        if (!::progressPanel.isInitialized || !::progressText.isInitialized || !::progressBar.isInitialized) return
        progressPanel.visibility = View.VISIBLE
        progressText.text = if (progress.isIndeterminate) {
            progress.message
        } else {
            "${progress.message} ${progress.current.coerceIn(0, progress.total)}/${progress.total} (${progress.percent}%)"
        }
        progressBar.isIndeterminate = progress.isIndeterminate
        if (!progress.isIndeterminate) progressBar.progress = progress.percent
    }

    private fun hideProgress() {
        if (!::progressPanel.isInitialized || !::progressBar.isInitialized) return
        progressBar.isIndeterminate = false
        progressBar.progress = 0
        progressPanel.visibility = View.GONE
    }

    private fun showStatus(message: String) {
        currentMessage = message
        if (!::statusText.isInitialized) return
        statusText.text = message
        val tone = statusTone(message)
        statusText.setTextColor(tone.text)
        statusText.background = rounded(tone.background, 8, tone.border, 1)
    }

    private fun defaultStatus(loggedIn: Boolean): String {
        return if (loggedIn) "로그인되었습니다." else "로그인이 필요합니다."
    }

    private fun statusTone(message: String): Tone {
        return when {
            message.contains("실패") || message.contains("사용할 수 없습니다") -> Tone(Palette.ErrorBg, Palette.ErrorText, Palette.ErrorLine)
            message.contains("필요") || message.contains("입력") || message.contains("미허용") || message.contains("선택되지") -> Tone(Palette.WarningBg, Palette.WarningText, Palette.WarningLine)
            message.contains("완료") || message.contains("되었습니다") || message.contains("저장") || message.contains("활성화") || message.contains("표시했습니다") -> Tone(Palette.SuccessBg, Palette.SuccessText, Palette.SuccessLine)
            else -> Tone(Palette.InfoBg, Palette.InfoText, Palette.InfoLine)
        }
    }

    private fun statusBanner(): TextView {
        return TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(44)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            includeFontPadding = false
        }
    }

    private fun createProgressPanel(): LinearLayout {
        progressText = TextView(this).apply {
            textSize = 12f
            setTextColor(Palette.Muted)
            includeFontPadding = false
        }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            isIndeterminate = true
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Palette.Surface, 8, Palette.Line, 1)
            addView(progressText, blockParams(bottom = 8))
            addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)))
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(Palette.Surface, 8, Palette.Line, 1)
            elevation = dp(1).toFloat()
            layoutParams = blockParams(bottom = 14)
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 17f
            setTextColor(Palette.Ink)
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
    }

    private fun fieldLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Palette.Muted)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(6))
            includeFontPadding = false
        }
    }

    private fun supportText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Palette.Muted)
            setLineSpacing(dp(2).toFloat(), 1.0f)
            includeFontPadding = false
        }
    }

    private fun inputField(hint: String, inputType: Int): EditText {
        return EditText(this).apply {
            this.hint = hint
            this.inputType = inputType
            setSingleLine(true)
            textSize = 15f
            setTextColor(Palette.Ink)
            setHintTextColor(Palette.Faint)
            background = rounded(Palette.Input, 8, Palette.Line, 1)
            minHeight = dp(52)
            setPadding(dp(14), 0, dp(14), 0)
        }
    }

    private fun actionButton(label: String, style: ButtonStyle, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minHeight = dp(48)
            setPadding(dp(12), 0, dp(12), 0)
            setTextColor(buttonTextColor(style))
            background = buttonBackground(style)
            isClickable = true
            isFocusable = true
            setOnClickListener { if (!isBusy) action() }
            actionButtons.add(this)
            applyBusyState()
        }
    }

    private fun horizontal(vararg views: View): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            views.forEachIndexed { index, view ->
                addView(view, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    if (index > 0) leftMargin = dp(8)
                })
            }
        }
    }

    private fun infoRow(label: String, value: TextView): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 14f
                setTextColor(Palette.Muted)
                includeFontPadding = false
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(value)
        }
    }

    private fun valueText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Palette.Ink)
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
    }

    private fun badge(text: String, loggedIn: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (loggedIn) Palette.SuccessText else Palette.WarningText)
            background = badgeBackground(loggedIn)
            minHeight = dp(32)
            setPadding(dp(12), 0, dp(12), 0)
        }
    }

    private fun badgeBackground(loggedIn: Boolean): GradientDrawable {
        return if (loggedIn) rounded(Palette.SuccessBg, 8, Palette.SuccessLine, 1) else rounded(Palette.WarningBg, 8, Palette.WarningLine, 1)
    }

    private fun buttonBackground(style: ButtonStyle): GradientDrawable {
        return when (style) {
            ButtonStyle.Primary -> rounded(Palette.Primary, 8)
            ButtonStyle.Secondary -> rounded(Palette.Surface, 8, Palette.Line, 1)
        }
    }

    private fun buttonTextColor(style: ButtonStyle): Int {
        return when (style) {
            ButtonStyle.Primary -> Color.WHITE
            ButtonStyle.Secondary -> Palette.Ink
        }
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null && strokeWidth > 0) setStroke(dp(strokeWidth), strokeColor)
        }
    }

    private fun blockParams(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            if (top > 0) topMargin = dp(top)
            if (bottom > 0) bottomMargin = dp(bottom)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class ButtonStyle { Primary, Secondary }

    private data class Tone(val background: Int, val text: Int, val border: Int)

    private object Palette {
        val Page: Int = Color.rgb(245, 247, 242)
        val Surface: Int = Color.rgb(255, 255, 255)
        val Input: Int = Color.rgb(250, 252, 248)
        val Ink: Int = Color.rgb(23, 32, 29)
        val Muted: Int = Color.rgb(91, 105, 99)
        val Faint: Int = Color.rgb(143, 154, 148)
        val Line: Int = Color.rgb(217, 226, 218)
        val Primary: Int = Color.rgb(23, 32, 29)
        val InfoBg: Int = Color.rgb(237, 244, 247)
        val InfoText: Int = Color.rgb(32, 84, 107)
        val InfoLine: Int = Color.rgb(194, 219, 230)
        val SuccessBg: Int = Color.rgb(232, 245, 236)
        val SuccessText: Int = Color.rgb(33, 105, 68)
        val SuccessLine: Int = Color.rgb(185, 222, 198)
        val WarningBg: Int = Color.rgb(255, 248, 226)
        val WarningText: Int = Color.rgb(128, 89, 24)
        val WarningLine: Int = Color.rgb(232, 211, 143)
        val ErrorBg: Int = Color.rgb(255, 238, 238)
        val ErrorText: Int = Color.rgb(151, 52, 52)
        val ErrorLine: Int = Color.rgb(238, 188, 188)
    }

    companion object {
        private const val REQUEST_RUNTIME_PERMISSIONS = 300
        private const val REQUEST_NOTIFICATION_PERMISSION = 301
        private const val REQUEST_CALL_SCREENING_ROLE = 302
        private const val REQUEST_CONTACTS_PERMISSION = 303
        private const val REQUEST_HISTORY_PERMISSIONS = 304
        private const val REQUEST_PHONE_STATE_PERMISSIONS = 305
    }
}
