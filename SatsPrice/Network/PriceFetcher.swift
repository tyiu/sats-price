//
//  PriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import BigDecimal

protocol PriceFetcher {
    func btcToUsd() async throws -> BigDecimal?
}
