package com.eventbudget.repository;

import com.eventbudget.model.domain.Event;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganizerUserIdOrderByEventDateDesc(Long organizerId);
}
