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

    @Test
    fun `should detect Brighton league`() {
        val fileContent = XmlParser::class.java.getResource("leagues.xml").readText()

        val league = XmlParser().parseLeagues(fileContent)

        val store = getFixtureStore().add(league.toLeagueInfos())

        val tuesdayLeagueId = store.findTuesdayLeagueId()
        val thursdayLeagueId = store.findThursdayLeagueId()

        assertEquals(tuesdayLeagueId, 1756)
        assertEquals(thursdayLeagueId, 1763)
    }

    @Test
    fun `should detect new Brighton league`() {
        val fileContent = XmlParser::class.java.getResource("leagues_22-09-2025.xml").readText()

        val league = XmlParser().parseLeagues(fileContent)

        val store = getFixtureStore().add(league.toLeagueInfos())

        val tuesdayLeagueId = store.findTuesdayLeagueId()
        val thursdayLeagueId = store.findThursdayLeagueId()

        assertEquals(tuesdayLeagueId, 1724)
        assertEquals(thursdayLeagueId, 1726)
    }
}
