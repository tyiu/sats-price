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
    let model: SatsPriceModel

    @Published var lastUpdated: Date?

    @Published var priceSourceInternal: PriceSource = .coinbase
    let priceFetcherDelegator = PriceFetcherDelegator(.coinbase)

    @Published var satsStringInternal: String = ""
    @Published var btcStringInternal: String = ""
    @Published var selectedCurrencies = Set<Locale.Currency>()
    @Published var currencyValueStrings: [Locale.Currency: String] = [:]

    var currencyPrices: [Locale.Currency: Decimal] = [:]

    let currentCurrency: Locale.Currency = Locale.current.currency ?? Locale.Currency("USD")

    init(model: SatsPriceModel) {
        self.model = model
    }

    var currencies: [Locale.Currency] {
        let commonISOCurrencyCodes = Set(Locale.commonISOCurrencyCodes)
        if commonISOCurrencyCodes.contains(currentCurrency.identifier) {
            return Locale.commonISOCurrencyCodes.map { Locale.Currency($0) }
        } else {
            var commonAndCurrentCurrencies = Locale.commonISOCurrencyCodes
            commonAndCurrentCurrencies.append(currentCurrency.identifier)
            commonAndCurrentCurrencies.sort()
            return commonAndCurrentCurrencies.map { Locale.Currency($0) }
        }
    }

    @MainActor
    func pullSelectedCurrenciesFromDB() async {
        do {
            let selectedCurrencies = Set(try await model.selectedCurrencies().compactMap { Locale.Currency($0.currencyCode) })
            let currenciesToAdd = selectedCurrencies.subtracting(self.selectedCurrencies)
            let currenciesToRemove = self.selectedCurrencies.subtracting(selectedCurrencies)

            self.selectedCurrencies.subtract(currenciesToRemove)
            self.selectedCurrencies.formUnion(currenciesToAdd)
        } catch {
            logger.error("Unable to pull selected currencies from DB. Error: \(error)")
        }
    }

    func addSelectedCurrency(_ currency: Locale.Currency) {
        selectedCurrencies.insert(currency)
        Task {
            try await model.insert(SelectedCurrency(currencyCode: currency.identifier))
        }
    }

    func removeSelectedCurrency(_ currency: Locale.Currency) {
        selectedCurrencies.remove(currency)
        Task {
            try await model.deleteSelectedCurrency(currencyCode: currency.identifier)
        }
    }

    var priceSource: PriceSource {
        get {
            priceSourceInternal
        }
        set {
            priceSourceInternal = newValue
            priceFetcherDelegator.priceSource = newValue
        }
    }

    @MainActor
    func updatePrice() async {
        do {
            let currencies = Set([currentCurrency] + selectedCurrencies)
            let prices = try await priceFetcherDelegator.convertBTC(toCurrencies: Array(currencies))

            currencyPrices = prices
            updateCurrencyValueStrings()
        } catch {
            clearCurrencyValueStrings()
        }
        lastUpdated = Date.now
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

                updateCurrencyValueStrings()
            } else {
                btcStringInternal = ""
                clearCurrencyValueStrings()
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

                updateCurrencyValueStrings()
            } else {
                satsStringInternal = ""
                clearCurrencyValueStrings()
            }
        }
    }

    func updateCurrencyValueStrings(excludedCurrency: Locale.Currency? = nil) {
        if let btc {
            let currencies = Set([currentCurrency] + selectedCurrencies)
                .filter { $0 != excludedCurrency }

            for currency in currencies {
                if let btcToCurrency = btcToCurrency(for: currency) {
                    currencyValueStrings[currency] = (btc * btcToCurrency).formatString()
                } else {
                    currencyValueStrings[currency] = ""
                }
            }
        } else {
            clearCurrencyValueStrings()
        }
    }

    func clearCurrencyValueStrings() {
        for currency in currencyValueStrings.keys {
            currencyValueStrings[currency] = ""
        }
    }

    func currencyValueString(for currency: Locale.Currency) -> Binding<String> {
        Binding<String>(
            get: {
                self.currencyValueStrings[currency, default: ""]
            },
            set: { newValue in
                guard self.currencyValueStrings[currency] != newValue else {
                    return
                }

                self.currencyValueStrings[currency] = newValue

                if let currencyValue = self.currencyValue(for: currency) {
                    if let btcToCurrency = self.currencyPrices[currency] {
    #if !SKIP
                        let btc = currencyValue / btcToCurrency
    #else
                        let btc = currencyValue.divide(btcToCurrency, 20, java.math.RoundingMode.DOWN)
    #endif
                        self.btcStringInternal = btc.formatString()

                        let sats = btc * Decimal(100000000)
                        self.satsStringInternal = sats.formatString()

                        self.updateCurrencyValueStrings(excludedCurrency: currency)
                    } else {
                        self.satsStringInternal = ""
                        self.btcStringInternal = ""
                        self.clearCurrencyValueStrings()
                    }
                } else {
                    self.satsStringInternal = ""
                    self.btcStringInternal = ""
                    self.clearCurrencyValueStrings()
                }
            }
        )
    }

    func currencyValue(for currency: Locale.Currency) -> Decimal? {
        guard let currencyValueString = currencyValueStrings[currency] else {
            return nil
        }

#if !SKIP
        return Decimal(string: currencyValueString)
#else
        do {
            return Decimal(currencyValueString)
        } catch {
            return nil
        }
#endif
    }

    func btcToCurrency(for currency: Locale.Currency) -> Decimal? {
        currencyPrices[currency]
    }

    func btcToCurrencyString(for currency: Locale.Currency) -> Binding<String> {
        Binding<String>(
            get: {
                self.currencyPrices[currency]?.formatString() ?? ""
            },
            set: { newValue in
#if !SKIP
                if let newPrice = Decimal(string: newValue), self.currencyPrices[currency] != newPrice {
                    self.currencyPrices[currency] = Decimal(string: newValue)

                    if let btc = self.btc {
                        self.currencyValueStrings[currency] = (btc * newPrice).formatString()
                    } else {
                        self.currencyValueStrings[currency] = ""
                    }
                }
#else
                do {
                    if let newPrice = Decimal(newValue), self.currencyPrices[currency] != newPrice {
                        self.currencyPrices[currency] = Decimal(newValue)

                        if let btc = self.btc {
                            self.currencyValueStrings[currency] = (btc * newPrice).formatString()
                        } else {
                            self.currencyValueStrings[currency] = ""
                        }
                    }
                } catch {
                    self.currencyPrices.removeValue(forKey: currency)
                }
#endif
            }
        )
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
