package com.sequenceiq.cloudbreak.service.events;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long entityId, String eventType, String eventMessage);

    void fireLdapEvent(LdapDetails ldapDetails, String eventType, String eventMessage, boolean notificateUserOnly);

    void fireRdsEvent(RdsDetails rdsDetails, String eventType, String eventMessage, boolean notificateUserOnly);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName);

    List<StructuredNotificationEvent> cloudbreakEvents(Workspace workspace, Long since);

    List<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId);
}
