import Shared
import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = ConverterViewModel()
    @State private var showCurrencyPicker = false

    var body: some View {
        NavigationView {
            Group {
                if let state = viewModel.state {
                    form(for: state)
                } else {
                    ProgressView()
                }
            }
            .navigationTitle("SatsPrice")
        }
    }

    @ViewBuilder
    private func form(for state: IosConverterState) -> some View {
        Form {
            Section(
                header: Text(IosLocalizationKt.localizedFormattedString(
                    resource: MR.strings.shared.btc_to_currency,
                    args: [state.defaultCurrencyCode]
                )),
                footer: Text(state.statusLine)
            ) {
                Picker(
                    IosLocalizationKt.localizedString(resource: MR.strings.shared.price_source),
                    selection: Binding(
                        get: { state.sourceName },
                        set: { viewModel.onSourceSelected($0) }
                    )
                ) {
                    ForEach(viewModel.sourceNames, id: \.self) { name in
                        Text(name).tag(name)
                    }
                }

                HStack {
                    if state.isManualSource {
                        NumericField(
                            placeholder: IosLocalizationKt.localizedString(resource: MR.strings.shared.rate_label),
                            value: state.manualRateInput,
                            keyboardType: .decimalPad,
                            sanitize: sanitizeDecimalInput,
                            onChange: { viewModel.onManualRateChanged($0) }
                        )
                    } else if !state.defaultCurrencyRate.isEmpty {
                        Text(state.defaultCurrencyRate)
                        Spacer()
                    } else {
                        Spacer()
                    }
                    if state.isLoading {
                        ProgressView()
                    } else {
                        Button {
                            viewModel.refresh()
                        } label: {
                            Image(systemName: "arrow.clockwise")
                        }
                        .buttonStyle(.borderless)
                    }
                }
            }

            if let error = state.errorMessage {
                Section {
                    HStack {
                        Text(error).foregroundColor(.red)
                        Spacer()
                        Button(IosLocalizationKt.localizedString(resource: MR.strings.shared.retry)) { viewModel.refresh() }
                    }
                }
            }

            Section(IosLocalizationKt.localizedString(resource: MR.strings.shared.bitcoin_section_title)) {
                amountRow(
                    label: IosLocalizationKt.localizedString(resource: MR.strings.shared.sats_label),
                    value: state.satsAmount,
                    keyboardType: .numberPad,
                    sanitize: sanitizeIntegerInput,
                    onChange: { viewModel.onSatsAmountChanged($0) }
                )
                amountRow(
                    label: IosLocalizationKt.localizedString(resource: MR.strings.shared.btc_label),
                    value: state.btcAmount,
                    keyboardType: .decimalPad,
                    sanitize: sanitizeDecimalInput,
                    onChange: { viewModel.onBtcAmountChanged($0) }
                )
                if state.exceedsMaxSupply {
                    Text(IosLocalizationKt.localizedString(resource: MR.strings.shared.exceeds_max_supply))
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }

            Section(IosLocalizationKt.localizedString(resource: MR.strings.shared.currencies_section_title)) {
                Button(
                    state.selectedCurrencyCodes.isEmpty
                        ? IosLocalizationKt.localizedString(resource: MR.strings.shared.add_currency)
                        : IosLocalizationKt.localizedFormattedString(
                            resource: MR.strings.shared.currencies_selected_count,
                            args: [state.selectedCurrencyCodes.count]
                        )
                ) {
                    showCurrencyPicker = true
                }

                ForEach(state.fiatRows, id: \.code) { row in
                    amountRow(
                        label: row.code,
                        value: row.amount,
                        keyboardType: .decimalPad,
                        sanitize: sanitizeDecimalInput,
                        onChange: { viewModel.onFiatAmountChanged(code: row.code, value: $0) }
                    )
                }
                .onDelete { indexSet in
                    for index in indexSet {
                        viewModel.onFiatCurrencyToggled(state.fiatRows[index].code)
                    }
                }
            }
        }
        .sheet(isPresented: $showCurrencyPicker) {
            CurrencyPickerSheet(
                currencies: state.availableCurrencies,
                selectedCodes: state.selectedCurrencyCodes,
                localeCurrencyCode: state.localeCurrencyCode,
                onToggle: { viewModel.onFiatCurrencyToggled($0) }
            )
        }
    }

    @ViewBuilder
    private func amountRow(
        label: String,
        value: String,
        keyboardType: UIKeyboardType,
        sanitize: @escaping (String) -> String,
        onChange: @escaping (String) -> Void
    ) -> some View {
        HStack {
            Text(label)
            Spacer()
            NumericField(
                placeholder: label,
                value: value,
                keyboardType: keyboardType,
                sanitize: sanitize,
                onChange: onChange,
                alignment: .trailing
            )
        }
    }
}

/// A text field whose displayed text is always forced back to [sanitize]'s output, even for
/// characters typed via a hardware keyboard or pasted in — `.keyboardType` alone only restricts
/// the on-screen keyboard's layout, and a `TextField` bound purely to external/async state
/// (like the shared Kotlin `PriceViewModel`'s `StateFlow`) doesn't reliably self-correct what a
/// user has already typed into its own internal editing buffer. Local `@State` does, since
/// SwiftUI re-renders a view synchronously whenever its own `@State` changes.
private struct NumericField: View {
    let placeholder: String
    let value: String
    let keyboardType: UIKeyboardType
    let sanitize: (String) -> String
    let onChange: (String) -> Void
    var alignment: TextAlignment = .leading

    @State private var text: String = ""

    var body: some View {
        TextField(placeholder, text: $text)
            .keyboardType(keyboardType)
            .multilineTextAlignment(alignment)
            .onAppear { text = value }
            .onChange(of: text) { _, newValue in
                let sanitized = sanitize(newValue)
                if sanitized != newValue {
                    text = sanitized
                }
                if sanitized != value {
                    onChange(sanitized)
                }
            }
            .onChange(of: value) { _, newValue in
                if newValue != text {
                    text = newValue
                }
            }
    }
}

private func sanitizeDecimalInput(_ raw: String) -> String {
    var result = ""
    var seenDot = false
    for char in raw {
        if char.isNumber {
            result.append(char)
        } else if char == "." && !seenDot {
            result.append(char)
            seenDot = true
        }
    }
    return result
}

private func sanitizeIntegerInput(_ raw: String) -> String {
    raw.filter { $0.isNumber }
}
