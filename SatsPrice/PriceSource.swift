//
//  PriceSource.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/20/24.
//

import Foundation

enum PriceSource: CaseIterable, CustomStringConvertible {
    case coinbase
    case coingecko

    var description: String {
        switch self {
        case .coinbase:
            "Coinbase"
        case .coingecko:
            "CoinGecko"
        }
    }
}
