//
//  SpotPriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import BigDecimal

protocol SpotPriceFetcher {
    func btcToUsd() async throws -> BigDecimal?
}
