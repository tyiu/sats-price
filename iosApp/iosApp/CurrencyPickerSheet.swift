import Shared
import SwiftUI

struct CurrencyPickerSheet: View {
    let currencies: [CurrencyInfo]
    let selectedCodes: [String]
    let localeCurrencyCode: String?
    let onToggle: (String) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
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
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(IosLocalizationKt.localizedString(resource: MR.strings.shared.done)) { dismiss() }
                }
            }
        }
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
