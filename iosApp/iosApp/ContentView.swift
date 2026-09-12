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
            // once edit mode is active, which this toggles. Nothing to reorder or delete with
            // only one currency shown, so the button itself is pointless then.
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if let state = viewModel.state, displayedRows(for: state).count > 1 {
                        EditButton()
                    }
                }
                // NumericField's .decimalPad/.numberPad keyboards have no Return/Done key of
                // their own (unlike a standard text keyboard). `.scrollDismissesKeyboard` alone
                // isn't a reliable substitute here — Form's sections without .onMove/.onDelete
                // (Price Source, Bitcoin) don't consistently wire up the drag-to-dismiss gesture,
                // so it only ever worked when dragging within the Currencies section. This is a
                // deterministic fallback that works regardless of which field is focused.
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button {
                        UIApplication.shared.sendAction(
                            #selector(UIResponder.resignFirstResponder),
                            to: nil,
                            from: nil,
                            for: nil
                        )
                    } label: {
                        Text(IosLocalizationKt.localizedString(resource: MR.strings.shared.done))
                    }
                }
            }
            // Kept alongside the Done button above: a free bonus in the Currencies section,
            // where it does work reliably (see the toolbar comment for where it falls short).
            .scrollDismissesKeyboard(.interactively)
            #endif
        }
    }

    private func displayedRows(for state: IosConverterState) -> [FiatRow] {
        state.isManualSource
            ? state.fiatRows.filter { $0.code == state.defaultCurrencyCode }
            : state.fiatRows
    }

    /// Nil for Manual (a manually typed rate has no "last updated" moment to show). Doesn't name
    /// the price source — just when the rate was last fetched.
    private func statusText(for state: IosConverterState) -> String? {
        if state.isManualSource {
            return nil
        }
        if let dateTime = state.lastUpdatedDateTime {
            return IosLocalizationKt.localizedFormattedString(
                resource: MR.strings.shared.updated_status,
                args: [dateTime]
            )
        }
        return IosLocalizationKt.localizedString(resource: MR.strings.shared.loading_rates_status)
    }

    #if os(macOS)
    /// [codes] with [moving] relocated to sit right before [target] — how macOS's manual
    /// drag-and-drop (see `.dropDestination` above) computes its new currency order, since
    /// there's no List to hand the reordering off to natively.
    private func reorderedCodes(_ codes: [String], moving: String, toBeBefore target: String) -> [String] {
        guard moving != target, let fromIndex = codes.firstIndex(of: moving) else { return codes }
        var reordered = codes
        reordered.remove(at: fromIndex)
        let insertIndex = reordered.firstIndex(of: target) ?? reordered.count
        reordered.insert(moving, at: insertIndex)
        return reordered
    }
    #endif

    @ViewBuilder
    private func form(for state: IosConverterState) -> some View {
        Form {
            Section(
                footer: Group {
                    if let statusText = statusText(for: state) {
                        Text(statusText)
                    }
                }
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
                    Text(IosLocalizationKt.localizedFormattedString(
                        resource: MR.strings.shared.btc_to_currency,
                        args: [state.defaultCurrencyCode]
                    ))
                    Spacer()
                    if state.isManualSource {
                        NumericField(
                            placeholder: "",
                            value: state.manualRateInput,
                            keyboardType: .decimalPad,
                            sanitize: sanitizeDecimalInput,
                            onChange: { viewModel.onManualRateChanged($0) },
                            alignment: .trailing
                        )
                    } else if !state.defaultCurrencyRate.isEmpty {
                        Text(NumberFormatKt.groupDigits(value: state.defaultCurrencyRate))
                    }
                    if !state.isManualSource {
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

                HStack {
                    Text(IosLocalizationKt.localizedFormattedString(
                        resource: MR.strings.shared.currency_to_sats,
                        args: [state.defaultCurrencyCode]
                    ))
                    Spacer()
                    if state.isManualSource {
                        NumericField(
                            placeholder: "",
                            value: state.manualSatsPerCurrencyInput,
                            keyboardType: .numberPad,
                            sanitize: sanitizeIntegerInput,
                            onChange: { viewModel.onManualSatsPerCurrencyChanged($0) },
                            alignment: .trailing
                        )
                    } else if !state.oneCurrencyToSats.isEmpty {
                        Text(NumberFormatKt.groupDigits(value: state.oneCurrencyToSats))
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
                if !state.isManualSource {
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
                }

                let displayedRows = displayedRows(for: state)

                ForEach(displayedRows, id: \.code) { row in
                    amountRow(
                        label: currencyFieldLabel(for: row.code),
                        value: row.amount,
                        keyboardType: .decimalPad,
                        sanitize: sanitizeDecimalInput,
                        onChange: { viewModel.onFiatAmountChanged(code: row.code, value: $0) },
                        isPriced: state.pricedCurrencyCodes.contains(row.code),
                        sourceName: state.sourceName,
                        isManualSource: state.isManualSource
                    )
                    .deleteDisabled(row.code == state.defaultCurrencyCode)
                    #if os(macOS)
                    // Form isn't List-backed on macOS, so drag-and-drop reordering has to be
                    // wired up manually rather than coming for free from .onMove below.
                    .draggable(row.code)
                    .dropDestination(for: String.self) { draggedCodes, _ in
                        guard let draggedCode = draggedCodes.first else { return false }
                        viewModel.onFiatCurrenciesReordered(
                            reorderedCodes(displayedRows.map(\.code), moving: draggedCode, toBeBefore: row.code)
                        )
                        return true
                    }
                    .contextMenu {
                        if row.code != state.defaultCurrencyCode {
                            Button(role: .destructive) {
                                viewModel.onFiatCurrencyToggled(row.code)
                            } label: {
                                Label(
                                    IosLocalizationKt.localizedString(
                                        resource: MR.strings.shared.remove_currency_content_description
                                    ),
                                    systemImage: "trash"
                                )
                            }
                        }
                    }
                    #endif
                }
                .onMove(perform: displayedRows.count > 1 ? { indices, newOffset in
                    var codes = displayedRows.map(\.code)
                    codes.move(fromOffsets: indices, toOffset: newOffset)
                    viewModel.onFiatCurrenciesReordered(codes)
                } : nil)
                #if os(iOS)
                .onDelete(perform: displayedRows.count > 1 ? { indexSet in
                    for index in indexSet {
                        viewModel.onFiatCurrencyToggled(displayedRows[index].code)
                    }
                } : nil)
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
                selectedCount: state.selectedCurrencyCodes.count,
                onToggle: { viewModel.onFiatCurrencyToggled($0) },
                onReset: { viewModel.onSelectedCurrenciesReset() }
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
        isManualSource: Bool = false
    ) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(label)
                if !isPriced && !isManualSource {
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
            // Bounded rather than unconstrained so the field doesn't grow to fill the whole row
            // (the original reason for a fixed width here), but wide enough for the largest
            // values this app actually displays — e.g. a Sats amount at the max BTC supply
            // (`maxSupplyBtcAmount` * 100,000,000) is a 16-digit, comma-grouped ~21-character
            // string, which a 140pt cap was clipping.
            .frame(minWidth: 80, idealWidth: 140, maxWidth: 220)
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
