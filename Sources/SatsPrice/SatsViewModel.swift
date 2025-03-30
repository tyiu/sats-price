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
    static let MAXIMUM_BTC = Decimal(21000000)

    private static let SATS_IN_BTC = Decimal(100000000)

    let model: SatsPriceModel

    @Published var lastUpdated: Date?

    @Published var priceSourceInternal: PriceSource = .coinbase
    let priceFetcherDelegator = PriceFetcherDelegator(.coinbase)

    @Published var satsStringInternal: String = ""
    @Published var btcStringInternal: String = ""
    @Published var selectedCurrencies = Set<Locale.Currency>()
    @Published var currencyValueStrings: [Locale.Currency: String] = [:]

    @Published var currencyPrices: [Locale.Currency: Decimal] = [:]
    @Published var currencyPriceStrings: [Locale.Currency: String] = [:]

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
            updateCurrencyPriceStrings()
            updateCurrencyValueStrings()
        } catch {
            clearCurrencyValueStrings()
        }
        lastUpdated = Date.now
    }

    func updateCurrencyPriceStrings() {
        currencyPriceStrings = Dictionary(
            uniqueKeysWithValues: currencyPrices.map { ($0.key, $0.value.formatString(currency: $0.key)) }
        )
    }

    private func priceWithoutGroupingSeparator(_ priceString: String) -> String {
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .decimal
        let decimalSeparator = numberFormatter.decimalSeparator

        return priceString.filter {
            $0.isDigit || String($0) == decimalSeparator
        }
    }

    var satsString: String {
        get {
            satsStringInternal
        }
        set {
            let oldPriceWithoutGroupingSeparator = priceWithoutGroupingSeparator(satsStringInternal)
            let newPriceWithoutGroupingSeparator = priceWithoutGroupingSeparator(newValue)

            guard oldPriceWithoutGroupingSeparator != newPriceWithoutGroupingSeparator else {
                return
            }

            satsStringInternal = newPriceWithoutGroupingSeparator

            if let sats {
#if !SKIP
                // Formatting the internal string after modifying it only if the platform is Apple.
                // Apple does not seem to call get after set until after focus is moved to a different component.
                // Android, on the other hand, does call get immediately after set,
                // which causes text entry issues if the user keeps on entering input.
                satsStringInternal = sats.formatSatsString()

                let btc = sats / SatsViewModel.SATS_IN_BTC
#else
                let btc = sats.divide(SatsViewModel.SATS_IN_BTC, 20, java.math.RoundingMode.DOWN)
#endif
                btcStringInternal = btc.formatBTCString()

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
            let oldPriceWithoutGroupingSeparator = priceWithoutGroupingSeparator(btcStringInternal)
            let newPriceWithoutGroupingSeparator = priceWithoutGroupingSeparator(newValue)

            guard oldPriceWithoutGroupingSeparator != newPriceWithoutGroupingSeparator else {
                return
            }

            btcStringInternal = newPriceWithoutGroupingSeparator

            if let btc {
#if !SKIP
                // Formatting the internal string after modifying it only if the platform is Apple.
                // Apple does not seem to call get after set until after focus is moved to a different component.
                // Android, on the other hand, does call get immediately after set,
                // which causes text entry issues if the user keeps on entering input.
                btcStringInternal = btc.formatBTCString()
#endif

                let sats = btc * SatsViewModel.SATS_IN_BTC
                satsStringInternal = sats.formatSatsString()

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
                    currencyValueStrings[currency] = (btc * btcToCurrency).formatString(currency: currency)
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
                let oldPriceWithoutGroupingSeparator = self.priceWithoutGroupingSeparator(self.currencyValueStrings[currency] ?? "")
                let newPriceWithoutGroupingSeparator = self.priceWithoutGroupingSeparator(newValue)

                guard oldPriceWithoutGroupingSeparator != newPriceWithoutGroupingSeparator else {
                    return
                }

                self.currencyValueStrings[currency] = newPriceWithoutGroupingSeparator

                if let currencyValue = self.currencyValue(for: currency) {
                    if let btcToCurrency = self.currencyPrices[currency] {
#if !SKIP
                        let btc = currencyValue / btcToCurrency
#else
                        let btc = currencyValue.divide(btcToCurrency, 20, java.math.RoundingMode.DOWN)
#endif
                        self.btcStringInternal = btc.formatBTCString()

                        let sats = btc * SatsViewModel.SATS_IN_BTC
                        self.satsStringInternal = sats.formatSatsString()

#if !SKIP
                        // Formatting the internal string after modifying it only if the platform is Apple.
                        // Apple does not seem to call get after set until after focus is moved to a different component.
                        // Android, on the other hand, does call get immediately after set,
                        // which causes text entry issues if the user keeps on entering input.
                        self.updateCurrencyValueStrings(excludedCurrency: nil)
#else
                        self.updateCurrencyValueStrings(excludedCurrency: currency)
#endif
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
        return Decimal(string: priceWithoutGroupingSeparator(currencyValueString))
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
                self.currencyPriceStrings[currency, default: ""]
            },
            set: { newValue in
                let oldPriceWithoutGroupingSeparator = self.priceWithoutGroupingSeparator(self.currencyPriceStrings[currency, default: ""])
                let newPriceWithoutGroupingSeparator = self.priceWithoutGroupingSeparator(newValue)

                guard oldPriceWithoutGroupingSeparator != newPriceWithoutGroupingSeparator else {
                    return
                }

                self.currencyPriceStrings[currency] = newPriceWithoutGroupingSeparator

#if !SKIP
                if let newPrice = Decimal(string: newPriceWithoutGroupingSeparator), self.currencyPrices[currency] != newPrice {
                    self.currencyPrices[currency] = newPrice

                    // Formatting the internal string after modifying it only if the platform is Apple.
                    // Apple does not seem to call get after set until after focus is moved to a different
                    // component. Android, on the other hand, does call get immediately after set,
                    // which causes text entry issues if the user keeps on entering input.
                    self.currencyPriceStrings[currency] = newPrice.formatString(currency: currency)

                    if let btc = self.btc {
                        self.currencyValueStrings[currency] = (btc * newPrice).formatString(currency: currency)
                    } else {
                        self.currencyValueStrings[currency] = ""
                    }
                }
#else
                do {
                    if let newPrice = Decimal(newPriceWithoutGroupingSeparator), self.currencyPrices[currency] != newPrice {
                        self.currencyPrices[currency] = newPrice

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
        let priceWithoutGroupingSeparator = priceWithoutGroupingSeparator(satsStringInternal)
#if !SKIP
        return Decimal(string: priceWithoutGroupingSeparator)
#else
        do {
            return Decimal(priceWithoutGroupingSeparator)
        } catch {
            return nil
        }
#endif
    }

    var btc: Decimal? {
        let priceWithoutGroupingSeparator = priceWithoutGroupingSeparator(btcStringInternal)
#if !SKIP
        return Decimal(string: priceWithoutGroupingSeparator)
#else
        do {
            return Decimal(priceWithoutGroupingSeparator)
        } catch {
            return nil
        }
#endif
    }

    var exceedsMaximum: Bool {
        if let btc, btc > SatsViewModel.MAXIMUM_BTC {
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

    func formatString(minimumFractionDigits: Int, maximumFractionDigits: Int, usesGroupingSeparator: Bool) -> String {
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .decimal
        numberFormatter.minimumFractionDigits = minimumFractionDigits
        numberFormatter.maximumFractionDigits = maximumFractionDigits
        numberFormatter.usesGroupingSeparator = usesGroupingSeparator
#if !SKIP
        return numberFormatter.string(from: NSDecimalNumber(decimal: self)) ?? String(describing: self)
#else
        return numberFormatter.string(from: android.icu.math.BigDecimal(self as java.math.BigDecimal) as NSNumber) ?? stripTrailingZeros().toPlainString()
#endif
    }

    func formatSatsString() -> String {
        formatString(minimumFractionDigits: 0, maximumFractionDigits: 0, usesGroupingSeparator: true)
    }

    func formatBTCString() -> String {
        formatString(minimumFractionDigits: 0, maximumFractionDigits: 8, usesGroupingSeparator: true)
    }

    func formatString(currency: Locale.Currency) -> String {
#if !SKIP
        let currencyFormatter = NumberFormatter()
        currencyFormatter.numberStyle = .currency
        currencyFormatter.currencyCode = currency.identifier

        return formatString(
            minimumFractionDigits: currencyFormatter.minimumFractionDigits,
            maximumFractionDigits: currencyFormatter.maximumFractionDigits,
            usesGroupingSeparator: currencyFormatter.usesGroupingSeparator
        )
#else
        let javaCurrency = java.util.Currency.getInstance(currency.identifier)
        return formatString(
            minimumFractionDigits: javaCurrency.getDefaultFractionDigits(),
            maximumFractionDigits: javaCurrency.getDefaultFractionDigits(),
            usesGroupingSeparator: true
        )
#endif
    }
}

private extension Character {
    var isDigit: Bool {
        self >= "0" && self <= "9"
    }
}
