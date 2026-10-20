package com.vplayed.core.ui

/**
 * Shared catalog strings write links as Markdown (`[Terms of use](terms)`), which VPlayed renders
 * natively. Returns the text with the markup removed and the link texts, in order, so the caller can
 * style them.
 */
fun markdownLinks(markdown: String): Pair<String, List<String>> {
    val links = mutableListOf<String>()
    val text = MARKDOWN_LINK.replace(markdown) { match ->
        match.groupValues[1].also(links::add)
    }
    return text to links
}

private val MARKDOWN_LINK = Regex("""\[([^\]]+)]\(([^)]*)\)""")
