//
//  CoinbaseSpotPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import BigDecimal

private struct CoinbaseSpotPriceResponse: Codable {
    let data: CoinbaseSpotPrice
}

private struct CoinbaseSpotPrice: Codable {
    let amount: String
    let base: String
    let currency: String
}

class CoinbaseSpotPriceFetcher : SpotPriceFetcher {
    private static let urlString = "https://api.coinbase.com/v2/prices/BTC-USD/spot"
    private static let btc = "BTC"
    private static let usd = "USD"

    func btcToUsd() async throws -> BigDecimal? {
        do {
            guard let urlComponents = URLComponents(string: CoinbaseSpotPriceFetcher.urlString), let url = urlComponents.url else {
                return nil
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let coinbaseSpotPriceResponse = try JSONDecoder().decode(CoinbaseSpotPriceResponse.self, from: data)
            let coinbaseSpotPrice = coinbaseSpotPriceResponse.data

            guard coinbaseSpotPrice.base == CoinbaseSpotPriceFetcher.btc && coinbaseSpotPrice.currency == CoinbaseSpotPriceFetcher.usd else {
                return nil
            }

            return BigDecimal(coinbaseSpotPrice.amount)
        } catch {
            return nil
        }
    }
}
