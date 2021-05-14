package com.sequenceiq.distrox.v1.distrox.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Response;
import com.sequenceiq.distrox.v1.distrox.facade.DistroxEventsFacade;

@Service
public class DistroXEventService {

    @Inject
    private DistroxEventsFacade distroxEventsFacade;

    @Inject
    private LegacyStructuredEventService legacyStructuredEventService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private StackViewService stackViewService;

    public List<DistroXEventV1Response> list(Long since) {
        return distroxEventsFacade.retrieveEvents(since);
    }

    public Page<DistroXEventV1Response> getEventsByStack(String crn, Pageable pageable) {
        return distroxEventsFacade.retrieveEventsByStack(crn, pageable);
    }

    public StructuredEventContainer structured(String crn) {
        return legacyStructuredEventService.getStructuredEventsForStack(getWorkloadStackName(crn), threadLocalService.getRequestedWorkspaceId());
    }

    public StreamingOutput download(String crn) {
        StructuredEventContainer events = legacyStructuredEventService.getStructuredEventsForStack(getWorkloadStackName(crn),
                threadLocalService.getRequestedWorkspaceId());
        StreamingOutput streamingOutput = output -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
                zipOutputStream.putNextEntry(new ZipEntry("struct-events.json"));
                zipOutputStream.write(JsonUtil.writeValueAsString(events).getBytes());
                zipOutputStream.closeEntry();
            }
        };
        return streamingOutput;
    }

    private String getWorkloadStackName(String crn) {
        StackView stackView = stackViewService.getByCrn(threadLocalService.getRequestedWorkspaceId(), crn);
        if (!WORKLOAD.equals(stackView.getType())) {
            throw new BadRequestException(String.format("Stack with crn %s is not a datahub cluster!", crn));
        }
        return stackView.getName();
    }
}
