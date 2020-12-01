package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public abstract class AuditGrpcServiceAssertion<T extends CloudbreakTestDto, C extends MicroserviceClient> {

    @Inject
    private AuditClient auditClient;

    public T create(TestContext testContext, T testDto, C client) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, getCreateEventName());
        return testDto;
    }

    public T start(TestContext testContext, T testDto, C client) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, getStartEventName());
        return testDto;
    }

    public T stop(TestContext testContext, T testDto, C client) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, getStopEventName());
        return testDto;
    }

    public T delete(TestContext testContext, T testDto, C client) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, getDeleteEventName());
        return testDto;
    }

    public T modify(TestContext testContext, T testDto, C client) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, getModifyEventName());
        return testDto;
    }

    protected void validateEventList(List<AuditProto.CdpAuditEvent> cdpAuditEvents, T testDto, String eventName) {
        if (shouldCheckRestEvents()) {
            List<AuditProto.CdpAuditEvent> restEvents = filterRestEvents(cdpAuditEvents, testDto, eventName);
            checkRestEvents(restEvents, eventName);
        }
        if (shouldCheckFlowEvents()) {
            List<AuditProto.CdpAuditEvent> flowEvents = filterFlowEvents(cdpAuditEvents, testDto, eventName);
            checkFlowEvents(flowEvents, testDto, eventName);
        }
    }

    protected AuditClient getAuditClient() {
        return auditClient;
    }

    private void checkFlowEvents(List<AuditProto.CdpAuditEvent> flowEvents, T testDto, String eventName) {
        if (flowEvents.isEmpty() || (flowEvents.size() >= 2 && flowEvents.size() % 2 != 0)) {
            throw new TestFailException(eventName + " flow audit log must contains minimum 2 items but has " + flowEvents.size());
        }
        if (flowEvents.stream().noneMatch(e -> "INIT_STATE".equals(getFlowState(e)))) {
            throw new TestFailException(eventName + " flow audit log must contains INIT_STATE");
        }
        if (flowEvents.stream().noneMatch(e -> "FINAL_STATE".equals(getFlowState(e)))) {
            throw new TestFailException(eventName + " flow audit log must contains FINAL_STATE");
        }
        if (!flowEvents.stream().allMatch(e -> testDto.getCrn().equals(getCrn(e)))) {
            throw new TestFailException(eventName + " flow audit log must match with all crns");
        }
        if (flowEvents.stream().allMatch(e -> StringUtils.isEmpty(getFlowId(e)))) {
            throw new TestFailException("flow id cannot be null or empty for " + eventName);
        }
        if (flowEvents.stream().allMatch(e -> StringUtils.isEmpty(getUserCrn(e)))) {
            throw new TestFailException("User crn cannot be null or empty for " + eventName);
        }
        if (flowEvents.stream().allMatch(e -> StringUtils.isEmpty(getEnvironmentCrn(e)))) {
            throw new TestFailException("Environment crn cannot be null or empty for " + eventName);
        }
    }

    private String getFlowState(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getValue("flowState");
    }

    private String getCrn(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getValue("clusterCrn");
    }

    private String getFlowId(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getValue("flowId");
    }

    private String getUserCrn(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getValue("userCrn");
    }

    private String getEnvironmentCrn(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getValue("environmentCrn");
    }

    private void checkRestEvents(List<AuditProto.CdpAuditEvent> restEvents, String eventName) {
        if (restEvents.isEmpty()) {
            throw new TestFailException(eventName + " rest audit log must contain only 1 item but has " + restEvents.size());
        }
    }

    private List<AuditProto.CdpAuditEvent> filterFlowEvents(List<AuditProto.CdpAuditEvent> cdpAuditEvents, T testDto, String eventName) {
        return cdpAuditEvents.stream()
                .filter(cdpAuditEvent -> cdpAuditEvent.getCdpServiceEvent().getResourceCrnList().contains(testDto.getCrn())
                        && cdpAuditEvent.getEventName().equals(eventName))
                .collect(Collectors.toList());
    }

    private List<AuditProto.CdpAuditEvent> filterRestEvents(List<AuditProto.CdpAuditEvent> cdpAuditEvents, T testDto, String eventName) {
        return cdpAuditEvents.stream().filter(cdpAuditEvent -> {
            String requestParameters = cdpAuditEvent.getApiRequestEvent().getRequestParameters();
            return (requestParameters.contains(testDto.getName()) || requestParameters.contains(testDto.getCrn()))
                    && cdpAuditEvent.getEventName().equals(eventName);
        }).collect(Collectors.toList());
    }

    protected String getStopEventName() {
        throw new UnsupportedOperationException("Cannot check stop event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected String getDeleteEventName() {
        throw new UnsupportedOperationException("Cannot check delete event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected String getStartEventName() {
        throw new UnsupportedOperationException("Cannot check start event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected String getCreateEventName() {
        throw new UnsupportedOperationException("Cannot check create event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected String getModifyEventName() {
        throw new UnsupportedOperationException("Cannot check modify event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected boolean shouldCheckRestEvents() {
        return true;
    }

    protected boolean shouldCheckFlowEvents() {
        return true;
    }

    protected abstract Crn.Service getService();
}
