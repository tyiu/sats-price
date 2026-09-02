package xyz.tyiu.satsprice.data

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import xyz.tyiu.satsprice.systemCurrencies
import kotlin.time.Clock

@Serializable
internal data class CoinGeckoSimplePriceResponse(
    val bitcoin: Map<String, JsonElement> = emptyMap(),
)

class CoinGeckoExchangeRateSource(
    private val httpClient: HttpClient,
) : ExchangeRateSource {
    override val id: String = "coingecko"
    override val displayName: String = "CoinGecko"

    private val requestedVsCurrencies: String by lazy {
        systemCurrencies().joinToString(",") { it.code.lowercase() }
    }

    override suspend fun getRates(base: String): ExchangeRates {
        val response: CoinGeckoSimplePriceResponse = httpClient
            .get("https://api.coingecko.com/api/v3/simple/price") {
                parameter("ids", "bitcoin")
                parameter("vs_currencies", requestedVsCurrencies)
                parameter("precision", "full")
            }
            .body()

        return ExchangeRates(
            base = "BTC",
            rates = response.bitcoin
                .mapKeys { (code, _) -> code.uppercase() }
                .mapValues { (_, value) -> BigDecimal.parseString(value.jsonPrimitive.content) },
            fetchedAt = Clock.System.now(),
        )
    }
}
