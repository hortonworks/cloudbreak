package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface CloudbreakEventService {
    String DATAHUB_RESOURCE_TYPE = "datahub";

    String DATALAKE_RESOURCE_TYPE = "datalake";

    String ENVIRONMENT_RESOURCE_TYPE = "environment";

    String FREEIPA_RESOURCE_TYPE = "freeipa";

    String KERBEROS_RESOURCE_TYPE = "kerberos";

    String LDAP_RESOURCE_TYPE = "ldap";

    String CREDENTIAL_RESOURCE_TYPE = "credential";

    @Deprecated
    String LEGACY_RESOURCE_TYPE = "stacks";

    String REDBEAMS_RESOURCE_TYPE = "redbeams";

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent);

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String instanceGroupName, ResourceEvent resourceEvent,
            Collection<String> eventMessageArgs);

    List<StructuredNotificationEvent> cloudbreakEvents(Long workspaceId, Long since);

    List<StructuredNotificationEvent> cloudbreakLastEventsForStack(Long stackId, String stackType, int size);

    Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, String stackType, Pageable pageable);

    default Set<String> getTypes() {
        return Set.of(DATAHUB_RESOURCE_TYPE, DATALAKE_RESOURCE_TYPE, ENVIRONMENT_RESOURCE_TYPE, FREEIPA_RESOURCE_TYPE, KERBEROS_RESOURCE_TYPE,
                LDAP_RESOURCE_TYPE, CREDENTIAL_RESOURCE_TYPE, LEGACY_RESOURCE_TYPE);
    }

}
