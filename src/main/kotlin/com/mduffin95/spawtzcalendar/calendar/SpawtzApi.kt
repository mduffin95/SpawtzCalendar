package com.mduffin95.spawtzcalendar.calendar

import com.mduffin95.spawtzcalendar.model.League
import com.mduffin95.spawtzcalendar.model.LeagueId
import com.mduffin95.spawtzcalendar.model.SeasonId
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request

fun getLeague(leagueId: LeagueId, seasonId: SeasonId): League {
    val input = getInput(leagueId, seasonId)
    return XmlParser().parse(input)
}

// TODO: Retrieve standings using the following format
// https://trytagrugby.spawtz.com/External/Fixtures/Feed.aspx?Type=Standings&SportId=0&VenueId=0&LeagueId=2156&SeasonId=96&DivisionId=8319

fun getLeagues(): String {
    val client = JavaHttpClient()

    val request = Request.Companion(Method.GET, "https://trytagrugby.spawtz.com/External/Fixtures/Feed.aspx")
        .query("Type", "Leagues")

    val response = client(request)

    val input = response.bodyString()
    return input
}

fun getInput(leagueId: LeagueId, seasonId: SeasonId): String {
    val client = JavaHttpClient()

    // Tuesday =
    val request = Request(Method.GET, "https://trytagrugby.spawtz.com/External/Fixtures/Feed.aspx")
        .query("Type", "Fixtures")
        .query("LeagueId", leagueId.toString())
        .query("SeasonId", seasonId.toString())

    val response = client(request)

    val input = response.bodyString()
    return input
}

fun getLatestSeasonForLeague(leagueId: LeagueId): League? {
    for (seasonId in 90..200) {
        val input = getInput(leagueId, seasonId)
        val league = XmlParser().parse(input)
        if (league.fixtures.isNotEmpty()) {
            return league
        }
    }

    return null
}