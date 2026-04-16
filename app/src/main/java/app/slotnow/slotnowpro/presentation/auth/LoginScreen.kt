package app.slotnow.slotnowpro.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.slotnow.slotnowpro.R
import app.slotnow.slotnowpro.presentation.components.LoadingOverlay
import app.slotnow.slotnowpro.ui.theme.SlotNowProTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onPhoneChange: (String) -> Unit,
    onSendOtpClick: () -> Unit,
    onMissingShopContext: () -> Unit,
    onRedirectHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val networkErrorMessage = stringResource(R.string.onboarding_shop_network_error)

    LaunchedEffect(uiState.requiresShopSetup) {
        if (uiState.requiresShopSetup) {
            onMissingShopContext()
            onRedirectHandled()
        }
    }

    LaunchedEffect(uiState.phoneError) {
        when (val error = uiState.phoneError) {
            PhoneError.Network -> {
                snackbarHostState.showSnackbar(networkErrorMessage)
            }

            is PhoneError.Generic -> {
                snackbarHostState.showSnackbar(error.message)
            }

            else -> Unit
        }
    }

    val canSendOtp = uiState.isPhoneValid && !uiState.isRequestOtpLoading

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomCTA(
                enabled = canSendOtp,
                isLoading = uiState.isRequestOtpLoading,
                onClick = onSendOtpClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            HeaderSection()
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(durationMillis = 400)) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(durationMillis = 400)
                            )
                ) {
                    if (uiState.shopName.isNullOrBlank()) {
                        ShopIdentitySkeleton(modifier = Modifier.fillMaxWidth())
                    } else {
                        ShopIdentityCard(
                            shopName = uiState.shopName,
                            shopLogoUrl = uiState.shopLogoUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PhoneInputField(
                    value = uiState.phoneInput,
                    isError = uiState.phoneError != null,
                    errorMessage = uiState.phoneError?.toInlineMessage(),
                    onValueChange = onPhoneChange,
                    onDone = {
                        if (canSendOtp) {
                            onSendOtpClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Loading overlay when sending OTP
        if (uiState.isRequestOtpLoading) {
            LoadingOverlay(
                message = stringResource(R.string.login_loading_send_otp)
            )
        }
    }
}

@Composable
private fun HeaderSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShopIdentityCard(
    shopName: String,
    shopLogoUrl: String?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShopAvatar(
                shopName = shopName,
                shopLogoUrl = shopLogoUrl,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = shopName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShopAvatar(
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
        AsyncImage(
            model = shopLogoUrl,
            contentDescription = shopName,
            modifier = modifier.clip(CircleShape)
        )
        return
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ShopIdentitySkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shopIdentitySkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(18.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(14.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun PhoneInputField(
    value: String,
    isError: Boolean,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(R.string.login_phone_label)) },
        placeholder = { Text(text = stringResource(R.string.login_phone_hint)) },
        leadingIcon = {
            Text(
                text = stringResource(R.string.login_country_code),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        isError = isError,
        supportingText = {
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
    )
}

@Composable
private fun BottomCTA(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled || isLoading) 1f else 0.7f,
        animationSpec = tween(durationMillis = 220),
        label = "ctaAlpha"
    )

    Surface(
        modifier = modifier.imePadding(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Button(
            onClick = {
                keyboardController?.hide()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            enabled = enabled && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .defaultMinSize(minHeight = 52.dp)
                .navigationBarsPadding()
                .alpha(contentAlpha)
        ) {
            Text(text = stringResource(R.string.login_send_otp))
        }
    }
}

@Composable
private fun PhoneError.toInlineMessage(): String {
    return when (this) {
        PhoneError.InvalidFormat -> stringResource(R.string.login_error_invalid_phone)
        PhoneError.NotRegistered -> stringResource(R.string.login_error_not_registered)
        PhoneError.ShopUnavailable -> stringResource(R.string.login_error_shop_unavailable)
        PhoneError.Network -> stringResource(R.string.onboarding_shop_network_error)
        is PhoneError.Generic -> message
    }
}


@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    SlotNowProTheme {
        LoginScreen(
            uiState = AuthUiState(
                shopName = "Modern Salon",
                shopSlug = "modern-salon",
                shopLogoUrl = null,
                shopTimezone = "Asia/Kolkata",
                phoneInput = "9876543210",
                isPhoneValid = true
            ),
            onPhoneChange = {},
            onSendOtpClick = {},
            onMissingShopContext = {},
            onRedirectHandled = {}
        )
    }
}
