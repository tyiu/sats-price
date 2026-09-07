import Foundation
import Shared

@MainActor
final class ConverterViewModel: ObservableObject {
    @Published private(set) var state: IosConverterState?

    private let bridge = IosPriceViewModel()

    var sourceNames: [String] { bridge.sourceNames }

    init() {
        bridge.observeState { [weak self] newState in
            self?.state = newState
        }
    }

    func refresh() {
        bridge.refresh()
    }

    func onBtcAmountChanged(_ value: String) {
        bridge.onBtcAmountChanged(value: value)
    }

    func onSatsAmountChanged(_ value: String) {
        bridge.onSatsAmountChanged(value: value)
    }

    func onFiatAmountChanged(code: String, value: String) {
        bridge.onFiatAmountChanged(code: code, value: value)
    }

    func onFiatCurrencyToggled(_ code: String) {
        bridge.onFiatCurrencyToggled(code: code)
    }

    func onFiatCurrenciesReordered(_ newOrder: [String]) {
        bridge.onFiatCurrenciesReordered(newOrder: newOrder)
    }

    func onSourceSelected(_ name: String) {
        bridge.onSourceSelected(name: name)
    }

    func onManualRateChanged(_ value: String) {
        bridge.onManualRateChanged(value: value)
    }
}
