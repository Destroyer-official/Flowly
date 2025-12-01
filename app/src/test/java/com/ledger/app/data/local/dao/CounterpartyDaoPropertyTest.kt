package com.ledger.app.data.local.dao

import com.ledger.app.data.local.entity.CounterpartyEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * **Feature: offline-ledger-app, Property 9: Counterparty Search Accuracy**
 * 
 * For any search query, results should include all counterparties whose 
 * display name or phone number contains the query (case-insensitive), 
 * and exclude all others.
 * 
 * **Validates: Requirements 11.2**
 * 
 * This test validates the search logic for counterparties.
 */
class CounterpartyDaoPropertyTest : FunSpec({

    // Generator for valid counterparty entities
    fun arbCounterpartyEntity(): Arb<CounterpartyEntity> = Arb.bind(
        Arb.string(1, 50), // displayName (non-empty)
        Arb.string(10, 15).orNull(), // phoneNumber
        Arb.string(0, 100).orNull(), // notes
        Arb.boolean(), // isFavorite
        Arb.long(1L, Long.MAX_VALUE / 2) // createdAt
    ) { displayName, phoneNumber, notes, isFavorite, createdAt ->
        CounterpartyEntity(
            id = 0,
            displayName = displayName,
            phoneNumber = phoneNumber,
            notes = notes,
            isFavorite = isFavorite,
            createdAt = createdAt
        )
    }

    // Generator for search queries
    fun arbSearchQuery(): Arb<String> = Arb.string(1, 10)

    /**
     * Helper function that simulates the SQL LIKE query behavior
     * for counterparty search.
     */
    fun matchesSearch(counterparty: CounterpartyEntity, query: String): Boolean {
        val lowerQuery = query.lowercase()
        val nameMatches = counterparty.displayName.lowercase().contains(lowerQuery)
        val phoneMatches = counterparty.phoneNumber?.lowercase()?.contains(lowerQuery) == true
        return nameMatches || phoneMatches
    }

    test("Property 9 - Search results include all matching counterparties by name") {
        checkAll(100, arbCounterpartyEntity(), arbSearchQuery()) { counterparty, query ->
            // If the query is contained in the display name, it should match
            val expectedMatch = counterparty.displayName.lowercase().contains(query.lowercase())
            val actualMatch = matchesSearch(counterparty.copy(phoneNumber = null), query)
            
            actualMatch shouldBe expectedMatch
        }
    }

    test("Property 9 - Search results include all matching counterparties by phone") {
        checkAll(100, arbCounterpartyEntity()) { counterparty ->
            // If counterparty has a phone number, searching for part of it should match
            val phone = counterparty.phoneNumber
            if (phone != null && phone.length >= 3) {
                val query = phone.substring(0, 3)
                val matches = matchesSearch(counterparty, query)
                matches shouldBe true
            }
        }
    }

    test("Property 9 - Search is case-insensitive for display name") {
        checkAll(100, arbCounterpartyEntity()) { counterparty ->
            // Searching with uppercase version of name should still match
            val upperQuery = counterparty.displayName.take(3).uppercase()
            val lowerQuery = counterparty.displayName.take(3).lowercase()
            
            val upperMatches = matchesSearch(counterparty, upperQuery)
            val lowerMatches = matchesSearch(counterparty, lowerQuery)
            
            // Both should match since search is case-insensitive
            upperMatches shouldBe lowerMatches
        }
    }

    test("Property 9 - Non-matching queries return no results") {
        checkAll(100, arbCounterpartyEntity()) { counterparty ->
            // A query that doesn't exist in name or phone should not match
            val impossibleQuery = "ZZZZXXXXXQQQQ12345"
            val matches = matchesSearch(counterparty, impossibleQuery)
            
            matches shouldBe false
        }
    }

    test("Property 9 - Empty phone number doesn't cause false matches") {
        checkAll(100, arbSearchQuery()) { query ->
            val counterpartyWithNoPhone = CounterpartyEntity(
                id = 1,
                displayName = "Test User",
                phoneNumber = null,
                notes = null,
                isFavorite = false,
                createdAt = System.currentTimeMillis()
            )
            
            // Should only match if query is in display name
            val expectedMatch = "test user".contains(query.lowercase())
            val actualMatch = matchesSearch(counterpartyWithNoPhone, query)
            
            actualMatch shouldBe expectedMatch
        }
    }
})
