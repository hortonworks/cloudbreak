package com.sequenceiq.cloudbreak.service.audit;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
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
    private ConverterUtil converterUtil;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    public AuditEventV4Response getAuditEvent(Long auditId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return getAuditEventByWorkspaceId(workspaceService.getDefaultWorkspaceForUser(user).getId(), auditId);
    }

    public AuditEventV4Response getAuditEventByWorkspaceId(Long workspaceId, Long auditId) {
        StructuredEventEntity event = Optional.ofNullable(structuredEventRepository.findByWorkspaceIdAndId(workspaceId, auditId))
                .orElseThrow(notFound("StructuredEvent", auditId));
        return converterUtil.convert(event, AuditEventV4Response.class);
    }

    public List<AuditEventV4Response> getAuditEventsByWorkspaceId(Long workspaceId, String resourceType, Long resourceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        List<AuditEventV4Response> auditEventV4Responses = getEventsForUserWithTypeAndResourceIdByWorkspace(workspace, resourceType, resourceId);
        auditEventV4Responses.sort(new AuditEventComparator().reversed());
        return auditEventV4Responses;
    }

    private List<AuditEventV4Response> getEventsForUserWithTypeAndResourceIdByWorkspace(Workspace workspace, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = structuredEventRepository.findByWorkspaceAndResourceTypeAndResourceId(workspace, resourceType, resourceId);
        return events != null ? converterUtil.convertAll(events, AuditEventV4Response.class) : Collections.emptyList();
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
