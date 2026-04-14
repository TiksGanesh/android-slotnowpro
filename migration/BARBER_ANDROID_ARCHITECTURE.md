# Barber Android App — Native Kotlin Architecture

> Target: Android API 26+ (Android 8.0 Oreo)  
> Language: Kotlin  
> UI Toolkit: Jetpack Compose + Material Design 3  
> Date: 2026-04-13

---

## 1. Architecture Pattern: MVVM + Clean Architecture (Layered)

```
┌──────────────────────────────────────────┐
│              Presentation Layer           │
│  Composables → ViewModels → UiState       │
├──────────────────────────────────────────┤
│              Domain Layer                 │
│  UseCases (optional) · Domain Models      │
├──────────────────────────────────────────┤
│               Data Layer                  │
│  Repositories → Remote DataSource        │
│               → Local DataSource (Cache) │
├──────────────────────────────────────────┤
│            Infrastructure                │
│  Retrofit · OkHttp · Room · Hilt         │
└──────────────────────────────────────────┘
```

**Why this stack?**
- **MVVM** is Google's recommended pattern for Compose; `ViewModel` survives configuration changes
- **Clean Architecture** decouples UI from network; repositories are the single source of truth
- **Hilt** reduces boilerplate vs Koin while being officially supported
- **Compose** replaces XML layouts with a declarative, reactive UI model matching the web's React paradigm

---

## 2. Package Structure

```
com.clipper.barber/
├── di/                         # Hilt dependency injection modules
│   ├── NetworkModule.kt
│   ├── AuthModule.kt
│   └── RepositoryModule.kt
│
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── OnboardingApi.kt       # GET shop/{slug}/validate (no auth)
│   │   │   ├── BarberAuthApi.kt       # Retrofit interface for auth endpoints
│   │   │   └── BarberDashboardApi.kt  # Retrofit interface for bookings/workflow
│   │   ├── dto/                       # JSON response models (Data Transfer Objects)
│   │   │   ├── onboarding/
│   │   │   │   └── ShopValidateResponse.kt
│   │   │   ├── auth/
│   │   │   │   ├── RequestOtpResponse.kt
│   │   │   │   ├── VerifyOtpResponse.kt
│   │   │   │   └── BarberProfileDto.kt
│   │   │   └── dashboard/
│   │   │       ├── BookingDto.kt
│   │   │       ├── BookingsListResponse.kt
│   │   │       └── WorkflowResponse.kt
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt     # Adds Authorization + Accept-Language headers
│   │       └── TokenAuthenticator.kt # Handles 401 → refresh token
│   │
│   ├── local/
│   │   ├── prefs/
│   │   │   ├── TokenManager.kt        # EncryptedSharedPreferences: token + expiry
│   │   │   ├── ShopManager.kt         # EncryptedSharedPreferences: shopSlug/name/timezone/logoUrl
│   │   │   └── LanguageManager.kt     # Regular SharedPreferences: language code (en/hi/mr)
│   │   └── db/                        # Room DB (optional — offline cache Phase 5)
│   │       ├── AppDatabase.kt
│   │       └── BookingDao.kt
│   │
│   └── repository/
│       ├── OnboardingRepositoryImpl.kt
│       ├── AuthRepositoryImpl.kt
│       ├── BookingsRepositoryImpl.kt
│       └── WorkflowRepositoryImpl.kt
│
├── domain/
│   ├── model/                         # Domain models (independent of network/DB)
│   │   ├── Barber.kt
│   │   ├── Booking.kt
│   │   ├── BookingStatus.kt
│   │   ├── PaymentStatus.kt
│   │   └── WorkflowAction.kt
│   │
│   ├── repository/                    # Repository interfaces
│   │   ├── AuthRepository.kt
│   │   ├── BookingsRepository.kt
│   │   └── WorkflowRepository.kt
│   │
│   └── usecase/                       # Optional — encapsulate complex business logic
│       ├── GetDashboardBookingsUseCase.kt
│       └── ExecuteWorkflowActionUseCase.kt
│
├── presentation/
│   ├── navigation/
│   │   ├── AppNavGraph.kt             # NavHost + all routes
│   │   └── Screen.kt                  # Sealed class of route strings
│   │
│   ├── onboarding/
│   │   ├── LanguageSelectionScreen.kt # Step 1: language picker (inline hardcoded labels)
│   │   ├── ShopSetupScreen.kt         # Step 2: slug entry + validation
│   │   └── OnboardingViewModel.kt     # validateShop() + setLanguage()
│   │
│   ├── auth/
│   │   ├── LoginScreen.kt             # Phone entry step (shows shop name from ShopManager)
│   │   ├── OtpScreen.kt               # OTP verification step
│   │   └── AuthViewModel.kt
│   │
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   ├── DashboardViewModel.kt
│   │   ├── components/
│   │   │   ├── AppointmentCard.kt
│   │   │   ├── DateNavigator.kt
│   │   │   ├── StatusFilterTabs.kt
│   │   │   └── StatusBadge.kt
│   │   └── BookingDetailBottomSheet.kt
│   │
│   ├── workflow/
│   │   └── WorkflowViewModel.kt       # Shared with dashboard (or merged into DashboardViewModel)
│   │
│   └── profile/
│       ├── ProfileScreen.kt
│       └── ProfileViewModel.kt
│
├── util/
│   ├── DateTimeUtils.kt               # Timezone-aware formatting utilities
│   ├── AmountUtils.kt                 # Paise → ₹ formatting
│   ├── PhoneUtils.kt                  # E.164 validation mirror of web lib
│   └── ApiResult.kt                  # Sealed result wrapper
│
└── res/
    ├── values/
    │   └── strings.xml                # English (canonical — all keys defined here)
    ├── values-hi/
    │   └── strings.xml                # Hindi (complete — no English fallback allowed)
    └── values-mr/
        └── strings.xml                # Marathi (complete — no English fallback allowed)
│
└── MainApplication.kt                 # @HiltAndroidApp
```

