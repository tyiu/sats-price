@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package xyz.tyiu.satsprice.domain

// BigInt keeps this precise for arbitrarily large integer parts (a plain JS number would lose
// precision past 2^53); binding each constructor result to a variable first, rather than
// chaining `new Foo(x).bar()`, avoids a known js() misparse — see SystemCurrencies.web.kt.
actual fun localizedGroupedInteger(digits: String): String = js(
    """(function() { var big = BigInt(digits); var fmt = new Intl.NumberFormat(); return fmt.format(big); })()""",
)

actual fun localizedDecimalSeparator(): String = js(
    """(function() { var fmt = new Intl.NumberFormat(); var parts = fmt.formatToParts(1.5); var found = "."; parts.forEach(function(p) { if (p.type === "decimal") { found = p.value; } }); return found; })()""",
)
