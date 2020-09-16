package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
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
            checkFlowEvents(flowEvents, eventName);
        }
    }

    private void checkFlowEvents(List<AuditProto.CdpAuditEvent> flowEvents, String eventName) {
        if (flowEvents.isEmpty() || (flowEvents.size() >= 2 && flowEvents.size() % 2 != 0)) {
            throw new TestFailException(eventName + "flow audit log must contains minimum 2 items but has " + flowEvents.size());
        }
    }

    private void checkRestEvents(List<AuditProto.CdpAuditEvent> restEvents, String eventName) {
        if (restEvents.isEmpty()) {
            throw new TestFailException(eventName + " rest audit log must contains only 1 item but has " + restEvents.size());
        }
    }

    @NotNull
    private List<AuditProto.CdpAuditEvent> filterFlowEvents(List<AuditProto.CdpAuditEvent> cdpAuditEvents, T testDto, String eventName) {
        return cdpAuditEvents.stream()
                .filter(cdpAuditEvent -> cdpAuditEvent.getCdpServiceEvent().getResourceCrnList().contains(testDto.getCrn())
                        && cdpAuditEvent.getEventName().equals(eventName))
                .collect(Collectors.toList());
    }

    @NotNull
    private List<AuditProto.CdpAuditEvent> filterRestEvents(List<AuditProto.CdpAuditEvent> cdpAuditEvents, T testDto, String eventName) {
        return cdpAuditEvents.stream().filter(cdpAuditEvent -> {
            String requestParameters = cdpAuditEvent.getApiRequestEvent().getRequestParameters();
            return (requestParameters.contains(testDto.getName()) || requestParameters.contains(testDto.getCrn()))
                    && cdpAuditEvent.getEventName().equals(eventName);
        }).collect(Collectors.toList());
    }

    @NotNull
    protected String getStopEventName() {
        throw new UnsupportedOperationException("Cannot check stop event for " + getClass().getSimpleName() + " because it is not supported");
    }

    @NotNull
    protected String getDeleteEventName() {
        throw new UnsupportedOperationException("Cannot check delete event for " + getClass().getSimpleName() + " because it is not supported");
    }

    @NotNull
    protected String getStartEventName() {
        throw new UnsupportedOperationException("Cannot check start event for " + getClass().getSimpleName() + " because it is not supported");
    }

    @NotNull
    protected String getCreateEventName() {
        throw new UnsupportedOperationException("Cannot check create event for " + getClass().getSimpleName() + " because it is not supported");
    }

    @NotNull
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
