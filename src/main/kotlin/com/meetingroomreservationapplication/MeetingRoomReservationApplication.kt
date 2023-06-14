package com.meetingroomreservationapplication

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class MeetingRoomReservationApplication

fun main(args: Array<String>) {
    runApplication<MeetingRoomReservationApplication>(*args)
}
