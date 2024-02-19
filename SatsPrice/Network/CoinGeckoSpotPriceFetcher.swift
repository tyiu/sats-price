//
//  CoinGeckoSpotPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import BigDecimal

private struct CoinGeckoSpotPriceResponse: Codable {
    let bitcoin: CoinGeckoSpotPrice
}

private struct CoinGeckoSpotPrice: Codable {
    let usd: Decimal
}

class CoinGeckoSpotPriceFetcher : SpotPriceFetcher {
    private static let urlString = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd&precision=18"

    func btcToUsd() async throws -> BigDecimal? {
        do {
            guard let urlComponents = URLComponents(string: CoinGeckoSpotPriceFetcher.urlString), let url = urlComponents.url else {
                return nil
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let spotPriceResponse = try JSONDecoder().decode(CoinGeckoSpotPriceResponse.self, from: data)
            let spotPrice = spotPriceResponse.bitcoin

            return BigDecimal(spotPrice.usd)
        } catch {
            return nil
        }
    }
}
