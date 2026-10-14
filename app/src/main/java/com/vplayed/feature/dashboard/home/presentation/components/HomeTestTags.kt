package com.vplayed.feature.dashboard.home.presentation.components

/** Test tags for home containers. Content itself is found by text/semantics in tests. */
object HomeTestTags {
    const val SHIMMER = "home_shimmer"
    const val LIST = "home_list"
    const val HERO = "home_hero"
    const val PAGING_LOADING = "home_paging_loading"
    const val PAGING_RETRY = "home_paging_retry"

    fun section(id: String) = "home_section_$id"

    fun row(id: String) = "home_row_$id"
}
