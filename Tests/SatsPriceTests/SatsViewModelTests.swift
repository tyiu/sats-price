// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  SatsViewModelTests.swift
//  SatsPriceTests
//
//  Created by Terry Yiu on 2/19/24.
//

import Foundation
import XCTest
@testable import SatsPrice

final class SatsViewModelTests: XCTestCase {

    let currency = Locale.Currency("USD")

    func testSatsViewModel() {
        let satsViewModel = SatsViewModel()
        satsViewModel.btcToCurrencyString(for: currency).wrappedValue = "54321"

        // Test BTC updates.
        satsViewModel.btcString = "1"
#if !SKIP
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "1"))
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "100000000"))
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal(string: "54321"))
#else
        XCTAssertEqual(satsViewModel.btc, Decimal("1"))
        XCTAssertEqual(satsViewModel.sats, Decimal("100000000"))
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal("54321"))
#endif
        XCTAssertEqual(satsViewModel.btcString, "1")
        XCTAssertEqual(satsViewModel.satsString, "100000000")
        XCTAssertEqual(satsViewModel.currencyValueString(for: currency).wrappedValue, "54321")

        // Test Sats updates.
        satsViewModel.satsString = "200000000"
#if !SKIP
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "2"))
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "200000000"))
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal(string: "108642"))
#else
        XCTAssertEqual(satsViewModel.btc, Decimal("2"))
        XCTAssertEqual(satsViewModel.sats, Decimal("200000000"))
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal("108642"))
#endif
        XCTAssertEqual(satsViewModel.btcString, "2")
        XCTAssertEqual(satsViewModel.satsString, "200000000")
        XCTAssertEqual(satsViewModel.currencyValueString(for: currency).wrappedValue, "108642")

        // Test currency value updates.
        satsViewModel.currencyValueString(for: currency).wrappedValue = "162963"
#if !SKIP
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "3"))
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "300000000"))
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal(string: "162963"))
#else
        XCTAssertEqual(satsViewModel.btc, Decimal("3"))
        XCTAssertEqual(satsViewModel.sats, Decimal("300000000"))
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal("162963"))
#endif
        XCTAssertEqual(satsViewModel.btcString, "3")
        XCTAssertEqual(satsViewModel.satsString, "300000000")
        XCTAssertEqual(satsViewModel.currencyValueString(for: currency).wrappedValue, "162963")

        // Test fractional amounts.
        // Precision between platforms on this calculation is different so we have different assertions for each.
        satsViewModel.currencyValueString(for: currency).wrappedValue = "1"
#if !SKIP
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "0.00001840908672520756245282671526665562"))
        XCTAssertEqual(satsViewModel.btcString, "0.00001840908672520756245282671526665562")
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "1840.908672520756245282671526665562"))
        XCTAssertEqual(satsViewModel.satsString, "1840.908672520756245282671526665562")
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal(string: "1"))
#else
        XCTAssertEqual(satsViewModel.btc, Decimal("0.00001840908672520756"))
        XCTAssertEqual(satsViewModel.btcString, "0.00001840908672520756")
        XCTAssertEqual(satsViewModel.sats, Decimal("1840.908672520756"))
        XCTAssertEqual(satsViewModel.satsString, "1840.908672520756")
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal("1"))
#endif
        XCTAssertEqual(satsViewModel.currencyValueString(for: currency).wrappedValue, "1")

        // Test large amounts that exceed the cap of 21M BTC.
        // Precision between platforms on this calculation is different so we have different assertions for each.
        satsViewModel.currencyValueString(for: currency).wrappedValue = "11407419999999"
#if !SKIP
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "210000184.09084884298889932070469983984"))
        XCTAssertEqual(satsViewModel.btcString, "210000184.09084884298889932070469983984")
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "21000018409084884.298889932070469983984"))
        XCTAssertEqual(satsViewModel.satsString, "21000018409084884.298889932070469983984")
        XCTAssertEqual(satsViewModel.currencyValue(for: currency), Decimal(string: "11407419999999"))
#else
        XCTAssertEqual(satsViewModel.btc, Decimal("210000184.0908488429888993207"))
        XCTAssertEqual(satsViewModel.btcString, "210000184.0908488429888993207")
        XCTAssertEqual(satsViewModel.sats, Decimal("21000018409084884.29888993207"))
        XCTAssertEqual(satsViewModel.satsString, "21000018409084884.29888993207")
        XCTAssertEqual(satsViewModel.currencyValue, Decimal("11407419999999"))
#endif
        XCTAssertEqual(satsViewModel.currencyValueString(for: currency).wrappedValue, "11407419999999")
    }

}
