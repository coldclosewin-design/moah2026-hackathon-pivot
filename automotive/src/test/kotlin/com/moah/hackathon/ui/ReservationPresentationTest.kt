package com.moah.hackathon.ui

import com.moah.hackathon.data.SeedCatalog
import com.moah.hackathon.feature.lesson.Reservation
import com.moah.hackathon.ui.lesson.*
import org.junit.Assert.*
import org.junit.Test

class ReservationPresentationTest {
    private val venues = SeedCatalog.venues
    private val booking = Reservation("venue-seocho", "slot-14", SeedCatalog.COURSE_PARKING, 0L)

    @Test fun bookingUsesTheSelectedVenueSlotAndCourse() {
        val details = bookingDetails(booking, venues)!!
        assertEquals("예약 · 서초 14:00 · 주차 3종", details.badge)
        assertEquals("서초 · 오늘 14:00–15:00 · 주차 3종", details.summary)
        val changed = booking.copy(venueId = "venue-bundang", slotId = "slot-16", courseId = SeedCatalog.COURSE_ROAD_B)
        assertEquals("예약 · 분당 16:00 · 도로 B", bookingDetails(changed, venues)!!.badge)
    }

    @Test fun absentOrUnresolvableBookingsNeverInventConfirmation() {
        assertNull(bookingDetails(null, venues))
        assertNull(bookingDetails(booking, emptyList()))
        assertNull(bookingDetails(booking.copy(venueId = "removed"), venues))
        assertNull(bookingDetails(booking.copy(slotId = "removed"), venues))
        assertNull(bookingDetails(booking.copy(courseId = "removed"), venues))
    }

    @Test fun courseIdsCannotBeResolvedFromAnotherVenue() {
        assertNull(bookingDetails(booking.copy(venueId = "venue-gangnam", courseId = SeedCatalog.COURSE_ROAD_B), venues))
    }

    @Test fun distanceFormattingKeepsFractionsAndOmitsUnknownValues() {
        val venue = venues.first()
        assertEquals("서초 · 3 km", venueAreaLine(venue))
        assertEquals("서초 · 3.5 km", venueAreaLine(venue.copy(distanceKm = 3.5f)))
        listOf(null, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { distance ->
            assertEquals("서초", venueAreaLine(venue.copy(distanceKm = distance)))
        }
    }
}
