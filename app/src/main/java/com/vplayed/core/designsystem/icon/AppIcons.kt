package com.vplayed.core.designsystem.icon

import androidx.annotation.DrawableRes
import com.vplayed.R

/**
 * Central icon registry. Icons are vector drawables in `res/drawable` so they can be replaced per
 * brand without code changes. Use with `painterResource(AppIcons.Home)`.
 */
object AppIcons {
    @DrawableRes val Home: Int = R.drawable.ic_home
    @DrawableRes val Tune: Int = R.drawable.ic_tune
    @DrawableRes val Error: Int = R.drawable.ic_error
    @DrawableRes val Empty: Int = R.drawable.ic_inbox
    @DrawableRes val Back: Int = R.drawable.ic_arrow_back
    @DrawableRes val Check: Int = R.drawable.ic_check
    @DrawableRes val Play: Int = R.drawable.ic_play
    @DrawableRes val Add: Int = R.drawable.ic_add
    @DrawableRes val ChevronRight: Int = R.drawable.ic_chevron_right
    @DrawableRes val Schedule: Int = R.drawable.ic_schedule
    @DrawableRes val Notifications: Int = R.drawable.ic_notifications
    @DrawableRes val Views: Int = R.drawable.ic_visibility
    @DrawableRes val Likes: Int = R.drawable.ic_thumb_up
    @DrawableRes val MusicNote: Int = R.drawable.ic_music_note
    @DrawableRes val ShoppingBag: Int = R.drawable.ic_shopping_bag

    // Side menu and login (prototype Material Symbols Rounded).
    @DrawableRes val Menu: Int = R.drawable.ic_menu
    @DrawableRes val Login: Int = R.drawable.ic_login
    @DrawableRes val Logout: Int = R.drawable.ic_logout
    @DrawableRes val Close: Int = R.drawable.ic_close
    @DrawableRes val ArrowForward: Int = R.drawable.ic_arrow_forward
    @DrawableRes val PersonOutline: Int = R.drawable.ic_person_outline
    @DrawableRes val Devices: Int = R.drawable.ic_devices
    @DrawableRes val Download: Int = R.drawable.ic_download
    @DrawableRes val FourK: Int = R.drawable.ic_4k
    @DrawableRes val Redeem: Int = R.drawable.ic_redeem
    @DrawableRes val ChevronLeft: Int = R.drawable.ic_chevron_left
    @DrawableRes val DesktopWindows: Int = R.drawable.ic_desktop_windows
    @DrawableRes val PhoneIphone: Int = R.drawable.ic_phone_iphone
    @DrawableRes val Tv: Int = R.drawable.ic_tv

    /** Third-party sign-in marks: Google keeps its own colors (draw untinted), Apple is tinted. */
    @DrawableRes val Google: Int = R.drawable.ic_google
    @DrawableRes val Apple: Int = R.drawable.ic_apple

    // Bottom navigation: filled when selected, outlined otherwise.
    @DrawableRes val HomeOutlined: Int = R.drawable.ic_home_outlined
    @DrawableRes val Search: Int = R.drawable.ic_search
    @DrawableRes val Promos: Int = R.drawable.ic_promos
    @DrawableRes val PromosOutlined: Int = R.drawable.ic_promos_outlined
    @DrawableRes val Subscription: Int = R.drawable.ic_subscription
    @DrawableRes val SubscriptionOutlined: Int = R.drawable.ic_subscription_outlined
}
