// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  SatsViewModelTests.swift
//  SatsPriceTests
//
//  Created by Terry Yiu on 2/19/24.
//

import XCTest
@testable import SatsPrice

final class SatsViewModelTests: XCTestCase {

    func testSatsViewModel() {
        let satsViewModel = SatsViewModel()
        satsViewModel.btcToUsdString = "54321"

        // Test BTC updates.
        satsViewModel.btcString = "1"
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "1"))
        XCTAssertEqual(satsViewModel.btcString, "1")
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "100000000"))
        XCTAssertEqual(satsViewModel.satsString, "100000000")
        XCTAssertEqual(satsViewModel.usd, Decimal(string: "54321"))
        XCTAssertEqual(satsViewModel.usdString, "54321")

        // Test Sats updates.
        satsViewModel.satsString = "200000000"
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "2"))
        XCTAssertEqual(satsViewModel.btcString, "2")
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "200000000"))
        XCTAssertEqual(satsViewModel.satsString, "200000000")
        XCTAssertEqual(satsViewModel.usd, Decimal(string: "108642"))
        XCTAssertEqual(satsViewModel.usdString, "108642")

        // Test USD updates.
        satsViewModel.usdString = "162963"
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "3"))
        XCTAssertEqual(satsViewModel.btcString, "3")
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "300000000"))
        XCTAssertEqual(satsViewModel.satsString, "300000000")
        XCTAssertEqual(satsViewModel.usd, Decimal(string: "162963"))
        XCTAssertEqual(satsViewModel.usdString, "162963")

        // Test fractional amounts.
        satsViewModel.usdString = "1"
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "0.00001840908672520756245282671526665562"))
        XCTAssertEqual(satsViewModel.btcString, "0.00001840908672520756245282671526665562")
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "1840.908672520756245282671526665562"))
        XCTAssertEqual(satsViewModel.satsString, "1840.908672520756245282671526665562")
        XCTAssertEqual(satsViewModel.usd, Decimal(string: "1"))
        XCTAssertEqual(satsViewModel.usdString, "1")

        // Test large amounts that exceed the cap of 21M BTC.
        satsViewModel.usdString = "11407419999999"
        XCTAssertEqual(satsViewModel.btc, Decimal(string: "210000184.09084884298889932070469983984"))
        XCTAssertEqual(satsViewModel.btcString, "210000184.09084884298889932070469983984")
        XCTAssertEqual(satsViewModel.sats, Decimal(string: "21000018409084884.298889932070469983984"))
        XCTAssertEqual(satsViewModel.satsString, "21000018409084884.298889932070469983984")
        XCTAssertEqual(satsViewModel.usd, Decimal(string: "11407419999999"))
        XCTAssertEqual(satsViewModel.usdString, "11407419999999")
    }

}
