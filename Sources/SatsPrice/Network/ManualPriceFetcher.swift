// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  ManualPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 8/29/24.
//

import Foundation

/// Fake price fetcher that returns a randomized price. Useful for development testing without requiring a network call.
class ManualPriceFetcher: PriceFetcher {
    var prices: [Locale.Currency: Decimal] = [:]

    func convertBTC(toCurrency currency: Locale.Currency) async throws -> Decimal? {
        prices[currency]
    }

    func convertBTC(toCurrencies currencies: [Locale.Currency]) async throws -> [Locale.Currency : Decimal] {
        guard !currencies.isEmpty else {
            return [:]
        }

        let filteredCurrencies = currencies.filter { prices.keys.contains($0) }
        let priceValues = filteredCurrencies.map { prices[$0, default: Decimal(0)] }
        return Dictionary(uniqueKeysWithValues: zip(filteredCurrencies, priceValues))
    }
}
