// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org

import Combine
import SwiftUI

public struct ContentView: View {
    let model: SatsPriceModel

    @StateObject private var satsViewModel: SatsViewModel

    private let dateFormatter: DateFormatter

    init(model: SatsPriceModel) {
        self.model = model

        _satsViewModel = StateObject<SatsViewModel>(wrappedValue: SatsViewModel(model: model))

        dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short
    }

    public var addCurrencyView: some View {
        NavigationLink(
            destination: {
                CurrencyPickerView(satsViewModel: satsViewModel)
            },
            label: {
                Text("Change Currencies")
            }
        )
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
                    Picker("Price Source", selection: $satsViewModel.priceSource) {
                        ForEach(PriceSource.allCases, id: \.self) {
                            Text($0.description)
                        }
                    }

                    HStack {
                        TextField("1 BTC to \(satsViewModel.currentCurrency.identifier)", text: satsViewModel.btcToCurrencyString(for: satsViewModel.currentCurrency))
                            .disabled(satsViewModel.priceSource != .manual)
#if os(iOS) || SKIP
                            .keyboardType(.decimalPad)
#endif
                        if satsViewModel.priceSource != .manual {
                            Button(action: {
                                Task {
                                    await satsViewModel.updatePrice()
                                }
                            }) {
                                Image(systemName: "arrow.clockwise.circle")
                            }
                        }
                    }
                } header: {
                    Text("1 BTC to \(satsViewModel.currentCurrency.identifier)")
                } footer: {
                    if satsViewModel.priceSource != .manual, let lastUpdated = satsViewModel.lastUpdated {
                        Text("Last updated: \(dateFormatter.string(from: lastUpdated))")
                    }
                }

                Section {
                    HStack {
                        Text("Sats")
                        TextField("Sats", text: $satsViewModel.satsString)
#if os(iOS) || SKIP
                            .keyboardType(.numberPad)
#endif
                    }

                    HStack {
                        Text("BTC")
                        TextField("BTC", text: $satsViewModel.btcString)
#if os(iOS) || SKIP
                            .keyboardType(.decimalPad)
#endif
                    }
                } footer: {
                    if satsViewModel.exceedsMaximum {
                        Text("21000000 BTC is the maximum.")
                    }
                }

                Section {
                    HStack {
                        Text(satsViewModel.currentCurrency.identifier)
                        TextField(satsViewModel.currentCurrency.identifier, text: satsViewModel.currencyValueString(for: satsViewModel.currentCurrency))
#if os(iOS) || SKIP
                            .keyboardType(.decimalPad)
#endif
                    }
                }

                Section {
                    if satsViewModel.priceSource != .manual {
                        ForEach(satsViewModel.selectedCurrencies.sorted { $0.identifier < $1.identifier }.filter { $0 != satsViewModel.currentCurrency }, id: \.identifier) { currency in
                            HStack {
                                Text(currency.identifier)
                                TextField(currency.identifier, text: satsViewModel.currencyValueString(for: currency))
#if os(iOS) || SKIP
                                    .keyboardType(.decimalPad)
#endif
                            }
                            .tag(currency.identifier)
                        }
                    }
                }

                if satsViewModel.priceSource != .manual {
                    addCurrencyView
                }
            }
            .task {
                await satsViewModel.pullSelectedCurrenciesFromDB()
                await satsViewModel.updatePrice()
            }
            .onChange(of: satsViewModel.priceSource) { newPriceSource in
                satsViewModel.lastUpdated = nil
                Task {
                    await satsViewModel.updatePrice()
                }
            }
#if os(macOS)
            .formStyle(.grouped)
#endif
        }
    }
}

#Preview {
    let satsPriceModel = try! SatsPriceModel(url: nil)
    ContentView(model: satsPriceModel)
}
