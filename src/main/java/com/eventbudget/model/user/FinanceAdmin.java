package com.eventbudget.model.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("FINANCE_ADMIN")
@Getter
@Setter
public class FinanceAdmin extends User {

    public FinanceAdmin() {
        setRole(UserRole.FINANCE_ADMIN);
    }
}
