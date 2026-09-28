package com.moah.hackathon.ui.lesson

import com.moah.hackathon.feature.lesson.Course
import com.moah.hackathon.feature.lesson.Reservation
import com.moah.hackathon.feature.lesson.Slot
import com.moah.hackathon.feature.lesson.Venue

internal data class BookingDetails(val venue: Venue, val slot: Slot, val course: Course) {
    val badge: String get() = "예약 · ${venue.area} ${slot.start} · ${course.title}"
    val summary: String get() = "${venue.area} · 오늘 ${slot.label} · ${course.title}"
}

/** Do not invent confirmation text for a booking whose catalogue entries cannot be resolved. */
internal fun bookingDetails(booking: Reservation?, venues: List<Venue>): BookingDetails? {
    booking ?: return null
    return BookingDetails(booking.venue(venues) ?: return null, booking.slot(venues) ?: return null,
        booking.course(venues) ?: return null)
}

internal fun venueAreaLine(venue: Venue): String = listOfNotNull(venue.area,
    venue.distanceKm?.takeIf { it.isFinite() && it >= 0f }?.let { "${it.toString().removeSuffix(".0")} km" }).joinToString(" · ")
