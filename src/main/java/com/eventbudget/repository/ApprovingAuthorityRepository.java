package com.eventbudget.repository;

import com.eventbudget.model.user.ApprovingAuthority;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovingAuthorityRepository extends JpaRepository<ApprovingAuthority, Long> {

    List<ApprovingAuthority> findAllByOrderByAuthorizationLimitAsc();
}
