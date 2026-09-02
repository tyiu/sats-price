package xyz.tyiu.satsprice.data

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoinGeckoExchangeRateSourceTest {

    private val sampleResponse = """
        {"bitcoin":{"usd":62808.12345678,"eur":54291.5}}
    """.trimIndent()

    @Test
    fun getRates_parsesCoinGeckoResponseIntoExchangeRates() = runTest {
        var requestedUrl = ""
        val mockEngine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(
                content = sampleResponse,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val source = CoinGeckoExchangeRateSource(client)
        val result = source.getRates("BTC")

        assertEquals("BTC", result.base)
        assertEquals(BigDecimal.parseString("62808.12345678"), result.rates["USD"])
        assertEquals(BigDecimal.parseString("54291.5"), result.rates["EUR"])
        assertTrue("ids=bitcoin" in requestedUrl)
        assertTrue("vs_currencies=" in requestedUrl)
        assertTrue("precision=full" in requestedUrl)
    }
}
