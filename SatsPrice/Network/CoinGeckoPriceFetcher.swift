//
//  CoinGeckoPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import BigDecimal

private struct CoinGeckoPriceResponse: Codable {
    let bitcoin: CoinGeckoPrice
}

private struct CoinGeckoPrice: Codable {
    let usd: Decimal
}

class CoinGeckoPriceFetcher : PriceFetcher {
    private static let urlString = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd&precision=18"

    func btcToUsd() async throws -> BigDecimal? {
        do {
            guard let urlComponents = URLComponents(string: CoinGeckoPriceFetcher.urlString), let url = urlComponents.url else {
                return nil
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let priceResponse = try JSONDecoder().decode(CoinGeckoPriceResponse.self, from: data)
            let price = priceResponse.bitcoin

            return BigDecimal(price.usd)
        } catch {
            return nil
        }
    }
}
