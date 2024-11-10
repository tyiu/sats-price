// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  PriceFetcher.swift
//  SatsPrice
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation

protocol PriceFetcher {
    func convertBTC(toCurrency currency: Locale.Currency) async throws -> Decimal?
    func convertBTC(toCurrencies currencies: [Locale.Currency]) async throws -> [Locale.Currency: Decimal]
}
