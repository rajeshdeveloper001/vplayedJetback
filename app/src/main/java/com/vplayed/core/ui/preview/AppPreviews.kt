package com.vplayed.core.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Preview

private const val PHONE = "spec:width=411dp,height=891dp"
private const val PHONE_LANDSCAPE = "spec:width=891dp,height=411dp,orientation=landscape"
private const val FOLDABLE = "spec:width=673dp,height=841dp"
private const val TABLET = "spec:width=1280dp,height=800dp,dpi=240"
private const val DARK = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL

/**
 * Full-screen previews: compact (phone portrait), medium (foldable), expanded (tablet) and
 * phone landscape, in light and dark. Use on every screen-level composable.
 */
@Preview(name = "Phone · Light", group = "Phone", device = PHONE, showSystemUi = true)
@Preview(name = "Phone · Dark", group = "Phone", device = PHONE, showSystemUi = true, uiMode = DARK)
@Preview(name = "Phone landscape · Light", group = "Landscape", device = PHONE_LANDSCAPE, showSystemUi = true)
@Preview(name = "Phone landscape · Dark", group = "Landscape", device = PHONE_LANDSCAPE, showSystemUi = true, uiMode = DARK)
@Preview(name = "Foldable · Light", group = "Foldable", device = FOLDABLE, showSystemUi = true)
@Preview(name = "Tablet · Light", group = "Tablet", device = TABLET, showSystemUi = true)
@Preview(name = "Tablet · Dark", group = "Tablet", device = TABLET, showSystemUi = true, uiMode = DARK)
annotation class ScreenPreviews

/** Component previews in light, dark and a large font scale. */
@Preview(name = "Light", group = "Theme", showBackground = true)
@Preview(name = "Dark", group = "Theme", showBackground = true, uiMode = DARK, backgroundColor = 0xFF141218)
@Preview(name = "Font scale 1.5", group = "Accessibility", showBackground = true, fontScale = 1.5f)
annotation class ComponentPreviews
