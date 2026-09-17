package xyz.tyiu.satsprice

import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        HtmlApp()
    }
}
