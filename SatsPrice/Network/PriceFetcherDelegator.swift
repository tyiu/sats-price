//
//  PriceFetcherDelegator.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/20/24.
//

import Foundation
import BigDecimal

class PriceFetcherDelegator: PriceFetcher {
    private let coinbasePriceFetcher = CoinbasePriceFetcher()
    private let coinGeckoPriceFetcher = CoinGeckoPriceFetcher()

    var priceSource: PriceSource = .coinbase

    private var delegate: PriceFetcher {
        switch priceSource {
        case .coinbase:
            coinbasePriceFetcher
        case .coingecko:
            coinGeckoPriceFetcher
        }
    }

    func btcToUsd() async throws -> BigDecimal? {
        return try await delegate.btcToUsd()
    }
}
