// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  CurrencyPickerView.swift
//  sats-price
//
//  Created by Terry Yiu on 11/10/24.
//

import SwiftUI

struct CurrencyPickerView: View {
    @ObservedObject var satsViewModel: SatsViewModel

    var body: some View {
        let currentCurrency = satsViewModel.currentCurrency

        List {
            Section("Current Currency") {
                let currentCurrency = satsViewModel.currentCurrency
                if let localizedCurrency = Locale.current.localizedString(forCurrencyCode: currentCurrency.identifier) {
                    Text("\(currentCurrency.identifier) - \(localizedCurrency)")
                } else {
                    Text(currentCurrency.identifier)
                }
            }

            if !satsViewModel.selectedCurrencies.isEmpty {
                Section("Selected Currencies") {
                    ForEach(satsViewModel.selectedCurrencies.filter { $0 != currentCurrency }.sorted { $0.identifier < $1.identifier }, id: \.identifier) { currency in
                        Button(
                            action: {
                                satsViewModel.removeSelectedCurrency(currency)
                            },
                            label: {
                                HStack {
                                    Group {
                                        if let localizedCurrency = Locale.current.localizedString(forCurrencyCode: currency.identifier) {
                                            Text("\(currency.identifier) - \(localizedCurrency)")
                                        } else {
                                            Text(currency.identifier)
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                    Image(systemName: "checkmark")
                                }
                            }
                        )
                        .buttonStyle(.plain)
                    }
                }
            }

            Section("Currencies") {
                ForEach(satsViewModel.currencies.filter { $0 != currentCurrency && !satsViewModel.selectedCurrencies.contains($0) }, id: \.identifier) { currency in
                    Button(
                        action: {
                            satsViewModel.addSelectedCurrency(currency)
                        },
                        label: {
                            if let localizedCurrency = Locale.current.localizedString(forCurrencyCode: currency.identifier) {
                                Text("\(currency.identifier) - \(localizedCurrency)")
                            } else {
                                Text(currency.identifier)
                            }
                        }
                    )
                    .buttonStyle(.plain)
                }
            }
        }
        .onDisappear(perform: {
            Task {
                await satsViewModel.updatePrice()
            }
        })
    }
}

#Preview {
    let satsPriceModel = try! SatsPriceModel(url: nil)
    CurrencyPickerView(satsViewModel: SatsViewModel(model: satsPriceModel))
}
