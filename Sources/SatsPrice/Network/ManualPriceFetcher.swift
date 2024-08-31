// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  ManualPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 8/29/24.
//

import Foundation

/// Fake price fetcher that returns a randomized price. Useful for development testing without requiring a network call.
class ManualPriceFetcher: PriceFetcher {
    var price: Decimal = Decimal(1)

    func btcToUsd() async throws -> Decimal? {
        return price
    }
}
