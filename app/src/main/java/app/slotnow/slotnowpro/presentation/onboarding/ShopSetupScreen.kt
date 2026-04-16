package app.slotnow.slotnowpro.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.slotnow.slotnowpro.R
import app.slotnow.slotnowpro.presentation.components.LoadingOverlay
import app.slotnow.slotnowpro.ui.theme.SlotNowProTheme

private val shopIdRegex = Regex(pattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
private val successColor = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSetupScreen(
    slugInput: String,
    validationState: ShopValidationState,
    onSlugChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    onLooksGoodClick: () -> Unit,
    modifier: Modifier = Modifier,
    lastUsedShopSlug: String? = null,
    onUseLastShopId: (String) -> Unit = {}
) {
    val sanitizedInput = sanitizeShopInput(slugInput)
    val hasInvalidFormat = sanitizedInput.isNotBlank() && !shopIdRegex.matches(sanitizedInput)
    val isLoading = validationState is ShopValidationState.Loading
    val canVerify = sanitizedInput.isNotBlank() && !hasInvalidFormat
    val ctaEnabled = when {
        isLoading -> false
        validationState is ShopValidationState.Valid -> true
        validationState is ShopValidationState.NetworkError -> false
        validationState is ShopValidationState.NotFound -> false
        validationState is ShopValidationState.Inactive -> false
        validationState is ShopValidationState.Error -> false
        else -> canVerify
    }

    val feedbackState = rememberFeedbackState(
        validationState = validationState,
        hasInvalidFormat = hasInvalidFormat
    )

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Button(
                        onClick = {
                            if (validationState is ShopValidationState.Valid) {
                                onLooksGoodClick()
                            } else {
                                onContinueClick()
                            }
                        },
                        enabled = ctaEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .defaultMinSize(minHeight = 52.dp)
                            .navigationBarsPadding()
                    ) {
                        Text(text = stringResource(R.string.onboarding_shop_continue))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.onboarding_shop_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.onboarding_shop_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = !lastUsedShopSlug.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.onboarding_shop_last_used, lastUsedShopSlug.orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = { onUseLastShopId(lastUsedShopSlug.orEmpty()) }) {
                            Text(text = stringResource(R.string.onboarding_shop_use_last))
                        }
                    }
                }

                OutlinedTextField(
                    value = sanitizedInput,
                    onValueChange = { input ->
                        onSlugChange(sanitizeShopInput(input))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.onboarding_shop_input_label)) },
                    placeholder = { Text(text = stringResource(R.string.onboarding_shop_hint)) },
                    isError = hasInvalidFormat || feedbackState is ShopFeedbackState.Error,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        FieldStateTrailingIcon(
                            validationState = validationState,
                            hasInvalidFormat = hasInvalidFormat
                        )
                    }
                )

                AnimatedContent(
                    targetState = feedbackState,
                    label = "shopFeedbackTransition"
                ) { state ->
                    when (state) {
                        ShopFeedbackState.Idle -> Spacer(modifier = Modifier.size(0.dp))

                        ShopFeedbackState.Loading -> {
                            Text(
                                text = stringResource(R.string.onboarding_shop_verifying),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is ShopFeedbackState.Success -> {
                            Text(
                                text = state.shopName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = successColor
                            )
                        }

                        is ShopFeedbackState.Error -> {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = validationState is ShopValidationState.NetworkError,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    TextButton(
                        onClick = onContinueClick,
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Text(text = stringResource(R.string.retry_label))
                    }
                }
            }
        }

        if (isLoading) {
            LoadingOverlay(message = stringResource(R.string.onboarding_shop_verifying))
        }
    }
}

