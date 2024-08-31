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
    let bitcoin: CoinGeckoPrice
}

private struct CoinGeckoPrice: Codable {
#if !SKIP
    let usd: Decimal
#else
    let usd: String
#endif
}

class CoinGeckoPriceFetcher : PriceFetcher {
    private static let urlString = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd&precision=18"

    func btcToUsd() async throws -> Decimal? {
        do {
            guard let urlComponents = URLComponents(string: CoinGeckoPriceFetcher.urlString), let url = urlComponents.url else {
                return nil
            }

            let (data, _) = try await URLSession.shared.data(from: url, delegate: nil)

            let priceResponse = try JSONDecoder().decode(CoinGeckoPriceResponse.self, from: data)
            let price = priceResponse.bitcoin

#if !SKIP
            return price.usd
#else
            return Decimal(price.usd)
#endif
        } catch {
            return nil
        }
    }
}
