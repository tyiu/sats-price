import Shared
import SwiftUI

struct CurrencyPickerSheet: View {
    let currencies: [CurrencyInfo]
    let selectedCodes: [String]
    let localeCurrencyCode: String?
    let onToggle: (String) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(currencies, id: \.code) { info in
                Button {
                    onToggle(info.code)
                } label: {
                    HStack {
                        Text(currencyLabel(for: info))
                            .foregroundColor(.primary)
                        Spacer()
                        if selectedCodes.contains(info.code) {
                            Image(systemName: "checkmark")
                                .foregroundColor(.accentColor)
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
        #if os(macOS)
        // macOS sizes a .sheet() to its content's ideal size rather than the parent window's
        // size (unlike iOS, which presents modally full-size); without an explicit frame here,
        // the List has no size to lay out rows in and renders empty.
        .frame(minWidth: 420, minHeight: 480)
        #endif
    }

    private func currencyLabel(for info: CurrencyInfo) -> String {
        if info.code == localeCurrencyCode {
            return IosLocalizationKt.localizedFormattedString(
                resource: MR.strings.shared.currency_option_label_local,
                args: [info.code, info.displayName]
            )
        } else {
            return IosLocalizationKt.localizedFormattedString(
                resource: MR.strings.shared.currency_option_label,
                args: [info.code, info.displayName]
            )
        }
    }
}