---

## 3. Key Dependencies (build.gradle)

```kotlin
// Jetpack Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.9.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.1")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")

// Hilt
implementation("com.google.dagger:hilt-android:2.51.1")
kapt("com.google.dagger:hilt-compiler:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Retrofit + OkHttp
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// AppCompat (required for setApplicationLocales back to API 21)
implementation("androidx.appcompat:appcompat:1.7.0")

// Encrypted Storage
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Room (Phase 5 - offline cache)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// Firebase
implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
implementation("com.google.firebase:firebase-messaging-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")
implementation("com.google.firebase:firebase-crashlytics-ktx")

// Testing
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.12")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

---

## 4. Networking Layer

### 4.1 Retrofit API Interfaces

```kotlin
// OnboardingApi.kt  — no auth interceptor, uses plain OkHttpClient
interface OnboardingApi {
    @GET("shop/{slug}/validate")
    suspend fun validateShop(@Path("slug") slug: String): ApiResponse<ShopValidateData>
}

data class ShopValidateData(
    val shopSlug: String,
    val shopName: String,
    val shopTimezone: String,
    val logoUrl: String?
)
```

```kotlin
// BarberAuthApi.kt
interface BarberAuthApi {
    @POST("auth/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): ApiResponse<RequestOtpData>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): ApiResponse<VerifyOtpData>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<RefreshTokenData>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @GET("auth/me")
    suspend fun getMe(): ApiResponse<MeData>

    @POST("auth/update-profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<BarberProfileData>
}

// BarberDashboardApi.kt
interface BarberDashboardApi {
    @GET("dashboard/bookings")
    suspend fun getBookings(@Query("date") date: String): ApiResponse<BookingsListData>

    @POST("dashboard/bookings/{bookingId}/workflow")
    suspend fun executeWorkflow(
        @Path("bookingId") bookingId: String,
        @Body request: WorkflowRequest
    ): ApiResponse<WorkflowData>
}
```

### 4.2 Auth Interceptor

```kotlin
// AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val languageManager: LanguageManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken() ?: return chain.proceed(chain.request())
        val language = languageManager.get() ?: "en"
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept-Language", language)
            .addHeader("X-Platform", "android")
            .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
            .build()
        return chain.proceed(request)
    }
}
```

### 4.3 Token Refresh Authenticator

```kotlin
// TokenAuthenticator.kt
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: BarberAuthApi    // direct reference, no interceptor
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only retry once
        if (response.request.header("X-Retry-After-Refresh") != null) return null

        return runBlocking {
            mutex.withLock {
                val currentToken = tokenManager.getToken() ?: return@withLock null

                // If token was already refreshed by concurrent request, use new token
                val requestToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")
                if (currentToken != requestToken) {
                    // Already refreshed — retry with current token
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .header("X-Retry-After-Refresh", "true")
                        .build()
                }

                // Attempt refresh
                val result = try {
                    authApi.refreshToken(RefreshTokenRequest(currentToken))
                } catch (e: Exception) {
                    null
                }

                if (result?.success == true && result.data != null) {
                    tokenManager.saveToken(result.data.token, result.data.expiresAt)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${result.data.token}")
                        .header("X-Retry-After-Refresh", "true")
                        .build()
                } else {
                    tokenManager.clearToken()
                    // Signal session expired to ViewModel via shared flow
                    SessionEventBus.emit(SessionEvent.Expired)
                    null
                }
            }
        }
    }
}
```

---

## 5. Token Manager

```kotlin
// TokenManager.kt
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "barber_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String, expiresAt: Instant) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRES_AT, expiresAt.toEpochMilli())
            .apply()
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (Instant.now().toEpochMilli() >= expiresAt) {
            clearToken()
            return null
        }
        return token
    }

    fun saveBarberInfo(barber: Barber) { /* JSON serialize to prefs */ }
    fun getBarberInfo(): Barber? { /* deserialize */ }

    fun clearToken() {
        // Clears token only — does NOT clear shop slug or language
        prefs.edit().remove(KEY_TOKEN).remove(KEY_EXPIRES_AT).apply()
    }

    companion object {
        private const val KEY_TOKEN = "barber_token"
        private const val KEY_EXPIRES_AT = "token_expires_at"
    }
}
```

---

## 6. ViewModel Pattern

```kotlin
// DashboardViewModel.kt
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val bookingsRepository: BookingsRepository,
    private val workflowRepository: WorkflowRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedTab = MutableStateFlow(BookingTab.UPCOMING)
    val selectedTab: StateFlow<BookingTab> = _selectedTab.asStateFlow()

    val filteredBookings: StateFlow<List<Booking>> = combine(
        uiState.map { it.allBookings },
        selectedTab
    ) { bookings, tab ->
        when (tab) {
            BookingTab.UPCOMING -> bookings.filter { it.isUpcoming }
            BookingTab.CANCELLED -> bookings.filter { it.status == BookingStatus.CANCELED }
            BookingTab.NO_SHOW -> bookings.filter { it.status == BookingStatus.NO_SHOW }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            selectedDate.collect { date ->
                loadBookings(date)
            }
        }
    }

    fun navigateDate(delta: Int) {
        _selectedDate.update { it.plusDays(delta.toLong()) }
    }

    fun goToToday() { _selectedDate.value = LocalDate.now() }

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun selectTab(tab: BookingTab) { _selectedTab.value = tab }

    fun refresh() { loadBookings(_selectedDate.value) }

    private fun loadBookings(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = bookingsRepository.getBookings(date)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, allBookings = result.data.bookings)
                }
                is ApiResult.ApiError -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, error = "No internet connection")
                }
            }
        }
    }

    fun executeWorkflowAction(bookingId: String, action: WorkflowAction) {
        viewModelScope.launch {
            _uiState.update { it.copy(workflowLoading = bookingId) }
            when (val result = workflowRepository.executeAction(bookingId, action)) {
                is ApiResult.Success -> {
                    // Update the specific booking in the list
                    _uiState.update { state ->
                        state.copy(
                            workflowLoading = null,
                            allBookings = state.allBookings.map { booking ->
                                if (booking.id == bookingId) booking.applyWorkflowResult(result.data)
                                else booking
                            }
                        )
                    }
                }
                is ApiResult.ApiError -> _uiState.update {
                    it.copy(workflowLoading = null, workflowError = result.message)
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(workflowLoading = null, workflowError = "Network error. Please retry.")
                }
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val allBookings: List<Booking> = emptyList(),
    val error: String? = null,
    val workflowLoading: String? = null,  // bookingId of in-flight workflow
    val workflowError: String? = null
)

enum class BookingTab { UPCOMING, CANCELLED, NO_SHOW }
```

---

## 7. Navigation

### App Launch Decision Tree

```
App launch
  │
  ├─ languageManager.get() == null?  → startDestination = "onboarding_language"
  ├─ shopManager.getSlug() == null?  → startDestination = "onboarding_shop"
  ├─ tokenManager.getToken() == null? → startDestination = "auth"
  └─ all stored + valid              → startDestination = "main"
```

### Navigation Graphs

```kotlin
// Screen.kt
sealed class Screen(val route: String) {
    // Onboarding
    object Language : Screen("onboarding_language")
    object ShopSetup : Screen("onboarding_shop")
    // Auth
    object Login : Screen("login")
    object Otp : Screen("otp/{maskedPhone}") {
        fun createRoute(maskedPhone: String) = "otp/$maskedPhone"
    }
    // Main
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")
    object LanguagePicker : Screen("language_picker")   // from profile
}

// AppNavGraph.kt
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // Onboarding graph — one-time, skipped on subsequent launches
        navigation(startDestination = Screen.Language.route, route = "onboarding") {
            composable(Screen.Language.route) {
                val vm: OnboardingViewModel = hiltViewModel()
                LanguageSelectionScreen(viewModel = vm,
                    onLanguageSelected = { code ->
                        vm.setLanguage(code)
                        // If slug already stored (edge case: only language was missing), skip shop step
                        val next = if (vm.hasShopSlug()) "auth" else Screen.ShopSetup.route
                        navController.navigate(next) { popUpTo(Screen.Language.route) { inclusive = true } }
                    }
                )
            }
            composable(Screen.ShopSetup.route) {
                val vm: OnboardingViewModel = hiltViewModel()
                ShopSetupScreen(viewModel = vm,
                    onShopValidated = {
                        navController.navigate("auth") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
        }

        // Auth graph
        navigation(startDestination = Screen.Login.route, route = "auth") {
            composable(Screen.Login.route) {
                val vm: AuthViewModel = hiltViewModel()
                LoginScreen(viewModel = vm, onOtpSent = { masked ->
                    navController.navigate(Screen.Otp.createRoute(masked))
                })
            }
            composable(
                Screen.Otp.route,
                arguments = listOf(navArgument("maskedPhone") { type = NavType.StringType })
            ) { entry ->
                val maskedPhone = entry.arguments?.getString("maskedPhone") ?: ""
                val vm: AuthViewModel = hiltViewModel()
                OtpScreen(viewModel = vm, maskedPhone = maskedPhone,
                    onVerified = {
                        navController.navigate("main") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
        }

        // Main graph
        navigation(startDestination = Screen.Dashboard.route, route = "main") {
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = hiltViewModel()
                DashboardScreen(viewModel = vm,
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Profile.route) {
                val vm: ProfileViewModel = hiltViewModel()
                ProfileScreen(viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onChangeLanguage = { navController.navigate(Screen.LanguagePicker.route) }
                )
            }
            composable(Screen.LanguagePicker.route) {
                val vm: OnboardingViewModel = hiltViewModel()
                LanguageSelectionScreen(viewModel = vm,
                    onLanguageSelected = { code ->
                        vm.setLanguage(code)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
```

> **Logout navigation**: clears the shop slug and navigates to `"onboarding"` (not `"auth"`), so the user goes back through shop onboarding. Language is never cleared.

---

## 8. Timezone Utilities

```kotlin
// DateTimeUtils.kt
object DateTimeUtils {

    fun formatTime(utcIso: String, timezone: String): String {
        val instant = Instant.parse(utcIso)
        val zoneId = ZoneId.of(timezone)
        val zonedDateTime = instant.atZone(zoneId)
        return DateTimeFormatter.ofPattern("hh:mm a").format(zonedDateTime)
    }

    fun formatTimeRange(startUtc: String, endUtc: String, timezone: String): String {
        return "${formatTime(startUtc, timezone)} – ${formatTime(endUtc, timezone)}"
    }

    fun formatDate(utcIso: String, timezone: String): String {
        val instant = Instant.parse(utcIso)
        val zoneId = ZoneId.of(timezone)
        return DateTimeFormatter.ofPattern("EEE, dd MMM yyyy").format(instant.atZone(zoneId))
    }

    fun formatLocalDate(date: LocalDate): String {
        return DateTimeFormatter.ofPattern("EEE, dd MMM yyyy").format(date)
    }

    fun isCurrentAppointment(startUtc: String, endUtc: String, timezone: String): Boolean {
        val now = ZonedDateTime.now(ZoneId.of(timezone)).toInstant()
        val start = Instant.parse(startUtc)
        val end = Instant.parse(endUtc)
        return now >= start && now < end
    }

    fun isMissedAppointment(startUtc: String, status: BookingStatus, timezone: String): Boolean {
        if (status != BookingStatus.CONFIRMED && status != BookingStatus.PENDING_PAYMENT) return false
        val now = ZonedDateTime.now(ZoneId.of(timezone)).toInstant()
        val start = Instant.parse(startUtc)
        return now > start
    }

    fun localDateToIso(date: LocalDate): String = date.toString() // YYYY-MM-DD
}
```

---

## 9. Amount Utilities

```kotlin
// AmountUtils.kt
object AmountUtils {
    fun formatAmount(paise: Int): String {
        val rupees = paise / 100.0
        return "₹%.2f".format(rupees)
    }

    fun isDue(paymentStatus: PaymentStatus): Boolean =
        paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.PARTIAL

    fun dueAmount(totalPaise: Int, paidPaise: Int): Int = totalPaise - paidPaise
}
```

---

## 10. UI Component Examples

### Status Badge
```kotlin
@Composable
fun StatusBadge(status: BookingStatus) {
    val (label, containerColor, contentColor) = when (status) {
        BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED ->
            Triple("Upcoming", Blue100, Blue700)
        BookingStatus.SEATED ->
            Triple("In Chair", Amber100, Amber700)
        BookingStatus.COMPLETED ->
            Triple("Completed", Green100, Green700)
        BookingStatus.CANCELED ->
            Triple("Cancelled", Rose100, Rose700)
        BookingStatus.NO_SHOW ->
            Triple("No Show", Slate100, Slate700)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
```

### Appointment Card
```kotlin
@Composable
fun AppointmentCard(
    booking: Booking,
    shopTimezone: String,
    onClick: () -> Unit
) {
    val isCurrent = DateTimeUtils.isCurrentAppointment(
        booking.startTime, booking.endTime, shopTimezone
    )
    val isMissed = DateTimeUtils.isMissedAppointment(
        booking.startTime, booking.status, shopTimezone
    )

    val borderColor = when {
        isCurrent -> Color(0xFF10B981)  // emerald
        isMissed  -> Color(0xFFEF4444)  // red
        else      -> Color(0xFFE5E7EB)  // gray
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissed) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = DateTimeUtils.formatTimeRange(
                        booking.startTime, booking.endTime, shopTimezone
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                StatusBadge(booking.status)
            }
            if (isCurrent) {
                Text("Current appointment",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF10B981))
            }
            if (isMissed) {
                Text("Missed start time",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF4444))
            }
            Spacer(Modifier.height(8.dp))
            Text(booking.customerName, style = MaterialTheme.typography.titleSmall)
            Text("${booking.serviceName} · ${booking.serviceDurationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(AmountUtils.formatAmount(booking.totalAmount),
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                if (AmountUtils.isDue(booking.paymentStatus)) {
                    Text("· ₹${AmountUtils.dueAmount(booking.totalAmount, booking.paidAmount) / 100.0} due",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF59E0B))
                }
            }
        }
    }
}
```

---

## 11. Session Event Bus

For cross-cutting session expiry handling (e.g., token revoked mid-use):

```kotlin
// SessionEventBus.kt
object SessionEventBus {
    private val _events = MutableSharedFlow<SessionEvent>()
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    suspend fun emit(event: SessionEvent) = _events.emit(event)
}

sealed class SessionEvent {
    object Expired : SessionEvent()
    object Revoked : SessionEvent()
    data class BarberInactive(val message: String) : SessionEvent()
}

// Observe in MainActivity or NavGraph
LaunchedEffect(Unit) {
    SessionEventBus.events.collect { event ->
        when (event) {
            is SessionEvent.Expired, SessionEvent.Revoked -> {
                navController.navigate("auth") {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }
}
```

---

## 12. Hilt Modules

```kotlin
// NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideTokenManager(@ApplicationContext ctx: Context) = TokenManager(ctx)

    @Provides @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager) = AuthInterceptor(tokenManager)

    @Provides @Singleton
    @Named("auth_retrofit")  // No auth interceptor — for refresh calls
    fun provideAuthRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("${BuildConfig.BASE_URL}/api/v1/barber/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        @Named("auth_retrofit") authRetrofit: Retrofit
    ) = TokenAuthenticator(tokenManager, authRetrofit.create(BarberAuthApi::class.java))

    @Provides @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("${BuildConfig.BASE_URL}/api/v1/barber/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideBarberAuthApi(retrofit: Retrofit): BarberAuthApi =
        retrofit.create(BarberAuthApi::class.java)

    @Provides @Singleton
    fun provideBarberDashboardApi(retrofit: Retrofit): BarberDashboardApi =
        retrofit.create(BarberDashboardApi::class.java)

    // OnboardingApi uses a plain client (no auth interceptor)
    @Provides @Singleton
    fun provideOnboardingApi(@Named("auth_retrofit") authRetrofit: Retrofit): OnboardingApi =
        authRetrofit.create(OnboardingApi::class.java)

    @Provides @Singleton
    fun provideShopManager(@ApplicationContext ctx: Context): ShopManager = ShopManager(ctx)

    @Provides @Singleton
    fun provideLanguageManager(@ApplicationContext ctx: Context): LanguageManager = LanguageManager(ctx)
}
```

---

## 13. ProGuard Rules

```proguard
# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Gson DTOs
-keep class com.clipper.barber.data.remote.dto.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
```

---

## 14. Testing Strategy

### Unit Tests
- `OnboardingViewModelTest`: Shop slug validation states (valid, not found, inactive, network error)
- `LanguageManagerTest`: Save/retrieve/default language code
- `ShopManagerTest`: Save/retrieve/clear shop info
- `AuthViewModelTest`: OTP request/verify flow, error states, token storage
- `DashboardViewModelTest`: Date navigation, tab filtering, workflow action optimistic update
- `DateTimeUtilsTest`: Timezone formatting, current appointment detection
- `TokenManagerTest`: Save/retrieve/expiry of tokens — confirm `clearToken()` does not affect `ShopManager`
- `WorkflowRepositoryTest`: Mocked API responses, result mapping

### Integration Tests
- `OnboardingFlowTest`: Language selection → shop slug entry → successful validation → login screen
- `LoginFlowTest`: End-to-end Compose UI test for OTP login (MockWebServer)
- `DashboardFlowTest`: Bookings list render, pull-to-refresh, tab switching
- `LogoutFlowTest`: Confirm token cleared, shop slug cleared, language preserved, navigates to onboarding

### Test Doubles
- Use `MockWebServer` (OkHttp) for API tests — avoids network dependency
- Use Hilt `@TestInstallIn` to swap real modules for test doubles

---

## 15. Minimum Supported Version & Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35" />

<uses-permission android:name="android.permission.INTERNET" />
<!-- No CALL_PHONE permission needed — uses ACTION_DIAL -->
<!-- No RECEIVE_SMS permission needed — OTP via WhatsApp -->

<!-- FCM (Phase 5) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**minSdkVersion 26** (Android 8.0):
- `java.time` available natively (no desugaring needed above API 26)
- `EncryptedSharedPreferences` stable
- Covers 95%+ of active Android devices as of 2026

---

## 16. Onboarding Architecture

### OnboardingViewModel

```kotlin
// OnboardingViewModel.kt
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingApi: OnboardingApi,
    private val shopManager: ShopManager,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _shopState = MutableStateFlow<ShopValidationState>(ShopValidationState.Idle)
    val shopState: StateFlow<ShopValidationState> = _shopState.asStateFlow()

    fun hasShopSlug(): Boolean = shopManager.getSlug() != null

    fun validateShop(slug: String) {
        val cleanSlug = slug.trim().lowercase()
        viewModelScope.launch {
            _shopState.value = ShopValidationState.Loading
            _shopState.value = when (val result = onboardingApi.validateShop(cleanSlug)) {
                is ApiResult.Success -> {
                    shopManager.save(result.data)
                    ShopValidationState.Valid(result.data.shopName)
                }
                is ApiResult.ApiError -> when (result.code) {
                    "SHOP_NOT_FOUND" -> ShopValidationState.NotFound
                    "SHOP_INACTIVE"  -> ShopValidationState.Inactive
                    else             -> ShopValidationState.Error(result.message)
                }
                is ApiResult.NetworkError -> ShopValidationState.NetworkError
            }
        }
    }

    fun setLanguage(code: String) {
        languageManager.save(code)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(code)
        )
    }
}

sealed class ShopValidationState {
    object Idle : ShopValidationState()
    object Loading : ShopValidationState()
    data class Valid(val shopName: String) : ShopValidationState()
    object NotFound : ShopValidationState()
    object Inactive : ShopValidationState()
    object NetworkError : ShopValidationState()
    data class Error(val message: String) : ShopValidationState()
}
```

### Language Selection Screen — Special Localisation Rule

The `LanguageSelectionScreen` is the **only screen** in the app that uses inline hardcoded strings instead of `stringResource()`. This is intentional: it must render before any locale is applied. All three language names are embedded directly in the composable.

```kotlin
// LanguageSelectionScreen.kt  — inline labels only, NO stringResource() here
val languages = listOf(
    LanguageOption("en", "English",          "English"),
    LanguageOption("hi", "हिंदी",             "Hindi"),
    LanguageOption("mr", "मराठी",             "Marathi")
)
```

When this screen is reused from the Profile screen (language change flow), it operates the same way — the only difference is that the back button is visible (since it's not the first-launch context).

---

## 17. Localisation Architecture

### String Resource Structure

```
res/
├── values/
│   └── strings.xml          English — canonical. ALL keys defined here.
├── values-hi/
│   └── strings.xml          Hindi — complete. Zero fallback to English permitted.
└── values-mr/
    └── strings.xml          Marathi — complete. Zero fallback to English permitted.
```

### String Key Convention: `screen_element_description`

```xml
<!-- res/values/strings.xml (English) -->
<resources>
    <!-- Onboarding -->
    <string name="onboarding_language_title">Choose your language</string>
    <string name="onboarding_shop_title">Enter your Shop ID</string>
    <string name="onboarding_shop_hint">e.g. cuts-by-raj</string>
    <string name="onboarding_shop_continue">Continue</string>
    <string name="onboarding_shop_found">Found: %1$s</string>
    <string name="onboarding_shop_not_found">No shop found with this ID. Check with your manager.</string>
    <string name="onboarding_shop_inactive">This shop is currently inactive. Contact support.</string>

    <!-- Login -->
    <string name="login_phone_label">Phone Number</string>
    <string name="login_phone_hint">Enter your 10-digit number</string>
    <string name="login_send_otp">Send OTP</string>
    <string name="login_otp_sent">OTP sent to %1$s</string>
    <string name="login_shop_name_prefix">Logging into</string>

    <!-- Dashboard -->
    <string name="dashboard_tab_upcoming">Upcoming</string>
    <string name="dashboard_tab_cancelled">Cancelled</string>
    <string name="dashboard_tab_no_show">No Show</string>
    <string name="dashboard_today">Today</string>
    <string name="dashboard_empty_state">No appointments for this day</string>
    <string name="dashboard_current_appointment">Current appointment</string>
    <string name="dashboard_missed_start">Missed start time</string>

    <!-- Booking workflow -->
    <string name="workflow_start_service">Start Service</string>
    <string name="workflow_complete_service">Complete Service</string>
    <string name="workflow_collect">Collect %1$s</string>
    <string name="workflow_done">Done</string>
    <string name="workflow_mark_no_show">Mark No Show</string>
    <string name="workflow_cancel_refund">Cancel &amp; Refund</string>

    <!-- Profile -->
    <string name="profile_title">Profile</string>
    <string name="profile_shop_section_title">Shop</string>
    <string name="profile_language_label">Language</string>
    <string name="profile_save_changes">Save Changes</string>
    <string name="profile_discard">Discard Changes</string>

    <!-- Errors -->
    <string name="error_network">No internet connection. Please retry.</string>
    <string name="error_session_expired">Your session has expired. Please log in again.</string>
    <string name="error_barber_inactive">Your account has been deactivated. Contact your manager.</string>
</resources>
```

### LanguageManager

```kotlin
// LanguageManager.kt
class LanguageManager @Inject constructor(
    @ApplicationContext context: Context
) {
    // Regular SharedPreferences — language is not sensitive data
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun save(code: String) {
        prefs.edit().putString(KEY_LANG, code).apply()
    }

    fun get(): String? = prefs.getString(KEY_LANG, null)

    fun getOrDefault(): String = prefs.getString(KEY_LANG, null) ?: "en"

    // Language is NEVER cleared on logout — it is a device preference
    companion object {
        private const val KEY_LANG = "language"
    }
}
```

### Applying Language on App Startup

```kotlin
// MainActivity.kt  (or Application.onCreate)
class MainActivity : AppCompatActivity() {
    @Inject lateinit var languageManager: LanguageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply stored language BEFORE super.onCreate() so initial layout uses correct locale
        val lang = languageManager.getOrDefault()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        super.onCreate(savedInstanceState)
        // ...
    }
}
```

### Zero Hardcoded Strings Rule

Enforced via `lint.xml`:
```xml
<!-- lint.xml -->
<lint>
    <issue id="HardcodedText" severity="error" />
</lint>
```

This causes the CI build to fail if any UI string is hardcoded in a composable (except `LanguageSelectionScreen` which has a documented exemption).

---

## 18. ShopManager

Stores shop context set during onboarding. Cleared on logout so the next user must pass through shop onboarding again. Language remains unaffected.

```kotlin
// ShopManager.kt
class ShopManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "shop_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(shop: ShopValidateData) {
        prefs.edit()
            .putString(KEY_SLUG, shop.shopSlug)
            .putString(KEY_NAME, shop.shopName)
            .putString(KEY_TZ, shop.shopTimezone)
            .putString(KEY_LOGO, shop.logoUrl)
            .apply()
    }

    fun getSlug(): String?    = prefs.getString(KEY_SLUG, null)
    fun getName(): String?    = prefs.getString(KEY_NAME, null)
    fun getTimezone(): String? = prefs.getString(KEY_TZ, null)
    fun getLogoUrl(): String? = prefs.getString(KEY_LOGO, null)

    /** Called on logout to reset shop context. Language is NOT affected. */
    fun clear() {
        prefs.edit()
            .remove(KEY_SLUG)
            .remove(KEY_NAME)
            .remove(KEY_TZ)
            .remove(KEY_LOGO)
            .apply()
    }

    companion object {
        private const val KEY_SLUG = "shop_slug"
        private const val KEY_NAME = "shop_name"
        private const val KEY_TZ   = "shop_timezone"
        private const val KEY_LOGO = "shop_logo_url"
    }
}
```

### Profile Screen — Shop Association Card

```kotlin
// In ProfileScreen.kt  (read-only section, always visible)
@Composable
fun ShopAssociationCard(shopManager: ShopManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.profile_shop_section_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = shopManager.getName() ?: "",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "@${shopManager.getSlug()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = shopManager.getTimezone() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Persistence Lifecycle Summary

| Data | Storage | Cleared on logout? | Cleared on cache wipe? |
|------|---------|--------------------|----------------------|
| Auth token | `TokenManager` (EncryptedPrefs) | Yes | Yes |
| Shop slug/name/timezone | `ShopManager` (EncryptedPrefs) | **Yes** | Yes |
| Language preference | `LanguageManager` (regular prefs) | **No** | Yes |

> **Decision**: Clear shop slug on logout so a different barber on the same device must reselect shop context via onboarding. Language is preserved (device-level preference).

