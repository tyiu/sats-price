// This is free software: you can redistribute and/or modify it
// under the terms of the GNU General Public License 3.0
// as published by the Free Software Foundation https://fsf.org
//
//  SatsPriceModel.swift
//  sats-price
//
//  Created by Terry Yiu on 11/15/24.
//

import Foundation
import OSLog
import Observation
import SkipSQL

public struct SelectedCurrency: Identifiable {
    public var id: String {
        currencyCode
    }

    public var currencyCode: String
}

/// Notification posted by the model when selected currencies change.
extension Notification.Name {
    public static var selectedCurrenciesDidChange: Notification.Name {
        return Notification.Name("selectedCurrenciesChange")
    }
}

/// Payload of `selectedCurrenciesDidChange` notifications.
public struct SelectedCurrenciesChange {
    public let inserts: [SelectedCurrency]
    /// Nil set means all records were deleted.
    public let deletes: Set<String>?

    public init(inserts: [SelectedCurrency] = [], deletes: [String]? = []) {
        self.inserts = inserts
        self.deletes = deletes == nil ? nil : Set(deletes!)
    }
}

public actor SatsPriceModel {
    private let ctx: SQLContext
    private var schemaInitializationResult: Result<Void, Error>?

    public init(url: URL?) throws {
        ctx = try SQLContext(path: url?.path ?? ":memory:", flags: [.readWrite, .create], logLevel: .info, configuration: .platform)
    }

    public func selectedCurrencies() throws -> [SelectedCurrency] {
        do {
            try initializeSchema()
            let statement = try ctx.prepare(sql: "SELECT currencyCode FROM SelectedCurrency")
            defer {
                do {
                    try statement.close()
                } catch {
                    logger.warning("Failed to close statement: \(error)")
                }
            }

            var selectedCurrencies: [SelectedCurrency] = []

            while try statement.next() {
                let currencyCode = statement.string(at: 0) ?? ""
                selectedCurrencies.append(SelectedCurrency(currencyCode: currencyCode))
            }

            return selectedCurrencies
        } catch {
            logger.error("Failed to get selected currencies from DB. Error: \(error)")
            throw error
        }
    }

    @discardableResult
    public func insert(_ selectedCurrency: SelectedCurrency) throws -> [SelectedCurrency] {
        try initializeSchema()
        let statement = try ctx.prepare(sql: "INSERT INTO SelectedCurrency (currencyCode) VALUES (?)")
        defer {
            do {
                try statement.close()
            } catch {
                logger.warning("Failed to close statement: \(error)")
            }
        }

        var insertedItems: [SelectedCurrency] = []
        try ctx.transaction {
            statement.reset()
            let values = Self.bindingValues(for: selectedCurrency)
            try statement.update(parameters: values)

            insertedItems.append(selectedCurrency)
        }
        NotificationCenter.default.post(name: .selectedCurrenciesDidChange, object: SelectedCurrenciesChange(inserts: insertedItems))
        return insertedItems
    }

    private static func bindingValues(for selectedCurrency: SelectedCurrency) -> [SQLValue] {
        return [
            .text(selectedCurrency.currencyCode)
        ]
    }

    @discardableResult
    public func deleteSelectedCurrency(currencyCode: String) throws -> Int {
        try initializeSchema()
        try ctx.exec(sql: "DELETE FROM SelectedCurrency WHERE currencyCode = ?", parameters: [.text(currencyCode)])
        NotificationCenter.default.post(name: .selectedCurrenciesDidChange, object: SelectedCurrenciesChange(deletes: [currencyCode]))
        return Int(ctx.changes)
    }

    private func initializeSchema() throws {
        switch schemaInitializationResult {
        case .success:
            return
        case .failure(let failure):
            throw failure
        case nil:
            break
        }

        do {
            var currentVersion = try currentSchemaVersion()
            currentVersion = try migrateSchema(v: Int64(1), current: currentVersion, ddl: """
            CREATE TABLE SelectedCurrency (currencyCode TEXT PRIMARY KEY NOT NULL)
            """)
            // Future column additions, etc here...
            schemaInitializationResult = .success(())
        } catch {
            schemaInitializationResult = .failure(error)
            throw error
        }
    }

    private func currentSchemaVersion() throws -> Int64 {
        try ctx.exec(sql: "CREATE TABLE IF NOT EXISTS SchemaVersion (id INTEGER PRIMARY KEY, version INTEGER)")
        try ctx.exec(sql: "INSERT OR IGNORE INTO SchemaVersion (id, version) VALUES (0, 0)")
        return try ctx.query(sql: "SELECT version FROM SchemaVersion").first?.first?.integerValue ?? Int64(0)
    }

    private func migrateSchema(v version: Int64, current: Int64, ddl: String) throws -> Int64 {
        guard current < version else {
            return current
        }
        let startTime = Date.now
        try ctx.transaction {
            try ctx.exec(sql: ddl)
            try ctx.exec(sql: "UPDATE SchemaVersion SET version = ?", parameters: [.integer(version)])
        }
        logger.log("Updated database schema to \(version) in \(Date.now.timeIntervalSince1970 - startTime.timeIntervalSince1970)")
        return version
    }
}
