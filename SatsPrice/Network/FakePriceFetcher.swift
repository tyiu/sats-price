//
//  FakePriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/21/24.
//

#if DEBUG
import Foundation
import BigDecimal

/// Fake price fetcher that returns a randomized price. Useful for development testing without requiring a network call.
class FakePriceFetcher: PriceFetcher {
    func btcToUsd() async throws -> BigDecimal? {
        BigDecimal(Double.random(in: 10000...100000))
    }
}
#endif
