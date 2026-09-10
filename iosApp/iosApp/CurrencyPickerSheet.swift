import Shared
import SwiftUI

struct CurrencyPickerSheet: View {
    let currentCurrency: CurrencyInfo
    let selectedOtherCurrencies: [CurrencyInfo]
    let unselectedCurrencies: [CurrencyInfo]
    let pricedCurrencyCodes: [String]
    let sourceName: String
    let localeCurrencyCode: String?
    let onToggle: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var searchQuery = ""

    private func matches(_ info: CurrencyInfo) -> Bool {
        SystemCurrenciesKt.matchesCurrencySearch(info: info, query: searchQuery)
    }

    var body: some View {
        NavigationStack {
            List {
                if matches(currentCurrency) {
                    Section(IosLocalizationKt.localizedString(resource: MR.strings.shared.current_currency_section_title)) {
                        currencyRow(for: currentCurrency, isSelected: true, onTap: nil)
                    }
                }

                let matchingSelectedOthers = selectedOtherCurrencies.filter(matches)
                if !matchingSelectedOthers.isEmpty {
                    Section(IosLocalizationKt.localizedString(resource: MR.strings.shared.selected_currencies_section_title)) {
                        ForEach(matchingSelectedOthers, id: \.code) { info in
                            currencyRow(for: info, isSelected: true, onTap: { onToggle(info.code) })
                        }
                    }
                }

                let matchingUnselected = unselectedCurrencies.filter(matches)
                if !matchingUnselected.isEmpty {
                    Section(IosLocalizationKt.localizedString(resource: MR.strings.shared.currencies_section_title)) {
                        ForEach(matchingUnselected, id: \.code) { info in
                            currencyRow(for: info, isSelected: false, onTap: { onToggle(info.code) })
                        }
                    }
                }
            }
            .navigationTitle(IosLocalizationKt.localizedString(resource: MR.strings.shared.currencies_section_title))
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(IosLocalizationKt.localizedString(resource: MR.strings.shared.done)) { dismiss() }
                }
            }
        }
        // Applied to the NavigationStack rather than chained onto the List below, alongside
        // .toolbar/.navigationTitle: combining all three on the same view triggers a SwiftUI/
        // AppKit bug on macOS ("Update ToolbarReader tried to update multiple times per frame",
        // logged every time this sheet's first presentation animates in) — harmless in practice,
        // but splitting .searchable onto the outer view avoids it entirely.
        .searchable(
            text: $searchQuery,
            prompt: IosLocalizationKt.localizedString(resource: MR.strings.shared.search_currencies_placeholder)
        )
        #if os(macOS)
        // macOS sizes a .sheet() to its content's ideal size rather than the parent window's
        // size (unlike iOS, which presents modally full-size); without an explicit frame here,
        // the List has no size to lay out rows in and renders empty.
        .frame(minWidth: 420, minHeight: 480)
        #endif
    }

    @ViewBuilder
    private func currencyRow(for info: CurrencyInfo, isSelected: Bool, onTap: (() -> Void)?) -> some View {
        let isPriced = pricedCurrencyCodes.contains(info.code)
        let content = HStack {
            VStack(alignment: .leading) {
                Text(currencyLabel(for: info))
                    .foregroundColor(.primary)
                if !isPriced {
                    Text(IosLocalizationKt.localizedFormattedString(
                        resource: MR.strings.shared.currency_not_priced,
                        args: [sourceName]
                    ))
                    .font(.caption)
                    .foregroundColor(.red)
                }
            }
            Spacer()
            if isSelected {
                Image(systemName: "checkmark")
                    .foregroundColor(.accentColor)
            }
        }
        if let onTap {
            Button(action: onTap) { content }
        } else {
            content
        }
    }

    private func currencyLabel(for info: CurrencyInfo) -> String {
        let text: String
        if info.code == localeCurrencyCode {
            text = IosLocalizationKt.localizedFormattedString(
                resource: MR.strings.shared.currency_option_label_local,
                args: [info.code, info.displayName]
            )
        } else {
            text = IosLocalizationKt.localizedFormattedString(
                resource: MR.strings.shared.currency_option_label,
                args: [info.code, info.displayName]
            )
        }
        if let flag = CurrencyFlagKt.currencyFlagEmoji(code: info.code) {
            return "\(flag) \(text)"
        }
        return text
    }
}
