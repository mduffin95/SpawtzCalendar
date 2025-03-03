package com.mduffin95

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class XmlParserTest {

    @Test
    fun shouldParse() {
        val fileContent = XmlParser::class.java.getResource("ttr1.xml").readText()

        val league = XmlParser().parse(fileContent)
        val store = InMemoryStore().add(league)

        val teams = store.getTeams()
        assertEquals(4, teams.size)

        val fixtures = store.getFixtures(9269) // Tagliatelle
        assertEquals(3, fixtures.size)
    }
}
