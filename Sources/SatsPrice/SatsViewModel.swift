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

    @Published var btcToCurrencyStringInternal: String = ""
    @Published var satsStringInternal: String = ""
    @Published var btcStringInternal: String = ""
    @Published var currencyValueStringInternal: String = ""
    @Published var selectedCurrency: Locale.Currency = Locale.current.currency ?? Locale.Currency("USD")

    var currencies: [Locale.Currency] {
        let commonISOCurrencyCodes = Set(Locale.commonISOCurrencyCodes)
        let currentCurrency = Locale.current.currency ?? Locale.Currency("USD")
        if commonISOCurrencyCodes.contains(currentCurrency.identifier) {
            return Locale.commonISOCurrencyCodes.map { Locale.Currency($0) }
        } else {
            var commonAndCurrentCurrencies = Locale.commonISOCurrencyCodes
            commonAndCurrentCurrencies.append(currentCurrency.identifier)
            commonAndCurrentCurrencies.sort()
            return commonAndCurrentCurrencies.map { Locale.Currency($0) }
        }
    }

    var btcToCurrencyString: String {
        get {
            btcToCurrencyStringInternal
        }
        set {
            guard btcToCurrencyStringInternal != newValue else {
                return
            }

            btcToCurrencyStringInternal = newValue

            if let btc, let btcToCurrency {
                currencyValueStringInternal = (btc * btcToCurrency).formatString()
            } else {
                currencyValueStringInternal = ""
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
                if let btcToCurrency {
                    currencyValueStringInternal = (btc * btcToCurrency).formatString()
                } else {
                    currencyValueStringInternal = ""
                }
            } else {
                btcStringInternal = ""
                currencyValueStringInternal = ""
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

                if let btcToCurrency {
                    currencyValueStringInternal = (btc * btcToCurrency).formatString()
                } else {
                    currencyValueStringInternal = ""
                }
            } else {
                satsStringInternal = ""
                currencyValueStringInternal = ""
            }
        }
    }

    var currencyValueString: String {
        get {
            currencyValueStringInternal
        }
        set {
            guard currencyValueStringInternal != newValue else {
                return
            }

            currencyValueStringInternal = newValue

            if let currencyValue {
                if let btcToCurrency {
#if !SKIP
                    let btc = currencyValue / btcToCurrency
#else
                    let btc = currencyValue.divide(btcToCurrency, 20, java.math.RoundingMode.DOWN)
#endif
                    btcStringInternal = btc.formatString()

                    let sats = btc * Decimal(100000000)
                    satsStringInternal = sats.formatString()
                } else {
                    satsStringInternal = ""
                    btcStringInternal = ""
                    currencyValueStringInternal = ""
                }
            } else {
                satsStringInternal = ""
                btcStringInternal = ""
                currencyValueStringInternal = ""
            }
        }
    }

    var btcToCurrency: Decimal? {
#if !SKIP
        return Decimal(string: btcToCurrencyStringInternal)
#else
        do {
            return Decimal(btcToCurrencyStringInternal)
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

    var currencyValue: Decimal? {
#if !SKIP
        return Decimal(string: currencyValueStringInternal)
#else
        do {
            return Decimal(currencyValueStringInternal)
        } catch {
            return nil
        }
#endif
    }

    var exceedsMaximum: Bool {
        if let btc, btc > Decimal(21000000) {
            return true
        }
        return false
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
