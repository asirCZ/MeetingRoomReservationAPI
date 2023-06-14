package com.meetingroomreservationapplication

import java.time.LocalDate
import java.time.LocalTime

data class Reservation(
    var id: Int = 0,
    var room: String = "",
    var date: LocalDate = LocalDate.MIN,
    var startTime: LocalTime = LocalTime.MIN,
    var endTime: LocalTime = LocalTime.MIN,
    var name: String = "",
    var surname: String = ""
)


