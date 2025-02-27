package com.mduffin95

interface Store {

    fun getFixtures(teamId: TeamId): List<Fixture>

    fun getTeams(leagueId: LeagueId): List<Team>

}
