// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org

import Combine
import SwiftUI

public struct ContentView: View {
    @ObservedObject private var satsViewModel = SatsViewModel()

    @State private var priceSource: PriceSource

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
            guard let price = try await priceFetcherDelegator.btcToUsd() else {
                satsViewModel.btcToUsdString = ""
                return
            }

            satsViewModel.btcToUsdString = "\(price)"
        } catch {
            satsViewModel.btcToUsdString = ""
        }
        satsViewModel.lastUpdated = Date.now
    }

    public var body: some View {
        Form {
            Section {
                Picker("Price Source", selection: $priceSource) {
                    ForEach(PriceSource.allCases, id: \.self) {
                        Text($0.description)
                    }
                }

                HStack {
                    TextField("", text: $satsViewModel.btcToUsdString)
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
                Text("1 BTC to USD")
            } footer: {
                if priceSource != .manual, let lastUpdated = satsViewModel.lastUpdated {
                    Text("Last updated: \(dateFormatter.string(from: lastUpdated))")
                }
            }

            Section {
                TextField("", text: $satsViewModel.satsString)
#if os(iOS) || SKIP
                    .keyboardType(.numberPad)
#endif
            } header: {
                Text("Sats")
            }

            Section {
                TextField("", text: $satsViewModel.btcString)
#if os(iOS) || SKIP
                    .keyboardType(.decimalPad)
#endif
            } header: {
                Text("BTC")
            }

            Section {
                TextField("", text: $satsViewModel.usdString)
#if os(iOS) || SKIP
                    .keyboardType(.decimalPad)
#endif
            } header: {
                Text("USD")
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

#Preview {
#if DEBUG
    ContentView(.fake)
#else
    ContentView(.coinbase)
#endif
}
