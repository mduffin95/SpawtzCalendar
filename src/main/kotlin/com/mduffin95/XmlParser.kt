package com.mduffin95

import com.sun.tools.javac.jvm.ByteCodes.ret
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.time.Duration

@Serializable
@XmlSerialName("League")
private data class XmlLeague(
    @XmlSerialName("Id") val id: Int,
    @XmlSerialName("SeasonId") val seasonId: Int,
    @XmlSerialName("DivisionId") val divisionId: Int,
    @XmlSerialName("LeagueName") val leagueName: String,
    @XmlSerialName("SportId") val sportId: Int,
    @XmlSerialName("SportName") val sportName: String,
    @XmlSerialName("SeasonName") val seasonName: String,
    @XmlSerialName("DivisionName") val divisionName: String?,
    @XmlElement(true) val week: List<XmlWeek>
) {

    fun toLeague(): League {
        val fixtures = week.stream()
            .flatMap { it.fixture.stream() }
            .map { it.toFixture() }
            .toList()
        return League(
            id,
            leagueName,
            fixtures
        )
    }
}

@Serializable
@XmlSerialName("Week")
private data class XmlWeek(
    @XmlSerialName("Date") val date: String,
    @XmlElement(true) val fixture: List<XmlFixture>
)

@Serializable
@XmlSerialName("Fixture")
private data class XmlFixture(
    @XmlSerialName("DateTime") @Serializable(with = LocalDateTimeSerializer::class) val dateTime: LocalDateTime,
    @XmlSerialName("Id") val id: Int,
    @XmlSerialName("FixtureName") val fixtureName: String,
    @XmlSerialName("ResourceId") val resourceId: Int,
    @XmlSerialName("PlayingAreaName") val playingAreaName: String,
    @XmlSerialName("VenueId") val venueId: Int,
    @XmlSerialName("VenueName") val venueName: String,
//    @XmlSerialName("MapLink") val mapLink: String,
    @XmlSerialName("HomeTeamId") val homeTeamId: Int,
    @XmlSerialName("HomeTeam") val homeTeam: String,
//    @XmlSerialName("HomeTeamScore") val homeTeamScore: String?,
//    @XmlSerialName("HomeTeamForfeit") val homeTeamForfeit: Boolean,
    @XmlSerialName("AwayTeamId") val awayTeamId: Int,
    @XmlSerialName("AwayTeam") val awayTeam: String,
//    @XmlSerialName("AwayTeamScore") val awayTeamScore: String?,
//    @XmlSerialName("AwayTeamForfeit") val awayTeamForfeit: Boolean,
    @XmlSerialName("Duration") val duration: Int,
//    @XmlSerialName("Umpires") val umpires: String?,
//    @XmlSerialName("UmpireIds") val umpireIds: String?,
//    @XmlSerialName("CrossLeague") val crossLeague: Boolean,
    @XmlSerialName("DivisionName") val divisionName: String?,
//    @XmlSerialName("CalendarColour") val calendarColour: String,
//    @XmlSerialName("ResourceOrder") val resourceOrder: Int,
//    @XmlSerialName("FixtureTypeId") val fixtureTypeId: Int,
//    @XmlSerialName("UmpireTeamId") val umpireTeamId: Int,
//    @XmlSerialName("HasSoccerFeed") val hasSoccerFeed: Boolean,
//    @XmlSerialName("PhaseName") val phaseName: String?
) {
    fun toFixture(): Fixture {
        return Fixture(
            id,
            Team(homeTeamId, homeTeam),
            Team(awayTeamId, awayTeam),
            dateTime.toJavaLocalDateTime(),
            Duration.ofMinutes(duration.toLong())
        )
    }
}


@Serializable
@XmlSerialName("Leagues")
data class XmlLeagues(
    @XmlElement(true) val item: List<XmlLeagueItem>
) {
    fun getSeason(leagueId: Int): Int? {
        return item.firstOrNull { it.leagueId == leagueId }?.seasonId
    }

    fun toLeagueInfos(): List<LeagueInfo> {
        return item.map { it.toLeagueInfo() }
    }
}

@Serializable
@XmlSerialName("Item")
data class XmlLeagueItem(
    @XmlSerialName("LeagueId") val leagueId: Int,
    @XmlSerialName("LeagueName") val leagueName: String,
    @XmlSerialName("LeagueType") val leagueType: String,
    @XmlSerialName("DivisionId") val divisionId: Int,
    @XmlSerialName("DivisionName") val divisionName: String,
    @XmlSerialName("SeasonId") val seasonId: Int,
    @XmlSerialName("SeasonName") val seasonName: String,
    @XmlSerialName("SportId") val sportId: Int,
    @XmlSerialName("Sport") val sport: String,
    @XmlSerialName("Class") val classType: String,
    @XmlSerialName("Junior") val junior: Boolean,
    @XmlSerialName("PhaseName") val phaseName: String,
    @XmlSerialName("PhaseId") val phaseId: Int,
    @XmlSerialName("HasMultiplePhases") val hasMultiplePhases: Boolean
) {
    fun toLeagueInfo(): LeagueInfo {
        return LeagueInfo(leagueId, leagueName, divisionId, divisionName, seasonId, seasonName)
    }
}



private object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    private const val PATTERN = "dd/MM/yyyy HH:mm"

    @OptIn(FormatStringsInDatetimeFormats::class)
    private val formatter = LocalDateTime.Format {
        byUnicodePattern(PATTERN)
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val str = decoder.decodeString()
        return LocalDateTime.parse(str, formatter)
    }
}

class XmlParser {
    private val xml = XML() {
        // ignore unknown children
        unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList() }
    }

    fun parse(input: String): League {
        val xmlLeague = xml.decodeFromString(XmlLeague.serializer(), input)
        return xmlLeague.toLeague()
    }

    fun parseLeagues(input: String): XmlLeagues {
        val xmlLeagues = xml.decodeFromString(XmlLeagues.serializer(), input)
        return xmlLeagues
    }
}
