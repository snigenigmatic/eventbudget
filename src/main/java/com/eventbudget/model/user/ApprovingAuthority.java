package com.eventbudget.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("APPROVER")
@Getter
@Setter
public class ApprovingAuthority extends User {

    @Column(precision = 15, scale = 2)
    private BigDecimal authorizationLimit;

    private String designation;

    public ApprovingAuthority() {
        setRole(UserRole.APPROVER);
    }

    public boolean canApprove(BigDecimal amount) {
        if (authorizationLimit == null) return false;
        return authorizationLimit.compareTo(amount) >= 0;
    }
}
