package com.sequenceiq.cloudbreak.service.audit;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.comparator.audit.AuditEventComparator;
import com.sequenceiq.cloudbreak.converter.v4.audit.StructuredEventEntityToAuditEventV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.db.LegacyStructuredEventDBService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class AuditEventService extends AbstractWorkspaceAwareResourceService<StructuredEventEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventService.class);

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private LegacyStructuredEventDBService legacyStructuredEventDBService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Inject
    private StructuredEventEntityToAuditEventV4ResponseConverter structuredEventEntityToAuditEventV4ResponseConverter;

    public AuditEventV4Response getAuditEvent(Long auditId) {
        User user = userService.getOrCreate(legacyRestRequestThreadLocalService.getCloudbreakUser());
        return getAuditEventByWorkspaceId(workspaceService.getDefaultWorkspaceForUser(user).getId(), auditId);
    }

    public AuditEventV4Response getAuditEventByWorkspaceId(Long workspaceId, Long auditId) {
        StructuredEventEntity event = Optional.ofNullable(legacyStructuredEventDBService.findByWorkspaceIdAndId(workspaceId, auditId))
                .orElseThrow(notFound("StructuredEvent", auditId));
        return structuredEventEntityToAuditEventV4ResponseConverter.convert(event);
    }

    public List<AuditEventV4Response> getAuditEventsByWorkspaceId(Long workspaceId, String resourceType, Long resourceId, String resourceCrn) {
        User user = userService.getOrCreate(legacyRestRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        List<AuditEventV4Response> auditEventV4Responses = getEventsForUserWithTypeAndResourceIdByWorkspace(workspace, resourceType, resourceId, resourceCrn);
        auditEventV4Responses.sort(new AuditEventComparator().reversed());
        return auditEventV4Responses;
    }

    @VisibleForTesting
    List<AuditEventV4Response> getEventsForUserWithTypeAndResourceIdByWorkspace(Workspace workspace, String resourceType, Long resourceId, String resourceCrn) {
        List<StructuredEventEntity> events;
        if (!Strings.isNullOrEmpty(resourceCrn)) {
            events = legacyStructuredEventDBService.findByWorkspaceAndResourceTypeAndResourceCrn(workspace, resourceCrn);
        } else {
            LOGGER.info("Try to fetch audit events by resource type and id: {}: {}", resourceType, resourceId);
            events = legacyStructuredEventDBService.findByWorkspaceAndResourceTypeAndResourceId(workspace, resourceType, resourceId);
        }
        return events != null ? events.stream()
                        .map(e -> structuredEventEntityToAuditEventV4ResponseConverter.convert(e))
                        .collect(Collectors.toList()) : Collections.emptyList();
    }

    @Override
    public WorkspaceResourceRepository<StructuredEventEntity, Long> repository() {
        return legacyStructuredEventDBService.repository();
    }

    @Override
    protected void prepareDeletion(StructuredEventEntity resource) {

    }

    @Override
    protected void prepareCreation(StructuredEventEntity resource) {

    }

}
