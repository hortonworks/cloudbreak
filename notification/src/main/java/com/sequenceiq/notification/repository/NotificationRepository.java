package com.sequenceiq.notification.repository;

import java.util.List;
import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationType;

@Transactional(Transactional.TxType.REQUIRED)
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT DISTINCT e.resourceCrn FROM Notification e WHERE e.sent = false")
    List<String> findDistinctResourceCrnBySentFalse();

    @Query("SELECT e FROM Notification e WHERE e.resourceCrn = :resourceCrn AND e.type IN :types AND e.sent = false")
    List<Notification> findByResourceCrnAndTypeContainsTypesAndSentFalse(String resourceCrn, Set<NotificationType> types);

    @Query("SELECT n.resourceCrn FROM Notification n WHERE n.sent = false AND n.type = :type AND n.resourceCrn IN :crns")
    Set<String> collectExistingUnsentRecordByResourceCrnAndType(Set<String> crns, NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.sent = true, n.sentAt = :currentTimeMillis WHERE n.id IN :ids")
    void markAsSent(List<Long> ids, Long currentTimeMillis);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.sent = TRUE")
    int purgeSentNotifications();
}
