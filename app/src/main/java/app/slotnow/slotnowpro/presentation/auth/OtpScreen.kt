package app.slotnow.slotnowpro.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.slotnow.slotnowpro.R
import app.slotnow.slotnowpro.presentation.components.LoadingOverlay
import app.slotnow.slotnowpro.ui.theme.SlotNowProTheme

/**
 * Modern OTP verification screen with 6-digit input boxes.
 * Features:
 * - Individual digit input boxes with auto-focus and auto-advance
 * - Shop identity card for visual continuity
 * - Animated resend section with countdown timer
 * - Error state handling and loading overlays
 * - Full OTP paste support
 */
@Composable
fun OtpScreen(
    uiState: AuthUiState,
    onOtpChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canVerify = uiState.otpInput.length == 6 && !uiState.isVerifyLoading
    val showLoadingOverlay = uiState.isVerifyLoading || uiState.isRequestOtpLoading
    val loadingMessage = when {
        uiState.isVerifyLoading -> stringResource(R.string.otp_loading_verify)
        uiState.isRequestOtpLoading -> stringResource(R.string.otp_loading_resend)
        else -> ""
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                OtpVerifyButton(
                    enabled = canVerify,
                    onClick = onVerifyClick
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                OtpHeaderSection(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp))

                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Shop identity card with animation
                    AnimatedVisibility(
                        visible = !uiState.shopName.isNullOrBlank(),
                        enter = fadeIn(animationSpec = tween(durationMillis = 400)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(durationMillis = 400))
                    ) {
                        if (!uiState.shopName.isNullOrBlank()) {
                            OTPShopIdentityCard(
                                shopName = uiState.shopName,
                                shopLogoUrl = uiState.shopLogoUrl,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // OTP info text
                    Text(
                        text = stringResource(R.string.login_otp_sent, uiState.maskedPhone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // OTP input boxes with 6 individual digit entries
                    OTPInputRow(
                        otpValue = uiState.otpInput,
                        onOtpChange = onOtpChange,
                        isError = uiState.otpError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error message display
                    if (uiState.otpError != null) {
                        Text(
                            text = uiState.otpError.toMessage(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Resend section with animated countdown/button transition
                    ResendSection(
                        resendCountdownSeconds = uiState.resendCountdownSeconds,
                        isLoading = uiState.isRequestOtpLoading,
                        onResendClick = onResendClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (showLoadingOverlay) {
            LoadingOverlay(message = loadingMessage)
        }
    }
}

/**
 * Header section with title and subtitle.
 */
@Composable
private fun OtpHeaderSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.otp_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.otp_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Verify button positioned at bottom with proper padding and navigation bar insets.
 */
@Composable
private fun OtpVerifyButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(text = stringResource(R.string.otp_verify))
        }
    }
}

/**
 * Extension function to convert OtpError sealed type to user-friendly message string.
 * Exhaustive when ensures all error cases are handled per Kotlin conventions.
 */
@Composable
private fun OtpError.toMessage(): String {
    return when (this) {
        OtpError.InvalidCode -> stringResource(R.string.otp_error_invalid)
        OtpError.Locked -> stringResource(R.string.otp_error_locked)
        OtpError.Network -> stringResource(R.string.onboarding_shop_network_error)
        is OtpError.Generic -> message
    }
}

@Preview(showBackground = true)
@Composable
private fun OtpScreenPreview() {
    SlotNowProTheme {
        OtpScreen(
            uiState = AuthUiState(
                shopName = "Modern Salon",
                shopSlug = "modern-salon",
                shopLogoUrl = null,
                phoneInput = "9876543210",
                maskedPhone = "+91 98xxx x3210",
                otpInput = "",
                resendCountdownSeconds = 21
            ),
            onOtpChange = {},
            onVerifyClick = {},
            onResendClick = {}
        )
    }
}

