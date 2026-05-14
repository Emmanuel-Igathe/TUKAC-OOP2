package com.tukac.controller;

import com.tukac.dto.ApiResponse;
import com.tukac.model.Event;
import com.tukac.model.EventRegistration;
import com.tukac.repository.EventRegistrationRepository;
import com.tukac.repository.EventRepository;
import com.tukac.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for managing club events.
 * Handles event scheduling, search, and member registrations (RSVPs).
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository registrationRepository;
    @Autowired private JwtService jwtService;
    @Autowired private com.tukac.service.ActivityLogService activityLogService;

    /**
     * BROWSE: Fetches a list of events.
     * Logic: If a search query is provided, it filters by title/description.
     * Data Enrichment: For each event, it calculates the total RSVPs and 
     * checks if the currently logged-in user has already registered.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEvents(
            @RequestParam(required = false) String search,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = null;
        // Manual JWT extraction to identify the user for RSVP status check
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try { userId = jwtService.extractUserId(authHeader.substring(7)); } catch (Exception ignored) {}
        }

        final Long finalUserId = userId;
        List<Event> events;
        if (search != null && !search.isEmpty()) {
            events = eventRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
        } else {
            events = eventRepository.findAllByOrderByEventDateAsc();
        }

        List<Map<String, Object>> result = events.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", e.getId());
            map.put("title", e.getTitle());
            map.put("description", e.getDescription());
            map.put("eventDate", e.getEventDate());
            map.put("eventTime", e.getEventTime());
            map.put("location", e.getLocation());
            map.put("capacity", e.getCapacity());
            // Aggregate data from registrations table
            map.put("rsvpCount", registrationRepository.countByEventId(e.getId()));
            map.put("isRsvped", finalUserId != null && registrationRepository.existsByUserIdAndEventId(finalUserId, e.getId()));
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * ADD: Creates a new event.
     * Access Control: Chairperson, Vice-Chairperson, and Secretary only.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','SECRETARY')")
    public ResponseEntity<ApiResponse<Event>> createEvent(@RequestBody Event event, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        event.setCreatedBy(userId);
        Event saved = eventRepository.save(event);
        activityLogService.log("CREATE_EVENT", "Created event: " + saved.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Event created", saved));
    }

    /**
     * UPDATE/EDIT: Modifies event details.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','SECRETARY')")
    public ResponseEntity<ApiResponse<Event>> updateEvent(@PathVariable Long id, @RequestBody Event updated) {
        Optional<Event> opt = eventRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Event event = opt.get();
        event.setTitle(updated.getTitle());
        event.setDescription(updated.getDescription());
        event.setEventDate(updated.getEventDate());
        event.setEventTime(updated.getEventTime());
        event.setLocation(updated.getLocation());
        event.setCapacity(updated.getCapacity());

        Event saved = eventRepository.save(event);
        activityLogService.log("UPDATE_EVENT", "Updated event: " + saved.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Event updated", saved));
    }

    /**
     * DELETE: Removes an event from the database.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','SECRETARY')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(id).get();
        eventRepository.deleteById(id);
        activityLogService.log("DELETE_EVENT", "Deleted event: " + event.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Event deleted", null));
    }

    /**
     * RSVP: Toggle registration for an event.
     * Business Logic: If already registered, it cancels (removes record). 
     * If not registered, it adds a new record to the registrations table.
     */
    @PostMapping("/{id}/rsvp")
    public ResponseEntity<ApiResponse<String>> rsvp(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (registrationRepository.existsByUserIdAndEventId(userId, id)) {
            // Cancel RSVP logic
            registrationRepository.findByUserIdAndEventId(userId, id)
                    .ifPresent(r -> registrationRepository.delete(r));
            activityLogService.log("RSVP_CANCEL", "Cancelled RSVP for event ID: " + id);
            return ResponseEntity.ok(ApiResponse.ok("RSVP cancelled", null));
        }

        // New RSVP registration
        registrationRepository.save(new EventRegistration(userId, id));
        activityLogService.log("RSVP_CONFIRM", "RSVP'd for event ID: " + id);
        return ResponseEntity.ok(ApiResponse.ok("RSVP confirmed!", null));
    }
}
