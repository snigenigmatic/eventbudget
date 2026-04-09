package com.eventbudget.repository;

import com.eventbudget.model.approval.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderBySentAtDesc(Long recipientId);
}
