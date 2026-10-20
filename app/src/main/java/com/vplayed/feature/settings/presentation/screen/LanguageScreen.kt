package com.vplayed.feature.settings.presentation.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.core.designsystem.component.AppCard
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.locale.AppLocaleManager
import com.vplayed.core.ui.AppScreenScaffold
import com.vplayed.core.ui.adaptive.ResponsiveContent
import com.vplayed.core.ui.brandName
import com.vplayed.core.ui.plus
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.feature.settings.presentation.viewmodel.LanguageViewModel
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.language_footer
import com.vplayed.shared.l10n.nav_language
import com.vplayed.shared.language.Language
import java.util.Locale
import org.jetbrains.compose.resources.stringResource

/** Test tags of the Language screen. */
object LanguageTestTags {
    const val LIST = "language_list"
    fun row(code: String) = "language_row_$code"
}

@Composable
fun LanguageRoute(onNavigateBack: () -> Unit, viewModel: LanguageViewModel = hiltViewModel()) {
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val current = viewModel.currentLanguage(AppLocaleManager.preferredLanguages(context))
    LanguageScreen(
        languages = languages,
        selectedCode = current.code,
        onNavigateBack = onNavigateBack,
        onSelect = { language ->
            if (language.code != current.code) {
                (context as? Activity)?.let { AppLocaleManager.setLanguage(it, language.code) }
            }
        },
    )
}

/**
 * Picks the app language from the backend's list, as VPlayed's Language screen does: each row shows
 * the language's own name ("தமிழ்") with its name in the current language underneath ("Tamil"), and
 * the app switches as soon as one is tapped.
 */
@Composable
fun LanguageScreen(
    languages: List<Language>,
    selectedCode: String,
    onNavigateBack: () -> Unit,
    onSelect: (Language) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(title = stringResource(Res.string.nav_language), modifier = modifier, onNavigateBack = onNavigateBack) { padding ->
        val dimens = AppDesignSystem.dimens
        ResponsiveContent(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(LanguageTestTags.LIST),
                contentPadding = padding.plus(dimens.screenHorizontalPadding, dimens.screenVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(dimens.cardSpacing),
            ) {
                item(key = "languages") {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.selectableGroup()) {
                            languages.forEach { language ->
                                LanguageRow(language, selected = language.code == selectedCode, onClick = { onSelect(language) })
                            }
                        }
                    }
                }
                item(key = "footer") {
                    Text(
                        text = stringResource(Res.string.language_footer, brandName()),
                        style = AppDesignSystem.typography.bodySmall,
                        color = AppDesignSystem.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = dimens.spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(language: Language, selected: Boolean, onClick: () -> Unit) {
    val dimens = AppDesignSystem.dimens
    val own = Locale.forLanguageTag(language.code)
    val nativeName = own.getDisplayLanguage(own).replaceFirstChar { it.titlecase(own) }.ifEmpty { language.name }
    val localizedName = own.getDisplayLanguage(LocalConfiguration.current.locales[0])
        .replaceFirstChar { it.titlecase(LocalConfiguration.current.locales[0]) }
        .ifEmpty { language.name }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.listItemHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(LanguageTestTags.row(language.code)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spacing.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = nativeName, style = AppDesignSystem.typography.bodyLarge, color = AppDesignSystem.colors.onSurface)
            if (localizedName != nativeName) {
                Text(text = localizedName, style = AppDesignSystem.typography.bodySmall, color = AppDesignSystem.colors.onSurfaceVariant)
            }
        }
        if (selected) {
            Icon(painterResource(AppIcons.Check), contentDescription = null, tint = AppDesignSystem.colors.primary)
        }
    }
}

@ScreenPreviews
@Composable
private fun LanguageScreenPreview() {
    AppTheme {
        LanguageScreen(languages = Language.FALLBACK, selectedCode = "en", onNavigateBack = {}, onSelect = {})
    }
}
