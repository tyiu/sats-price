//
//  PriceSource.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/20/24.
//

import Foundation

enum PriceSource: CaseIterable, CustomStringConvertible {
    
    static var allCases: [PriceSource] {
#if DEBUG
        [.coinbase, .coingecko, .manual, .fake]
#else
        [.coinbase, .coingecko, .manual]
#endif
    }

    case coinbase
    case coingecko
    case manual

#if DEBUG
    case fake
#endif

    var description: String {
        switch self {
        case .coinbase:
            "Coinbase"
        case .coingecko:
            "CoinGecko"
        case .manual:
            "Manual"
#if DEBUG
        case .fake:
            "Fake"
#endif
        }
    }
}
