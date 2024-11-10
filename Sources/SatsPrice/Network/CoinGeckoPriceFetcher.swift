// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  CoinGeckoPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation

private struct CoinGeckoPriceResponse: Codable {
#if !SKIP
    let bitcoin: [String: Decimal]
#else
    let bitcoin: [String: String]
#endif
}

class CoinGeckoPriceFetcher : PriceFetcher {
    func urlString(toCurrency currency: Locale.Currency) -> String {
        "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=\(currency.identifier.lowercased())&precision=18"
    }

    func urlString(toCurrencies currencies: [Locale.Currency]) -> String {
        let currenciesString = currencies.map { $0.identifier.lowercased() }.joined(separator: ",")
        return "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=\(currenciesString)&precision=18"
    }

    func convertBTC(toCurrency currency: Locale.Currency) async throws -> Decimal? {
        do {
            guard let urlComponents = URLComponents(string: urlString(toCurrency: currency)), let url = urlComponents.url else {
                return nil
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let priceResponse = try JSONDecoder().decode(CoinGeckoPriceResponse.self, from: data)
            guard let price = priceResponse.bitcoin[currency.identifier.lowercased()] else {
                return nil
            }

#if !SKIP
            return price
#else
            return Decimal(price)
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

            guard let urlComponents = URLComponents(string: urlString(toCurrencies: currencies)), let url = urlComponents.url else {
                return [:]
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let priceResponse = try JSONDecoder().decode(CoinGeckoPriceResponse.self, from: data)

            var results: [Locale.Currency : Decimal] = [:]
            for currency in currencies {
                if let price = priceResponse.bitcoin[currency.identifier.lowercased()] {
#if !SKIP
                    results[currency] = price
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
