package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class AuditGrpcServiceAssertion {

    @Inject
    private AuditClient auditClient;

    public DistroXTestDto distroxCreate(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(Crn.Service.DATAHUB).build());
        validateEventList(cdpAuditEvents, testDto, "CreateDatahubCluster");
        return testDto;
    }

    public DistroXTestDto distroxStart(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(Crn.Service.DATAHUB).build());
        validateEventList(cdpAuditEvents, testDto, "StartDatahubCluster");
        return testDto;
    }

    public DistroXTestDto distroxStop(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(Crn.Service.DATAHUB).build());
        validateEventList(cdpAuditEvents, testDto, "StopDatahubCluster");
        return testDto;
    }

    public DistroXTestDto distroxDelete(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(Crn.Service.DATAHUB).build());
        validateEventList(cdpAuditEvents, testDto, "DeleteDatahubCluster");
        return testDto;
    }

    private void validateEventList(List<AuditProto.CdpAuditEvent> cdpAuditEvents, DistroXTestDto testDto, String eventName) {
        List<AuditProto.CdpAuditEvent> restEvents = cdpAuditEvents.stream().filter(cdpAuditEvent -> {
            String requestParameters = cdpAuditEvent.getApiRequestEvent().getRequestParameters();
            String crn = testDto.getResponse().getCrn();
            return (requestParameters.contains(testDto.getName()) || requestParameters.contains(crn))
                    && cdpAuditEvent.getEventName().equals(eventName);
        }).collect(Collectors.toList());
        List<AuditProto.CdpAuditEvent> flowEvents = cdpAuditEvents.stream()
                .filter(cdpAuditEvent -> cdpAuditEvent.getCdpServiceEvent().getResourceCrnList().contains(testDto.getResponse().getCrn())
                        && cdpAuditEvent.getEventName().equals(eventName))
                .collect(Collectors.toList());

        if (restEvents.isEmpty()) {
            throw new TestFailException("Rest audit log must contains only 1 item but has " + restEvents.size());
        }
        if (flowEvents.isEmpty() || (flowEvents.size() >= 2 && flowEvents.size() % 2 != 0)) {
            throw new TestFailException("Flow audit log must contains minimum 2 items but has " + flowEvents.size());
        }
    }
}
