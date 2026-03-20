package com.eventbudget.model.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.eventbudget.model.user.EventOrganizer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    private String venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private EventOrganizer organizer;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL)
    private Budget budget;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isEligibleForClosure() {
        return eventDate != null
                && !eventDate.isAfter(LocalDate.now())
                && budget != null
                && budget.getStatus() == BudgetStatus.APPROVED
                && budget.hasNoPendingClaims();
    }

    public boolean hasConcluded() {
        return eventDate != null && !eventDate.isAfter(LocalDate.now());
    }
}