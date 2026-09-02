package xyz.tyiu.satsprice.data

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
internal data class CoinbaseExchangeRatesResponse(
    val data: CoinbaseExchangeRatesData,
)

@Serializable
internal data class CoinbaseExchangeRatesData(
    val currency: String,
    val rates: Map<String, String>,
)

class CoinbaseExchangeRateSource(
    private val httpClient: HttpClient,
) : ExchangeRateSource {
    override val id: String = "coinbase"
    override val displayName: String = "Coinbase"

    override suspend fun getRates(base: String): ExchangeRates {
        val response: CoinbaseExchangeRatesResponse = httpClient
            .get("https://api.coinbase.com/v2/exchange-rates") {
                parameter("currency", base)
            }
            .body()

        return ExchangeRates(
            base = response.data.currency,
            rates = response.data.rates.mapValues { (_, value) -> BigDecimal.parseString(value) },
            fetchedAt = Clock.System.now(),
        )
    }
}
