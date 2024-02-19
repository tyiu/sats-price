//
//  SatsViewModelTests.swift
//  SatsPriceTests
//
//  Created by Terry Yiu on 2/19/24.
//

import XCTest
import BigDecimal
@testable import SatsPrice

final class SatsViewModelTests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testSatsViewModel() {
        let satsViewModel = SatsViewModel()
        satsViewModel.btcToUsdString = "54321"

        // Test BTC updates.
        satsViewModel.btcString = "1"
        XCTAssertEqual(satsViewModel.btc, BigDecimal("1"))
        XCTAssertEqual(satsViewModel.btcString, "1")
        XCTAssertEqual(satsViewModel.sats, BigDecimal("100000000"))
        XCTAssertEqual(satsViewModel.satsString, "100000000")
        XCTAssertEqual(satsViewModel.usd, BigDecimal("54321"))
        XCTAssertEqual(satsViewModel.usdString, "54321")

        // Test Sats updates.
        satsViewModel.satsString = "200000000"
        XCTAssertEqual(satsViewModel.btc, BigDecimal("2"))
        XCTAssertEqual(satsViewModel.btcString, "2")
        XCTAssertEqual(satsViewModel.sats, BigDecimal("200000000"))
        XCTAssertEqual(satsViewModel.satsString, "200000000")
        XCTAssertEqual(satsViewModel.usd, BigDecimal("108642"))
        XCTAssertEqual(satsViewModel.usdString, "108642")

        // Test USD updates.
        satsViewModel.usdString = "162963"
        XCTAssertEqual(satsViewModel.btc, BigDecimal("3"))
        XCTAssertEqual(satsViewModel.btcString, "3")
        XCTAssertEqual(satsViewModel.sats, BigDecimal("300000000"))
        XCTAssertEqual(satsViewModel.satsString, "300000000")
        XCTAssertEqual(satsViewModel.usd, BigDecimal("162963"))
        XCTAssertEqual(satsViewModel.usdString, "162963")

        // Test fractional amounts.
        satsViewModel.usdString = "1"
        XCTAssertEqual(satsViewModel.btc, BigDecimal("0.000018409086725207562452"))
        XCTAssertEqual(satsViewModel.btcString, "0.000018409086725207562452")
        XCTAssertEqual(satsViewModel.sats, BigDecimal("1840.9086725207562452"))
        XCTAssertEqual(satsViewModel.satsString, "1840.9086725207562452")
        XCTAssertEqual(satsViewModel.usd, BigDecimal("1"))
        XCTAssertEqual(satsViewModel.usdString, "1")

        // Test large amounts that exceed the cap of 21M BTC.
        satsViewModel.usdString = "11407419999999"
        XCTAssertEqual(satsViewModel.btc, BigDecimal("210000184.09084884298"))
        XCTAssertEqual(satsViewModel.btcString, "210000184.09084884298")
        XCTAssertEqual(satsViewModel.sats, BigDecimal("21000018409084884.298"))
        XCTAssertEqual(satsViewModel.satsString, "21000018409084884.298")
        XCTAssertEqual(satsViewModel.usd, BigDecimal("11407419999999"))
        XCTAssertEqual(satsViewModel.usdString, "11407419999999")
    }

}
