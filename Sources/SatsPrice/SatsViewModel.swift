// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//
//  SatsViewModel.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import SwiftUI

class SatsViewModel: ObservableObject {
    @Published var lastUpdated: Date?

    @Published var btcToUsdStringInternal: String = ""
    @Published var satsStringInternal: String = ""
    @Published var btcStringInternal: String = ""
    @Published var usdStringInternal: String = ""

    var btcToUsdString: String {
        get {
            btcToUsdStringInternal
        }
        set {
            guard btcToUsdStringInternal != newValue else {
                return
            }

            btcToUsdStringInternal = newValue

            if let btc, let btcToUsd {
                usdStringInternal = (btc * btcToUsd).formatString()
            } else {
                usdStringInternal = ""
            }
        }
    }

    var satsString: String {
        get {
            satsStringInternal
        }
        set {
            guard satsStringInternal != newValue else {
                return
            }

            satsStringInternal = newValue

            if let sats {
#if !SKIP
                let btc = sats / Decimal(100000000)
#else
                let btc = sats.divide(Decimal(100000000), 20, java.math.RoundingMode.DOWN)
#endif
                btcStringInternal = btc.formatString()
                if let btcToUsd {
                    usdStringInternal = (btc * btcToUsd).formatString()
                } else {
                    usdStringInternal = ""
                }
            } else {
                btcStringInternal = ""
                usdStringInternal = ""
            }
        }
    }

    var btcString: String {
        get {
            btcStringInternal
        }
        set {
            guard btcStringInternal != newValue else {
                return
            }

            btcStringInternal = newValue

            if let btc {
                let sats = btc * Decimal(100000000)
                satsStringInternal = sats.formatString()

                if let btcToUsd {
                    usdStringInternal = (btc * btcToUsd).formatString()
                } else {
                    usdStringInternal = ""
                }
            } else {
                satsStringInternal = ""
                usdStringInternal = ""
            }
        }
    }

    var usdString: String {
        get {
            usdStringInternal
        }
        set {
            guard usdStringInternal != newValue else {
                return
            }

            usdStringInternal = newValue

            if let usd {
                if let btcToUsd {
#if !SKIP
                    let btc = usd / btcToUsd
#else
                    let btc = usd.divide(btcToUsd, 20, java.math.RoundingMode.DOWN)
#endif
                    btcStringInternal = btc.formatString()

                    let sats = btc * Decimal(100000000)
                    satsStringInternal = sats.formatString()
                } else {
                    satsStringInternal = ""
                    usdStringInternal = ""
                }
            } else {
                satsStringInternal = ""
                usdStringInternal = ""
            }
        }
    }

    var btcToUsd: Decimal? {
#if !SKIP
        return Decimal(string: btcToUsdStringInternal)
#else
        do {
            return Decimal(btcToUsdStringInternal)
        } catch {
            return nil
        }
#endif
    }

    var sats: Decimal? {
#if !SKIP
        return Decimal(string: satsStringInternal)
#else
        do {
            return Decimal(satsStringInternal)
        } catch {
            return nil
        }
#endif
    }

    var btc: Decimal? {
#if !SKIP
        return Decimal(string: btcStringInternal)
#else
        do {
            return Decimal(btcStringInternal)
        } catch {
            return nil
        }
#endif
    }

    var usd: Decimal? {
#if !SKIP
        return Decimal(string: usdStringInternal)
#else
        do {
            return Decimal(usdStringInternal)
        } catch {
            return nil
        }
#endif
    }
}

extension Decimal {
    func formatString() -> String {
#if !SKIP
        return String(describing: self)
#else
        return stripTrailingZeros().toPlainString()
#endif
    }
}
