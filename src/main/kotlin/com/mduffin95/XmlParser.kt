package com.mduffin95

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
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

@Serializable
@XmlSerialName("League")
data class League(
    @XmlSerialName("Id") val id: Int,
    @XmlSerialName("SeasonId") val seasonId: Int,
    @XmlSerialName("DivisionId") val divisionId: Int,
    @XmlSerialName("LeagueName") val leagueName: String,
    @XmlSerialName("SportId") val sportId: Int,
    @XmlSerialName("SportName") val sportName: String,
    @XmlSerialName("SeasonName") val seasonName: String,
    @XmlSerialName("DivisionName") val divisionName: String?,
    @XmlElement(true) val week: List<Week>
)

@Serializable
@XmlSerialName("Week")
data class Week(
    @XmlSerialName("Date") val date: String,
    @XmlElement(true) val fixture: List<Fixture>
)

@Serializable
@XmlSerialName("Fixture")
data class Fixture(
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
)


object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

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
        return xml.decodeFromString(League.serializer(), input)
    }
}
