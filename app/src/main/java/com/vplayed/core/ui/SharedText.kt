package com.vplayed.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.vplayed.R
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/*
 * The app's text comes from the shared string catalog (`com.vplayed.shared.l10n.Res`, the same strings
 * VPlayed shows). Composables use `org.jetbrains.compose.resources.stringResource(Res.string.…)`; these
 * helpers are for code outside composition (Toasts). Both follow the app language: the per-app locale,
 * which is the process's default locale (`AppLocaleManager`).
 */

/** [resource] in the app language, with `%1$s`… replaced by [args]. Reads a cached asset; cheap after the first use. */
fun sharedString(resource: StringResource, vararg args: Any): String = runBlocking { getString(resource, *args) }

/** The plural form of [resource] for [quantity] in the app language. */
fun sharedPlural(resource: PluralStringResource, quantity: Int, vararg args: Any): String =
    runBlocking { getPluralString(resource, quantity, *args) }

/**
 * [value] written with the app language's digits, as `%d` did in this app's own strings (the shared
 * catalog's placeholders are all `%1$s`, so numbers are formatted before they are passed in).
 */
@Composable
fun localizedNumber(value: Int): String = String.format(LocalConfiguration.current.locales[0], "%d", value)

/** The brand name some shared messages name (`%1$s`); never translated. */
@Composable
fun brandName(): String = stringResource(R.string.brand_name)

/** [brandName] outside composition. */
fun Context.brandName(): String = getString(R.string.brand_name)
