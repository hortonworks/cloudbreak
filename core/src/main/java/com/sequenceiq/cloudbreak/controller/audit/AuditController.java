package com.sequenceiq.cloudbreak.controller.audit;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.AuditEndpoint;
import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
@Transactional(TxType.NEVER)
public class AuditController implements AuditEndpoint {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private AuditEventService auditEventService;

    @Override
    public AuditEvent getAuditEvent(Long auditId) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        return auditEventService.getAuditEvent(identityUser.getUserId(), auditId);
    }

    @Override
    public List<AuditEvent> getAuditEvents(String resourceType, Long resourceId) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        return auditEventService.getAuditEvents(identityUser.getUserId(), resourceType, resourceId);
    }

    @Override
    public Response getAuditEventsZip(String resourceType, Long resourceId) {
        List<AuditEvent> auditEvents = getAuditEvents(resourceType, resourceId);
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(auditEvents).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        String fileName = String.format("audit-%s-%d.zip", resourceType, resourceId);
        return Response.ok(streamingOutput).header("content-disposition", String.format("attachment; filename = %s", fileName)).build();
    }
}
