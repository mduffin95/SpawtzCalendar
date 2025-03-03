package com.mduffin95

interface Store {

    fun getFixtures(teamId: TeamId): List<Fixture>

    fun getTeams(): List<Team>

}
