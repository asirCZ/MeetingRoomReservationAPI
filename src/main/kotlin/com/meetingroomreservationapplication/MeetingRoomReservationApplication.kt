package com.meetingroomreservationapplication

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@SpringBootApplication
class MeetingRoomReservationApplication

fun main(args: Array<String>) {
    runApplication<MeetingRoomReservationApplication>(*args)
}

@Component
class ApplicationStartup : ApplicationListener<ApplicationReadyEvent> {
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val port = event.applicationContext.environment.getProperty("server.port")
        println("Server is running on port: $port")
    }
}