@Composable
private fun FieldStateTrailingIcon(
    validationState: ShopValidationState,
    hasInvalidFormat: Boolean
) {
    when {
        // Loading state: no icon shown here because a full-screen LoadingOverlay is displayed instead.
        // This keeps the UI focused on the overlay feedback rather than cluttering the field.
        validationState is ShopValidationState.Loading -> Unit

        validationState is ShopValidationState.Valid -> {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = successColor
            )
        }

        hasInvalidFormat ||
            validationState is ShopValidationState.NotFound ||
            validationState is ShopValidationState.Inactive ||
            validationState is ShopValidationState.Error ||
            validationState is ShopValidationState.NetworkError ||
            validationState is ShopValidationState.InvalidInput -> {
            Text(
                text = "!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        else -> Unit
    }
}

@Composable
private fun rememberFeedbackState(
    validationState: ShopValidationState,
    hasInvalidFormat: Boolean
): ShopFeedbackState {
    if (hasInvalidFormat) {
        return ShopFeedbackState.Error(stringResource(R.string.onboarding_shop_invalid_format))
    }

    return when (validationState) {
        ShopValidationState.Idle,
        ShopValidationState.InvalidInput -> ShopFeedbackState.Idle

        ShopValidationState.Loading -> ShopFeedbackState.Loading

        is ShopValidationState.Valid -> ShopFeedbackState.Success(validationState.shopName)

        ShopValidationState.NotFound -> {
            ShopFeedbackState.Error(stringResource(R.string.onboarding_shop_not_found_short))
        }

        ShopValidationState.Inactive -> {
            ShopFeedbackState.Error(stringResource(R.string.onboarding_shop_inactive_short))
        }

        ShopValidationState.NetworkError -> {
            ShopFeedbackState.Error(stringResource(R.string.onboarding_shop_network_error))
        }

        is ShopValidationState.Error -> ShopFeedbackState.Error(validationState.message)
    }
}

private fun sanitizeShopInput(rawInput: String): String {
    val lowered = rawInput.lowercase()
    val normalized = buildString {
        lowered.forEach { char ->
            when {
                char.isLetterOrDigit() -> append(char)
                char == '-' || char == ' ' || char == '_' -> append('-')
            }
        }
    }

    return normalized
        .replace(Regex(pattern = "-+"), "-")
        .trimStart('-')
}

private sealed interface ShopFeedbackState {
    data object Idle : ShopFeedbackState
    data object Loading : ShopFeedbackState
    data class Success(val shopName: String) : ShopFeedbackState
    data class Error(val message: String) : ShopFeedbackState
}

@Preview(showBackground = true)
@Composable
private fun ShopSetupScreenIdlePreview() {
    SlotNowProTheme {
        ShopSetupScreen(
            slugInput = "",
            validationState = ShopValidationState.Idle,
            onSlugChange = {},
            onContinueClick = {},
            onLooksGoodClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopSetupScreenLoadingPreview() {
    SlotNowProTheme {
        ShopSetupScreen(
            slugInput = "cuts-by-raj",
            validationState = ShopValidationState.Loading,
            onSlugChange = {},
            onContinueClick = {},
            onLooksGoodClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopSetupScreenValidPreview() {
    SlotNowProTheme {
        ShopSetupScreen(
            slugInput = "cuts-by-raj",
            validationState = ShopValidationState.Valid(shopName = "Modern Salon"),
            onSlugChange = {},
            onContinueClick = {},
            onLooksGoodClick = {},
            lastUsedShopSlug = "cuts-by-raj"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopSetupScreenErrorPreview() {
    SlotNowProTheme {
        ShopSetupScreen(
            slugInput = "unknown",
            validationState = ShopValidationState.NotFound,
            onSlugChange = {},
            onContinueClick = {},
            onLooksGoodClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "hi", name = "Shop Setup Hindi")
@Composable
private fun ShopSetupScreenHindiPreview() {
    SlotNowProTheme {
        ShopSetupScreen(
            slugInput = "cuts-by-raj",
            validationState = ShopValidationState.NotFound,
            onSlugChange = {},
            onContinueClick = {},
            onLooksGoodClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "mr", name = "Shop Setup Marathi")
@Composable
private fun ShopSetupScreenMarathiPreview() {
    SlotNowProTheme {
        ShopSetupScreen(
            slugInput = "cuts-by-raj",
            validationState = ShopValidationState.Valid(shopName = "राज कट्स"),
            onSlugChange = {},
            onContinueClick = {},
            onLooksGoodClick = {}
        )
    }
}
