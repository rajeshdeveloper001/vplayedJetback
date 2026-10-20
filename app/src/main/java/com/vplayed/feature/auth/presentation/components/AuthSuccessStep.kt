package com.vplayed.feature.auth.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.brandName
import com.vplayed.core.ui.localizedNumber
import com.vplayed.core.ui.rememberReducedMotion
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.login_step
import com.vplayed.shared.l10n.otp_success_note
import com.vplayed.shared.l10n.otp_success_progress
import com.vplayed.shared.l10n.otp_success_title
import org.jetbrains.compose.resources.stringResource

private val CheckIconSize = 44.dp
private const val CHECK_BACKGROUND_ALPHA = 0.16f

/**
 * Success step (prototype `data-step="success"`): green check, "You're all set" and a progress bar
 * that fills while the app is about to open the Dashboard (the ViewModel ends the step after the
 * same duration). The bar is already full when the user turned animations off.
 */
@Composable
internal fun ColumnScope.AuthSuccessStep(notice: String? = null, title: String? = null) {
    val auth = AppDesignSystem.extendedColors.auth
    val dimens = AppDesignSystem.dimens
    val success = AppDesignSystem.extendedColors.success
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            progress.animateTo(1f, tween(OtpViewModel.SUCCESS_MILLIS.toInt(), easing = LinearEasing))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag(OtpTestTags.SUCCESS),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.login_step, localizedNumber(3)).uppercase(),
            style = AppDesignSystem.textStyles.badge,
            color = AppDesignSystem.extendedColors.formAccent,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(dimens.spacing.sm))
        Box(
            modifier = Modifier
                .size(dimens.authSuccessIconSize)
                .background(success.copy(alpha = CHECK_BACKGROUND_ALPHA), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(AppIcons.Check),
                contentDescription = null,
                tint = success,
                modifier = Modifier.size(CheckIconSize),
            )
        }
        Spacer(Modifier.height(dimens.spacing.xl))
        Text(
            text = title ?: stringResource(Res.string.otp_success_title),
            style = AppDesignSystem.textStyles.formTitle,
            color = auth.textStrong,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(dimens.spacing.sm))
        Text(
            text = stringResource(Res.string.otp_success_note, brandName()),
            style = AppDesignSystem.typography.bodyMedium,
            color = auth.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (notice != null) {
            // Not an error: an informational line under the title.
            Spacer(Modifier.height(dimens.spacing.sm))
            Text(
                text = notice,
                style = AppDesignSystem.textStyles.formSupporting,
                color = auth.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(OtpTestTags.NOTICE),
            )
        }
        Spacer(Modifier.height(dimens.spacing.lg))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.spacing.sm)) {
            val barShape = RoundedCornerShape(dimens.authSuccessBarHeight)
            Box(
                modifier = Modifier
                    .width(dimens.authSuccessBarWidth)
                    .height(dimens.authSuccessBarHeight)
                    .clip(barShape)
                    .background(auth.socialBorder),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(dimens.authSuccessBarWidth * progress.value)
                        .background(AppDesignSystem.colors.primary),
                )
            }
            Text(
                text = stringResource(Res.string.otp_success_progress),
                style = AppDesignSystem.textStyles.formSupporting,
                color = auth.textSecondary,
                modifier = Modifier.padding(top = dimens.spacing.xxs),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthSuccessStepPreview() {
    AppTheme {
        Column(Modifier.background(AppDesignSystem.extendedColors.auth.canvas).padding(AppDesignSystem.dimens.spacing.xl)) {
            AuthSuccessStep()
        }
    }
}
