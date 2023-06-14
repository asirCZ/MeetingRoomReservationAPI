package com.meetingroomreservationapplication

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/reservations")
class ReservationController {
    private val reservations = mutableListOf<Reservation>()
    private var currentReservationId: Int = 0
    private val availableRooms = listOf("A", "B", "C", "D")
    private val objectMapper = ObjectMapper()
    private val reservationsFile = File("reservations.json")
    private val minReservationTime = LocalTime.of(8, 0)
    private val maxReservationTime = LocalTime.of(17, 0)

    init {
        loadReservations()
    }

    /**
     * Retrieves all reservations.
     */
    @GetMapping
    fun getAllReservations(): List<Reservation> {
        return reservations
    }

    /**
     * Creates a new reservation.
     *
     * @param room The room name.
     * @param date The reservation date.
     * @param startTime The start time of the reservation.
     * @param endTime The end time of the reservation.
     * @param name The name of the person making the reservation.
     * @param surname The surname of the person making the reservation.
     * @return ResponseEntity containing the created reservation if successful, or an error response if not.
     */
    @PostMapping
    fun createReservation(
        @RequestParam room: String,
        @RequestParam date: String,
        @RequestParam startTime: String,
        @RequestParam endTime: String,
        @RequestParam name: String,
        @RequestParam surname: String
    ): ResponseEntity<Any> {
        if (!availableRooms.contains(room)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid room: $room"))
        }

        val reservationStartTime = LocalTime.parse(startTime)
        val reservationEndTime = LocalTime.parse(endTime)
        val reservationDate = LocalDate.parse(date)

        if (!isReservationTimeValid(reservationStartTime, reservationEndTime)) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "Invalid reservation time. The reservation time should be between $minReservationTime and $maxReservationTime.")
            )
        }

        if (!reservationStartTime.isRoomAvailable(room, reservationDate, reservationEndTime)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf("error" to "The room is already reserved at the given time.")
            )
        }

        val reservation = Reservation(
            getNextReservationId(),
            room,
            reservationDate,
            reservationStartTime,
            reservationEndTime,
            name,
            surname
        )
        reservations.add(reservation)
        saveReservations()
        return ResponseEntity.ok(reservation)
    }

    /**
     * Updates an existing reservation.
     *
     * @param id The ID of the reservation to update.
     * @param room The room name.
     * @param date The reservation date.
     * @param startTime The start time of the reservation.
     * @param endTime The end time of the reservation.
     * @param name The name of the person making the reservation.
     * @param surname The surname of the person making the reservation.
     * @return ResponseEntity containing the updated reservation if successful, or an error response if not.
     */
    @PutMapping("/{id}")
    fun updateReservation(
        @PathVariable id: String,
        @RequestParam room: String,
        @RequestParam date: String,
        @RequestParam startTime: String,
        @RequestParam endTime: String,
        @RequestParam name: String,
        @RequestParam surname: String
    ): ResponseEntity<Any> {
        val reservationId = id.toIntOrNull()
            ?: return ResponseEntity.badRequest().body(
                mapOf("error" to "Invalid reservation id: $id")
            )

        val reservation = reservations.find { it.id == reservationId }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to "Reservation with id $reservationId not found.")
            )

        try {
            val reservationStartTime = LocalTime.parse(startTime)
            val reservationEndTime = LocalTime.parse(endTime)
            val reservationDate = LocalDate.parse(date)

            if (!isReservationTimeValid(reservationStartTime, reservationEndTime)) {
                return ResponseEntity.badRequest().body(
                    mapOf("error" to "Invalid reservation time. The reservation time should be between $minReservationTime and $maxReservationTime.")
                )
            }

            if (!reservationStartTime.isRoomAvailable(room, reservationDate, reservationEndTime, reservationId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    mapOf("error" to "The room is already reserved at the given time.")
                )
            }

            reservation.room = room
            reservation.date = reservationDate
            reservation.startTime = reservationStartTime
            reservation.endTime = reservationEndTime
            reservation.name = name
            reservation.surname = surname
            saveReservations()
            return ResponseEntity.ok(reservation)
        } catch (e: DateTimeParseException) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "Invalid date or time format.")
            )
        }
    }


    /**
     * Deletes a reservation by its ID.
     *
     * @param id The ID of the reservation to delete.
     * @return ResponseEntity with a success response if the reservation is deleted, or an error response if not.
     */
    @DeleteMapping("/{id}")
    fun deleteReservation(@PathVariable id: String): ResponseEntity<Any> {
        val reservationId = id.toIntOrNull()
            ?: // Return a bad request response for an invalid reservation id
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid reservation id: $id"))

        val removed = reservations.removeIf { it.id == reservationId }
        if (!removed) {
            // Return a not found response if the reservation is not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to "Reservation with id $reservationId not found.")
            )
        }

        saveReservations()

        // Return a success response with an empty body
        return ResponseEntity.noContent().build()
    }


    private fun isReservationTimeValid(startTime: LocalTime, endTime: LocalTime): Boolean {
        return startTime >= minReservationTime && endTime <= maxReservationTime && startTime < endTime
    }

    private fun LocalTime.isRoomAvailable(
        room: String,
        date: LocalDate,
        endTime: LocalTime,
        excludeId: Int = -1
    ): Boolean {
        return reservations.none {
            it.room == room &&
                    it.date == date &&
                    (it.id != excludeId || excludeId == -1) &&
                    (it.startTime < endTime && it.endTime > this)
        }
    }

    private fun getNextReservationId(): Int {
        currentReservationId++
        return currentReservationId
    }

    private fun saveReservations() {
        try {
            val formattedReservations = reservations.map {
                mapOf(
                    "id" to it.id,
                    "room" to it.room,
                    "date" to it.date.toString(),
                    "startTime" to it.startTime.toString(),
                    "endTime" to it.endTime.toString(),
                    "name" to it.name,
                    "surname" to it.surname
                )
            }
            val module = JavaTimeModule()
            objectMapper.registerModule(module)
            objectMapper.writeValue(reservationsFile, formattedReservations)
        } catch (e: Exception) {
            println("Failed to save reservations: ${e.message}")
        }
    }


    private fun loadReservations() {
        if (reservationsFile.exists()) {
            try {
                val module = JavaTimeModule()
                objectMapper.registerModule(module)
                reservations.clear()
                reservations.addAll(objectMapper.readValue(reservationsFile))
                currentReservationId = reservations.maxByOrNull { it.id }?.id ?: 0
            } catch (e: Exception) {
                println("Failed to load reservations: ${e.message}")
            }
        }
    }

}