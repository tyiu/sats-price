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
#if DEBUG
    private let fakePriceFetcher = FakePriceFetcher()
#endif

    var priceSource: PriceSource

    init(_ priceSource: PriceSource) {
        self.priceSource = priceSource
    }

    private var delegate: PriceFetcher {
        switch priceSource {
        case .coinbase:
            coinbasePriceFetcher
        case .coingecko:
            coinGeckoPriceFetcher
#if DEBUG
        case .fake:
            fakePriceFetcher
#endif
        }
    }

    func btcToUsd() async throws -> BigDecimal? {
        return try await delegate.btcToUsd()
    }
}
