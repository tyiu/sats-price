//
//  ManualPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 8/29/24.
//

import Foundation
import BigDecimal

/// Fake price fetcher that returns a randomized price. Useful for development testing without requiring a network call.
class ManualPriceFetcher: PriceFetcher {
    var price: BigDecimal = 1

    func btcToUsd() async throws -> BigDecimal? {
        return price
    }
}
