// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  PriceFetcherDelegator.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/20/24.
//

import Foundation

class PriceFetcherDelegator: PriceFetcher {
    private let coinbasePriceFetcher = CoinbasePriceFetcher()
    private let coinGeckoPriceFetcher = CoinGeckoPriceFetcher()
    private let manualPriceFetcher = ManualPriceFetcher()
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
        case .manual:
            manualPriceFetcher
#if DEBUG
        case .fake:
            fakePriceFetcher
#endif
        }
    }

    func convertBTC(toCurrency currency: Locale.Currency) async throws -> Decimal? {
        return try await delegate.convertBTC(toCurrency: currency)
    }
}
