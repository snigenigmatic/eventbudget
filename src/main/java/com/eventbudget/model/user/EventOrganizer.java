package com.eventbudget.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("ORGANIZER")
@Getter
@Setter
public class EventOrganizer extends User {

    private String department;

    public EventOrganizer() {
        setRole(UserRole.ORGANIZER);
    }
}
