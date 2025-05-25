package com.mduffin95.spawtzcalendar

import com.mduffin95.spawtzcalendar.calendar.XmlParser
import com.mduffin95.spawtzcalendar.calendar.getFixtureStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class XmlParserTest {

    @Test
    fun `should parse teams and their fixtures`() {
        val fileContent = XmlParser::class.java.getResource("fixtures.xml").readText()

        val league = XmlParser().parse(fileContent)
        val store = getFixtureStore().add(league)

        val teams = store.getTeams()
        assertEquals(4, teams.size)

        val fixtures = store.getFixtures(9269) // Tagliatelle
        assertEquals(3, fixtures.size)
    }


    @Test
    fun `should parse leagues`() {
        val fileContent = XmlParser::class.java.getResource("leagues.xml").readText()

        val league = XmlParser().parseLeagues(fileContent)

        assertEquals(17, league.item.size)

        val season = league.getSeason(1763)

        assertEquals(90, season)
    }
}
