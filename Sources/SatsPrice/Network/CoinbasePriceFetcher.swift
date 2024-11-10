// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  CoinbasePriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation

private struct CoinbasePriceResponse: Codable {
    let data: CoinbasePrice
}

private struct CoinbasePrice: Codable {
    let amount: String
    let base: String
    let currency: String
}

private struct CoinbaseExchangeRatesResponse: Codable {
    let data: CoinbaseExchangeRatesResponseData
}

private struct CoinbaseExchangeRatesResponseData: Codable {
    let currency: String
    let rates: [String: String]
}

class CoinbasePriceFetcher : PriceFetcher {
    func urlString(toCurrency currency: Locale.Currency) -> String {
        "https://api.coinbase.com/v2/prices/BTC-\(currency.identifier)/spot"
    }

    private static let urlStringForAllCurrencies: String = "https://api.coinbase.com/v2/exchange-rates?currency=BTC"

    func convertBTC(toCurrency currency: Locale.Currency) async throws -> Decimal? {
        do {
            guard let urlComponents = URLComponents(string: urlString(toCurrency: currency)), let url = urlComponents.url else {
                return nil
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let coinbasePriceResponse = try JSONDecoder().decode(CoinbasePriceResponse.self, from: data)
            let coinbasePrice = coinbasePriceResponse.data

            guard coinbasePrice.base == "BTC" && coinbasePrice.currency == currency.identifier else {
                return nil
            }

            #if !SKIP
            return Decimal(string: coinbasePrice.amount)
            #else
            return Decimal(coinbasePrice.amount)
            #endif
        } catch {
            return nil
        }
    }

    func convertBTC(toCurrencies currencies: [Locale.Currency]) async throws -> [Locale.Currency : Decimal] {
        do {
            guard !currencies.isEmpty else {
                return [:]
            }

            if currencies.count == 1, let currency = currencies.first {
                guard let price = try await convertBTC(toCurrency: currency) else {
                    return [:]
                }

                return [currency: price]
            }

            guard let urlComponents = URLComponents(string: CoinbasePriceFetcher.urlStringForAllCurrencies), let url = urlComponents.url else {
                return [:]
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let coinbaseExchangeRatesResponse = try JSONDecoder().decode(CoinbaseExchangeRatesResponse.self, from: data)
            let rates = coinbaseExchangeRatesResponse.data.rates

            guard coinbaseExchangeRatesResponse.data.currency == "BTC" else {
                return [:]
            }

            var results: [Locale.Currency : Decimal] = [:]
            for currency in currencies {
                if let price = rates[currency.identifier] {
#if !SKIP
                    results[currency] = Decimal(string: price)
#else
                    results[currency] = Decimal(price)
#endif
                }
            }

            return results
        } catch {
            return [:]
        }
    }
}
