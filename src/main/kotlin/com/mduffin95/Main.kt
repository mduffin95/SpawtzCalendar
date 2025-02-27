package com.mduffin95

fun main() {
    val inputString = getInput();
    val calendar = parse(inputString)

    calendar.forEach {
        println(outputCalendar(it))
    }

    println(calendar)
}

