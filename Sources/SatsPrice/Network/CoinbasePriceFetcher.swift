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
    private static let urlString = "https://api.coinbase.com/v2/prices/BTC-USD/spot"
    private static let btc = "BTC"
    private static let usd = "USD"

    func btcToUsd() async throws -> Decimal? {
        do {
            guard let urlComponents = URLComponents(string: CoinbasePriceFetcher.urlString), let url = urlComponents.url else {
                return nil
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let coinbasePriceResponse = try JSONDecoder().decode(CoinbasePriceResponse.self, from: data)
            let coinbasePrice = coinbasePriceResponse.data

            guard coinbasePrice.base == CoinbasePriceFetcher.btc && coinbasePrice.currency == CoinbasePriceFetcher.usd else {
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
