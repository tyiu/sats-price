//
//  SatsViewModel.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import BigDecimal

class SatsViewModel: ObservableObject {
    @Published private(set) var btcToUsd: BigDecimal = BigDecimal.nan
    @Published var lastUpdated: Date = Date.now

    @Published private(set) var sats: BigDecimal = 0
    @Published private(set) var btc: BigDecimal = 0
    @Published private(set) var usd: BigDecimal = 0

    var btcToUsdString: String {
        get { btcToUsd.asString(.plain) }
        set {
            self.btcToUsd = BigDecimal(newValue)
            self.usd = btc.multiply(btcToUsd, Rounding(.down, 20))
        }
    }

    var satsString: String {
        get { sats.asString(.plain) }
        set {
            self.sats = BigDecimal(newValue)

            let preciseDivide = sats.divide(100000000)
            if preciseDivide.isNaN {
                self.btc = sats.divide(100000000, Rounding(.down, 20))
            } else {
                self.btc = sats.divide(100000000)
            }

            self.usd = btc.multiply(btcToUsd, Rounding(.down, 20))
        }
    }

    var btcString: String {
        get { btc.asString(.plain) }
        set {
            self.btc = BigDecimal(newValue)
            self.sats = btc.multiply(100000000, Rounding(.down, 20))
            self.usd = btc.multiply(btcToUsd, Rounding(.down, 20))
        }
    }

    var usdString: String {
        get { usd.asString(.plain) }
        set {
            self.usd = BigDecimal(newValue)

            let preciseDivide = usd.divide(btcToUsd)
            if preciseDivide.isNaN {
                self.btc = usd.divide(btcToUsd, Rounding(.down, 20))
            } else {
                self.btc = usd.divide(btcToUsd)
            }

            self.sats = btc.multiply(100000000, Rounding(.down, 20))
        }
    }
}
