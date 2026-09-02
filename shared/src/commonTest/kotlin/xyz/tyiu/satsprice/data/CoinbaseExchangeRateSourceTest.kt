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

class CoinbaseExchangeRateSourceTest {

    private val sampleResponse = """
        {"data":{"currency":"BTC","rates":{"USD":"50000.12","EUR":"46000.5"}}}
    """.trimIndent()

    @Test
    fun getRates_parsesCoinbaseResponseIntoExchangeRates() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = sampleResponse,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val source = CoinbaseExchangeRateSource(client)
        val result = source.getRates("BTC")

        assertEquals("BTC", result.base)
        assertEquals(BigDecimal.parseString("50000.12"), result.rates["USD"])
        assertEquals(BigDecimal.parseString("46000.5"), result.rates["EUR"])
    }
}
