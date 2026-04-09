package com.eventbudget.repository;

import com.eventbudget.model.user.EventOrganizer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOrganizerRepository extends JpaRepository<EventOrganizer, Long> {
}
