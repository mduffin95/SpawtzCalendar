package com.mduffin95

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class XmlParserTest {

    @Test
    fun shouldParse() {
        val fileContent = XmlParser::class.java.getResource("ttr1.xml").readText()

        val model = XmlParser().parse(fileContent)

        assertEquals(5, model.week.size)
    }
}
