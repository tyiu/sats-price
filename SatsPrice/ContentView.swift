//
//  ContentView.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import SwiftUI
import BigDecimal
import Combine

struct ContentView: View {
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

    var body: some View {
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
#if os(iOS)
                        .keyboardType(.decimalPad)
#endif
                    if priceSource != .manual {
                        Button(action: {
                            Task {
                                await updatePrice()
                            }
                        }) {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                }
            } header: {
                Text("1 BTC to USD")
            } footer: {
                Text("Last updated: \(dateFormatter.string(from: satsViewModel.lastUpdated))")
            }

            Section {
                TextField("", text: $satsViewModel.satsString)
#if os(iOS)
                    .keyboardType(.numberPad)
#endif
            } header: {
                Text("Sats")
            }

            Section {
                TextField("", text: $satsViewModel.btcString)
#if os(iOS)
                    .keyboardType(.decimalPad)
#endif
            } header: {
                Text("BTC")
            }

            Section {
                TextField("", text: $satsViewModel.usdString)
#if os(iOS)
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
