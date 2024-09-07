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

class CoinbasePriceFetcher : PriceFetcher {
    func urlString(toCurrency currency: Locale.Currency) -> String {
        "https://api.coinbase.com/v2/prices/BTC-\(currency.identifier)/spot"
    }

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
}
