package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long entityId, String eventType, String eventMessage);

    void fireLdapEvent(LdapDetails ldapDetails, String eventType, String eventMessage, boolean notificateUserOnly);

    void fireRdsEvent(RdsDetails rdsDetails, String eventType, String eventMessage, boolean notificateUserOnly);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName);

    List<StructuredNotificationEvent> cloudbreakEvents(Long workspaceId, Long since);

    List<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId);

    Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, Pageable pageable);
}
