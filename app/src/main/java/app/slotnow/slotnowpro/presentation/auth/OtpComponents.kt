package app.slotnow.slotnowpro.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.slotnow.slotnowpro.R
import coil.compose.AsyncImage

/**
 * OTP input row with 6 individual digit boxes.
 *
 * Features:
 * - Auto-focus on first box on screen load
 * - Auto-advance to next box after single digit entry
 * - Backspace support to move to previous box
 * - Full OTP paste support: pastes multi-digit content across remaining boxes
 * - Keyboard dismiss on completion (all 6 digits filled)
 */
@Composable
fun OTPInputRow(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    // Auto-focus first box on load (Unit key ensures this runs only once on composition).
    // Subsequent focus changes are managed implicitly via focusRequesters when user types/pastes.
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val digit = otpValue.getOrNull(index)?.toString() ?: ""

            OTPDigitBox(
                value = digit,
                onValueChange = { newValue ->
                    // Extract only digits from input
                    val digitsOnly = newValue.filter { it.isDigit() }

                    if (digitsOnly.isEmpty()) {
                        // Backspace: remove current digit and move focus back
                        // Use fixed-size buffer to safely handle index operations
                        val buffer = CharArray(6)
                        otpValue.forEachIndexed { i, c -> if (i < 6) buffer[i] = c }
                        // Remove digit at index by shifting left
                        if (index < buffer.size) {
                            for (i in index until 5) {
                                buffer[i] = buffer[i + 1]
                            }
                            buffer[5] = '\u0000'
                        }
                        val newOtp = buffer.takeWhile { it != '\u0000' }.joinToString("")
                        onOtpChange(newOtp)
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    } else if (digitsOnly.length > 1) {
                        // Paste detected: multiple digits in single input event
                        // Distribute pasted content starting from current index
                        val pastedDigits = digitsOnly.take(6 - index)
                        val buffer = CharArray(6)
                        otpValue.forEachIndexed { i, c -> if (i < 6) buffer[i] = c }
                        // Insert pasted digits at current index
                        pastedDigits.forEachIndexed { i, c ->
                            if (index + i < 6) buffer[index + i] = c
                        }
                        val newOtp = buffer.takeWhile { it != '\u0000' }.joinToString("")
                        onOtpChange(newOtp)

                        // Move focus to last pasted position or end
                        val newFocusIndex = minOf(index + pastedDigits.length, 5)
                        if (newFocusIndex < 6) {
                            focusRequesters[newFocusIndex].requestFocus()
                        } else {
                            focusManager.clearFocus()
                        }
                    } else {
                        // Normal single digit input
                        val sanitized = digitsOnly.take(1)
                        val buffer = CharArray(6)
                        otpValue.forEachIndexed { i, c -> if (i < 6) buffer[i] = c }
                        // Set digit at index
                        if (index < 6) buffer[index] = sanitized[0]
                        val newOtp = buffer.takeWhile { it != '\u0000' }.joinToString("")
                        onOtpChange(newOtp)

                        // Auto-advance to next box if not last
                        if (index < 5) {
                            focusRequesters[index + 1].requestFocus()
                        } else {
                            // Last box filled: hide keyboard
                            focusManager.clearFocus()
                        }
                    }
                },
                onKeyEvent = { keyEvent ->
                    when {
                        keyEvent.key == Key.Backspace && digit.isEmpty() && index > 0 -> {
                            // Move to previous box on backspace if current is empty
                            focusRequesters[index - 1].requestFocus()
                            true
                        }

                        else -> false
                    }
                },
                isError = isError,
                focusRequester = focusRequesters[index],
                modifier = Modifier
                    .weight(1f)
                    .size(50.dp)
            )
        }
    }
}

/**
 * Individual OTP digit box with focus, error, and completion states.
 */
@Composable
private fun OTPDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    onKeyEvent: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean) = { false },
    isError: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animate border color based on focus and error state
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else if (value.isNotEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    // Animate box scale for visual feedback
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "digitBoxScale"
    )

    Box(
        modifier = modifier
            .size(50.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .size(50.dp)
                .focusRequester(focusRequester)
                .onKeyEvent(onKeyEvent),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            singleLine = true,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Resend OTP section with countdown timer and resend button.
 * Animates between disabled (countdown) and enabled (button) states.
 */
@Composable
fun ResendSection(
    resendCountdownSeconds: Int,
    isLoading: Boolean,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canResend = resendCountdownSeconds == 0 && !isLoading

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = canResend,
            transitionSpec = {
                (fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150)))
            },
            label = "resendStateTransition"
        ) { showButton ->
            if (showButton) {
                TextButton(
                    onClick = onResendClick,
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(R.string.otp_resend),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.otp_resend_countdown, resendCountdownSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Shop identity card to maintain visual continuity from login screen.
 * Shows shop logo (or initials) and name.
 */
@Composable
fun OTPShopIdentityCard(
    shopName: String,
    shopLogoUrl: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OTPShopAvatar(
            shopName = shopName,
            shopLogoUrl = shopLogoUrl,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = shopName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Shop avatar with logo or initials fallback.
 * Reuses pattern from LoginScreen for consistency.
 */
@Composable
private fun OTPShopAvatar(
    shopName: String,
    shopLogoUrl: String?,
    modifier: Modifier = Modifier
) {
    val initials = shopName
        .trim()
        .split(" ")
        .filter { part -> part.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { part -> part.first().uppercaseChar().toString() }
        .ifBlank { "SN" }

    if (!shopLogoUrl.isNullOrBlank()) {
        // Load actual logo image when URL is available
        AsyncImage(
            model = shopLogoUrl,
            contentDescription = shopName,
            modifier = modifier
        )
        return
    }

    // Fallback to initials when logo URL is not available
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

