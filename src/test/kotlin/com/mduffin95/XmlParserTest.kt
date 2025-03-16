package com.mduffin95

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class XmlParserTest {

    @Test
    fun shouldParse() {
        val fileContent = XmlParser::class.java.getResource("fixtures.xml").readText()

        val league = XmlParser().parse(fileContent)
        val store = InMemoryStore().add(league)

        val teams = store.getTeams()
        assertEquals(4, teams.size)

        val fixtures = store.getFixtures(9269) // Tagliatelle
        assertEquals(3, fixtures.size)
    }


    @Test
    fun shouldParseLeagues() {
        val fileContent = XmlParser::class.java.getResource("leagues.xml").readText()

        val league = XmlParser().parseLeagues(fileContent)

        assertEquals(17, league.item.size)

        val season = league.getSeason(1763)

        assertEquals(90, season)
    }
}
