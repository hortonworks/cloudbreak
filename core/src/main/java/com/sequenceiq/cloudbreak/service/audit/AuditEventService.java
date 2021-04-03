package com.sequenceiq.cloudbreak.service.audit;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.comparator.audit.AuditEventComparator;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.db.StructuredEventRepository;

@Service
public class AuditEventService extends AbstractWorkspaceAwareResourceService<StructuredEventEntity> {

    @Inject
    private ConversionService conversionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Value("${cb.audit.limit:10000}")
    private int auditLimit;

    public AuditEvent getAuditEvent(Long auditId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return getAuditEventByWorkspaceId(workspaceService.getDefaultWorkspaceForUser(user).getId(), auditId);
    }

    public AuditEvent getAuditEventByWorkspaceId(Long workspaceId, Long auditId) {
        StructuredEventEntity event = Optional.ofNullable(structuredEventRepository.findByWorkspaceIdAndId(workspaceId, auditId))
                .orElseThrow(notFound("StructuredEvent", auditId));
        return conversionService.convert(event, AuditEvent.class);
    }

    public List<AuditEvent> getAuditEventsForWorkspace(String resourceType, Long resourceId, Workspace workspace) {
        List<AuditEvent> auditEvents = getEventsForUserWithTypeAndResourceIdByWorkspace(workspace, resourceType, resourceId);
        auditEvents.sort(new AuditEventComparator().reversed());
        return auditEvents;
    }

    public List<AuditEvent> getAuditEventsByWorkspaceId(Long workspaceId, String resourceType, Long resourceId, User user) {
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        List<AuditEvent> auditEvents = getEventsForUserWithTypeAndResourceIdByWorkspace(workspace, resourceType, resourceId);
        auditEvents.sort(new AuditEventComparator().reversed());
        return auditEvents;
    }

    private List<AuditEvent> getEventsForUserWithTypeAndResourceIdByWorkspace(Workspace workspace, String resourceType, Long resourceId) {
        PageRequest pageable = PageRequest.of(0, auditLimit, Sort.by("timestamp").descending());
        List<StructuredEventEntity> events = structuredEventRepository.findByWorkspaceAndResourceTypeAndResourceId(
                workspace, resourceType, resourceId, pageable).get().collect(Collectors.toList());
        return events != null ? (List<AuditEvent>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(AuditEvent.class))) : Collections.emptyList();
    }

    @Override
    public WorkspaceResourceRepository<StructuredEventEntity, Long> repository() {
        return structuredEventRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.STRUCTURED_EVENT;
    }

    @Override
    protected void prepareDeletion(StructuredEventEntity resource) {

    }

    @Override
    protected void prepareCreation(StructuredEventEntity resource) {

    }
}
