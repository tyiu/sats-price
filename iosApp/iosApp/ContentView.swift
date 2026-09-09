import Foundation
import Shared
import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = ConverterViewModel()
    @State private var showCurrencyPicker = false

    var body: some View {
        NavigationStack {
            Group {
                if let state = viewModel.state {
                    form(for: state)
                } else {
                    ProgressView()
                }
            }
            .navigationTitle("SatsPrice")
            #if os(iOS)
            // macOS Lists support drag-to-reorder directly; iOS only shows reorder handles
            // once edit mode is active, which this toggles.
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    EditButton()
                }
            }
            #endif
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
                        Text(NumberFormatKt.groupDigits(value: state.defaultCurrencyRate))
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
                    exceedsMaxSupplyText
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }

            Section(IosLocalizationKt.localizedString(resource: MR.strings.shared.currencies_section_title)) {
                Button(
                    state.selectedCurrencyCodes.count <= 1
                        ? IosLocalizationKt.localizedString(resource: MR.strings.shared.add_currency)
                        : IosLocalizationKt.localizedFormattedString(
                            resource: MR.strings.shared.currencies_selected_count,
                            args: [state.selectedCurrencyCodes.count]
                        )
                ) {
                    showCurrencyPicker = true
                }

                ForEach(Array(state.fiatRows.enumerated()), id: \.element.code) { index, row in
                    amountRow(
                        label: currencyFieldLabel(for: row.code),
                        value: row.amount,
                        keyboardType: .decimalPad,
                        sanitize: sanitizeDecimalInput,
                        onChange: { viewModel.onFiatAmountChanged(code: row.code, value: $0) },
                        isPriced: state.pricedCurrencyCodes.contains(row.code),
                        sourceName: state.sourceName,
                        onMoveUp: index > 0 ? {
                            var codes = state.fiatRows.map(\.code)
                            codes.move(fromOffsets: [index], toOffset: index - 1)
                            viewModel.onFiatCurrenciesReordered(codes)
                        } : nil,
                        onMoveDown: index < state.fiatRows.count - 1 ? {
                            var codes = state.fiatRows.map(\.code)
                            codes.move(fromOffsets: [index], toOffset: index + 2)
                            viewModel.onFiatCurrenciesReordered(codes)
                        } : nil
                    )
                    .deleteDisabled(row.code == state.defaultCurrencyCode)
                }
                .onMove { indices, newOffset in
                    var codes = state.fiatRows.map(\.code)
                    codes.move(fromOffsets: indices, toOffset: newOffset)
                    viewModel.onFiatCurrenciesReordered(codes)
                }
                #if os(iOS)
                .onDelete { indexSet in
                    for index in indexSet {
                        viewModel.onFiatCurrencyToggled(state.fiatRows[index].code)
                    }
                }
                #endif
            }
        }
        #if os(macOS)
        .formStyle(.grouped)
        #endif
        .sheet(isPresented: $showCurrencyPicker) {
            CurrencyPickerSheet(
                currentCurrency: state.currentCurrency,
                selectedOtherCurrencies: state.selectedOtherCurrencies,
                unselectedCurrencies: state.unselectedCurrencies,
                pricedCurrencyCodes: state.pricedCurrencyCodes,
                sourceName: state.sourceName,
                localeCurrencyCode: state.localeCurrencyCode,
                onToggle: { viewModel.onFiatCurrencyToggled($0) }
            )
        }
        .environment(\.openURL, OpenURLAction { url in
            guard url == Self.maxSupplyLinkURL else { return .systemAction }
            viewModel.onBtcAmountChanged(Self.maxSupplyBtcAmount)
            return .handled
        })
    }

    // `maxSupplyBtcAmount` is the sanitized digit-only form CurrencyConverter.MAX_BTC_SUPPLY
    // formats to, mirroring the shared Compose UI. `maxSupplyText` is locale-grouped (e.g.
    // "21,000,000 BTC" in en-US, "21.000.000 BTC" in de-DE) since it's substituted into the
    // localized sentence below, then searched for verbatim to turn it into a link — the two
    // always match exactly.
    private static let maxSupplyBtcAmount = "21000000"
    private static var maxSupplyText: String { "\(NumberFormatKt.groupDigits(value: maxSupplyBtcAmount)) BTC" }
    private static let maxSupplyLinkURL = URL(string: "satsprice://set-max-supply")!

    private var exceedsMaxSupplyText: Text {
        let linkText = Self.maxSupplyText
        let warning = IosLocalizationKt.localizedFormattedString(
            resource: MR.strings.shared.exceeds_max_supply,
            args: [linkText]
        )
        var attributed = AttributedString(warning)
        if let range = attributed.range(of: linkText) {
            attributed[range].link = Self.maxSupplyLinkURL
            attributed[range].underlineStyle = .single
            // SwiftUI renders `.link` runs in the accent color regardless of the Text's own
            // .foregroundColor modifier, so it has to be set directly on the link's range.
            attributed[range].foregroundColor = .red
        }
        return Text(attributed)
    }

    @ViewBuilder
    private func amountRow(
        label: String,
        value: String,
        keyboardType: NumericFieldKeyboard,
        sanitize: @escaping (String) -> String,
        onChange: @escaping (String) -> Void,
        isPriced: Bool = true,
        sourceName: String = "",
        onMoveUp: (() -> Void)? = nil,
        onMoveDown: (() -> Void)? = nil
    ) -> some View {
        HStack {
            #if os(macOS)
            // macOS's Form isn't List-backed, so .onMove's drag-to-reorder has no effect here;
            // these buttons are macOS's equivalent of iOS's Edit-mode drag handles.
            if onMoveUp != nil || onMoveDown != nil {
                VStack(spacing: 2) {
                    Button(action: { onMoveUp?() }) {
                        Image(systemName: "chevron.up")
                            .frame(width: 16, height: 10)
                    }
                    .disabled(onMoveUp == nil)
                    Button(action: { onMoveDown?() }) {
                        Image(systemName: "chevron.down")
                            .frame(width: 16, height: 10)
                    }
                    .disabled(onMoveDown == nil)
                }
                .font(.system(size: 10))
                .buttonStyle(.borderless)
            }
            #endif
            VStack(alignment: .leading) {
                Text(label)
                if !isPriced {
                    Text(IosLocalizationKt.localizedFormattedString(
                        resource: MR.strings.shared.currency_not_priced,
                        args: [sourceName]
                    ))
                    .font(.caption2)
                    .foregroundColor(.red)
                }
            }
            Spacer()
            NumericField(
                placeholder: "",
                value: isPriced ? value : "",
                keyboardType: keyboardType,
                sanitize: sanitize,
                onChange: onChange,
                alignment: .trailing
            )
            .disabled(!isPriced)
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
    let keyboardType: NumericFieldKeyboard
    let sanitize: (String) -> String
    let onChange: (String) -> Void
    var alignment: TextAlignment = .leading

    @State private var text: String = ""

    var body: some View {
        TextField(placeholder, text: $text)
            #if os(iOS)
            .keyboardType(keyboardType.uiKeyboardType)
            #endif
            .multilineTextAlignment(alignment)
            #if os(macOS)
            .frame(maxWidth: 140)
            #endif
            .onAppear { text = NumberFormatKt.groupDigits(value: value) }
            .onChange(of: text) { newValue in
                // sanitize() already drops whatever grouping separator groupDigits() inserts
                // (neither a digit nor the locale's decimal separator), so it doubles as
                // ungrouping the field's raw text.
                let sanitized = sanitize(newValue)
                let grouped = NumberFormatKt.groupDigits(value: sanitized)
                if grouped != newValue {
                    text = grouped
                }
                if sanitized != value {
                    onChange(sanitized)
                }
            }
            .onChange(of: value) { newValue in
                let grouped = NumberFormatKt.groupDigits(value: newValue)
                if grouped != text {
                    text = grouped
                }
            }
    }
}

private func currencyFieldLabel(for code: String) -> String {
    if let flag = CurrencyFlagKt.currencyFlagEmoji(code: code) {
        return "\(flag) \(code)"
    }
    return code
}

/// Unlike the shared Kotlin `sanitizeDecimalInput` (which never sees grouping separators, since
/// Compose's VisualTransformation never feeds its grouped display back into the real value),
/// `NumericField`'s single `text` buffer *is* the grouped display, re-fed through this on every
/// edit. So a raw "." can't get a universal pass as a stand-in decimal point here the way it does
/// in Kotlin — in a locale where "," is the decimal separator, groupDigits() uses "." for
/// grouping, and treating it as a second decimal point would corrupt the value. Only the exact
/// locale decimal separator (which is "." itself in e.g. en-US) counts; anything else non-numeric
/// is grouping noise to drop.
private func sanitizeDecimalInput(_ raw: String) -> String {
    let decimalSeparator = NumberFormat_appleKt.localizedDecimalSeparator()
    var result = ""
    var seenDot = false
    for char in raw {
        if char.isNumber {
            result.append(char)
        } else if !seenDot && String(char) == decimalSeparator {
            result.append(".")
            seenDot = true
        }
    }
    return result
}

private func sanitizeIntegerInput(_ raw: String) -> String {
    raw.filter { $0.isNumber }
}

/// `UIKeyboardType` doesn't exist on macOS (no on-screen keyboard), so this stands in for it
/// across both platforms; only iOS actually applies it, via `uiKeyboardType` below.
private enum NumericFieldKeyboard {
    case decimalPad
    case numberPad
}

#if os(iOS)
private extension NumericFieldKeyboard {
    var uiKeyboardType: UIKeyboardType {
        switch self {
        case .decimalPad: return .decimalPad
        case .numberPad: return .numberPad
        }
    }
}
#endif
