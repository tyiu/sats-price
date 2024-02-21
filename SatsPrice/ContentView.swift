//
//  ContentView.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import SwiftUI
import BigDecimal

struct ContentView: View {
    @ObservedObject private var satsViewModel = SatsViewModel()

    @State private var spotPriceSource: SpotPriceSource = .coinbase

    private let dateFormatter = DateFormatter()

    private let spotPriceFetcherDelegator = SpotPriceFetcherDelegator()

    init() {
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short
    }

    @MainActor
    func updatePrice() async {
        do {
            guard let price = try await spotPriceFetcherDelegator.btcToUsd() else {
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
                Picker("Price Source", selection: $spotPriceSource) {
                    ForEach(SpotPriceSource.allCases, id: \.self) {
                        Text($0.description)
                    }
                }
                .onChange(of: spotPriceSource) { newSpotPriceSource in
                    spotPriceFetcherDelegator.spotPriceSource = newSpotPriceSource
                    Task {
                        await updatePrice()
                    }
                }

                HStack {
                    TextField("", text: $satsViewModel.btcToUsdString)
                        .disabled(true)
                    Button(action: {
                        Task {
                            await updatePrice()
                        }
                    }) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            } header: {
                Text("1 BTC to USD")
            } footer: {
                Text("Last updated: \(dateFormatter.string(from: satsViewModel.lastUpdated))")
            }

#if os(iOS)
            Section {
                TextField("", text: $satsViewModel.satsString)
                    .keyboardType(.numberPad)
            } header: {
                Text("Sats")
            }

            Section {
                TextField("", text: $satsViewModel.btcString)
                    .keyboardType(.decimalPad)
            } header: {
                Text("BTC")
            }

            Section {
                TextField("", text: $satsViewModel.usdString)
                    .keyboardType(.decimalPad)
            } header: {
                Text("USD")
            }
#else
            Section {
                TextField("", text: $satsViewModel.satsString)
            } header: {
                Text("Sats")
            }

            Section {
                TextField("", text: $satsViewModel.btcString)
            } header: {
                Text("BTC")
            }

            Section {
                TextField("", text: $satsViewModel.usdString)
            } header: {
                Text("USD")
            }
#endif
        }
        .task {
            await updatePrice()
        }
#if os(macOS)
        .formStyle(.grouped)
#endif
    }
}

#Preview {
    ContentView()
}
