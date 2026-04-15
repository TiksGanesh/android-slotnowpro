package app.slotnow.slotnowpro.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.slotnow.slotnowpro.R
import app.slotnow.slotnowpro.ui.theme.SlotNowProTheme

@Composable
fun LanguageSelectionScreen(
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    enableSelectionHaptics: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    val canContinue = selectedLanguageCode.isNotBlank()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Button(
                    onClick = onNextClick,
                    enabled = canContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .defaultMinSize(minHeight = 52.dp)
                        .navigationBarsPadding()
                ) {
                    Text(text = stringResource(R.string.onboarding_language_continue))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (showBackButton) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(text = stringResource(R.string.onboarding_language_back))
                }
                Spacer(modifier = Modifier.size(4.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_language_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.onboarding_language_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(OnboardingLanguages.options, key = { it.code }) { option ->
                    val isSelected = option.code == selectedLanguageCode
                    LanguageOptionCard(
                        option = option,
                        isSelected = isSelected,
                        onClick = {
                            if (option.code != selectedLanguageCode) {
                                if (enableSelectionHaptics) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onLanguageSelected(option.code)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    option: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isSelected -> 1.01f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 140),
        label = "languageCardScale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 220),
        label = "languageCardColor"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 4f else 2f,
        animationSpec = tween(durationMillis = 180),
        label = "languageCardElevation"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .scale(scale)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = option.englishLabel
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatLanguageLabel(option),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.code.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            SelectionIndicator(isSelected = isSelected)
        }
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 220),
        label = "selectionIndicatorColor"
    )

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun formatLanguageLabel(option: LanguageOption): String {
    val prefix = option.flagEmoji?.let { "$it " }.orEmpty()
    return "$prefix${option.nativeLabel} • ${option.englishLabel}"
}

@Preview(showBackground = true, name = "Language Screen - English")
@Composable
private fun LanguageSelectionScreenEnglishPreview() {
    SlotNowProTheme {
        LanguageSelectionScreen(
            selectedLanguageCode = "en",
            onLanguageSelected = {},
            onNextClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "hi", name = "Language Screen - Hindi")
@Composable
private fun LanguageSelectionScreenHindiPreview() {
    SlotNowProTheme {
        LanguageSelectionScreen(
            selectedLanguageCode = "hi",
            onLanguageSelected = {},
            onNextClick = {},
            showBackButton = true
        )
    }
}

@Preview(showBackground = true, locale = "mr", name = "Language Screen - Marathi")
@Composable
private fun LanguageSelectionScreenMarathiPreview() {
    SlotNowProTheme {
        LanguageSelectionScreen(
            selectedLanguageCode = "mr",
            onLanguageSelected = {},
            onNextClick = {}
        )
    }
}
