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
}
