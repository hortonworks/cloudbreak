package com.sequenceiq.cloudbreak.controller.audit;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public abstract class BaseAuditController {

    public Response getAuditEventsZipResponse(List<AuditEvent> auditEvents, String resourceType, Long resourceId) {
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
