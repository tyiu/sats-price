// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org

import Combine
import SwiftUI

public struct ContentView: View {
    @ObservedObject private var satsViewModel = SatsViewModel()

    @State private var priceSource: PriceSource

    @State private var expandAddCurrencySection: Bool = false

    private let dateFormatter: DateFormatter

    private let priceFetcherDelegator: PriceFetcherDelegator

    init(_ priceSource: PriceSource) {
        dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short

        self.priceSource = priceSource
        priceFetcherDelegator = PriceFetcherDelegator(priceSource)
    }

    @MainActor
    func updatePrice() async {
        do {
            let currencies = Set([satsViewModel.currentCurrency] + satsViewModel.currencyValueStrings.keys)
            let prices = try await priceFetcherDelegator.convertBTC(toCurrencies: Array(currencies))

            satsViewModel.currencyPrices = prices
            satsViewModel.updateCurrencyValueStrings()
        } catch {
            satsViewModel.clearCurrencyValueStrings()
        }
        satsViewModel.lastUpdated = Date.now
    }

    public var addCurrencyView: some View {
        DisclosureGroup("Add Currency", isExpanded: $expandAddCurrencySection) {
            Picker("Currency", selection: $satsViewModel.selectedCurrency) {
                ForEach(satsViewModel.currencies, id: \.self) { currency in
                    Group {
                        if let localizedCurrency = Locale.current.localizedString(forCurrencyCode: currency.identifier) {
                            Text("\(currency.identifier) - \(localizedCurrency)")
                        } else {
                            Text(currency.identifier)
                        }
                    }
                    .tag(currency.identifier)
                }
            }
#if os(iOS) || SKIP
            .pickerStyle(.navigationLink)
#endif

            let selectedCurrency = satsViewModel.selectedCurrency
            if selectedCurrency == satsViewModel.currentCurrency || satsViewModel.currencyValueStrings.keys.contains(selectedCurrency) {
                Text("\(selectedCurrency.identifier) has already been added")
                    .foregroundStyle(.secondary)
            } else {
                Button("Add \(selectedCurrency.identifier)") {
                    satsViewModel.currencyValueStrings[selectedCurrency] = ""
                    expandAddCurrencySection = false

                    Task {
                        await updatePrice()
                    }
                }
            }
        }
    }

    public func selectedCurrencyBinding(_ currency: Locale.Currency) -> Binding<String> {
        Binding(
            get: {
                satsViewModel.currencyValueStrings[currency, default: ""]
            },
            set: { priceString in
                satsViewModel.currencyValueStrings[currency] = priceString
            }
        )
    }

    public var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Price Source", selection: $priceSource) {
                        ForEach(PriceSource.allCases, id: \.self) {
                            Text($0.description)
                        }
                    }

                    HStack {
                        TextField("1 BTC to \(satsViewModel.currentCurrency.identifier)", text: satsViewModel.btcToCurrencyString(for: satsViewModel.currentCurrency))
                            .disabled(priceSource != .manual)
#if os(iOS) || SKIP
                            .keyboardType(.decimalPad)
#endif
                        if priceSource != .manual {
                            Button(action: {
                                Task {
                                    await updatePrice()
                                }
                            }) {
                                Image(systemName: "arrow.clockwise.circle")
                            }
                        }
                    }
                } header: {
                    Text("1 BTC to \(satsViewModel.currentCurrency.identifier)")
                } footer: {
                    if priceSource != .manual, let lastUpdated = satsViewModel.lastUpdated {
                        Text("Last updated: \(dateFormatter.string(from: lastUpdated))")
                    }
                }

                Section {
                    TextField("Sats", text: $satsViewModel.satsString)
#if os(iOS) || SKIP
                        .keyboardType(.numberPad)
#endif
                } header: {
                    Text("Sats")
                } footer: {
                    if satsViewModel.exceedsMaximum {
                        Text("2100000000000000 sats is the maximum.")
                    }
                }

                Section {
                    TextField("BTC", text: $satsViewModel.btcString)
#if os(iOS) || SKIP
                        .keyboardType(.decimalPad)
#endif
                } header: {
                    Text("BTC")
                } footer: {
                    if satsViewModel.exceedsMaximum {
                        Text("21000000 BTC is the maximum.")
                    }
                }

                Section {
                    TextField(satsViewModel.currentCurrency.identifier, text: satsViewModel.currencyValueString(for: satsViewModel.currentCurrency))
#if os(iOS) || SKIP
                        .keyboardType(.decimalPad)
#endif
                } header: {
                    Text(satsViewModel.currentCurrency.identifier)
                }

                if priceSource != .manual {
                    ForEach(satsViewModel.currencyValueStrings.sorted { $0.key.identifier < $1.key.identifier }.filter { $0.key != satsViewModel.currentCurrency }, id: \.key.identifier) { currencyAndPrice in
                        Section {
                            TextField(currencyAndPrice.key.identifier, text: satsViewModel.currencyValueString(for: currencyAndPrice.key))
#if os(iOS) || SKIP
                                .keyboardType(.decimalPad)
#endif
                        } header: {
                            Text(currencyAndPrice.key.identifier)
                        }
                        .tag(currencyAndPrice.key.identifier)
                    }

                    addCurrencyView
                }
            }
            .task {
                await updatePrice()
            }
            .onChange(of: priceSource) { newPriceSource in
                satsViewModel.lastUpdated = nil
                priceFetcherDelegator.priceSource = newPriceSource
                Task {
                    await updatePrice()
                }
            }
#if os(macOS)
            .formStyle(.grouped)
#endif
        }
    }
}

#Preview {
#if DEBUG
    ContentView(.fake)
#else
    ContentView(.coinbase)
#endif
}
