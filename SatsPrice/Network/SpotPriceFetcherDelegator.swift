//
//  SpotPriceFetcherDelegator.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/20/24.
//

import Foundation
import BigDecimal

class SpotPriceFetcherDelegator: SpotPriceFetcher {
    private let coinbaseSpotPriceFetcher = CoinbaseSpotPriceFetcher()
    private let coinGeckoSpotPriceFetcher = CoinGeckoSpotPriceFetcher()

    var spotPriceSource: SpotPriceSource = .coinbase

    private var delegate: SpotPriceFetcher {
        switch spotPriceSource {
        case .coinbase:
            coinbaseSpotPriceFetcher
        case .coingecko:
            coinGeckoSpotPriceFetcher
        }
    }

    func btcToUsd() async throws -> BigDecimal? {
        return try await delegate.btcToUsd()
    }
}
