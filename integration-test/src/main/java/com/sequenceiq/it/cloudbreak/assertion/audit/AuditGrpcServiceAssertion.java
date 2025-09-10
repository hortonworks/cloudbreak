package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;

public abstract class AuditGrpcServiceAssertion<T extends CloudbreakTestDto, C extends MicroserviceClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditGrpcServiceAssertion.class);

    @Inject
    private AuditClient auditClient;

    public T create(TestContext testContext, T testDto, C client) {
        OperationInfo operationInfo = getCreateOperationInfo();
        executeAuditValidation(testContext, testDto, operationInfo);
        return testDto;
    }

    public T start(TestContext testContext, T testDto, C client) {
        OperationInfo operationInfo = getStartOperationInfo();
        executeAuditValidation(testContext, testDto, operationInfo);
        return testDto;
    }

    public T stop(TestContext testContext, T testDto, C client) {
        OperationInfo operationInfo = getStopOperationInfo();
        executeAuditValidation(testContext, testDto, operationInfo);
        return testDto;
    }

    public T delete(TestContext testContext, T testDto, C client) {
        OperationInfo operationInfo = getDeleteOperationInfo();
        executeAuditValidation(testContext, testDto, operationInfo);
        return testDto;
    }

    public T modify(TestContext testContext, T testDto, C client) {
        OperationInfo operationInfo = getModifyOperationInfo();
        executeAuditValidation(testContext, testDto, operationInfo);
        return testDto;
    }

    private void executeAuditValidation(TestContext testContext, T testDto, OperationInfo operationInfo) {
        retry(operationInfo, () -> {
            List<AuditProto.CdpAuditEvent> cdpAuditEvents = auditClient.listEvents(ListAuditEvent.builder()
                    .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                    .eventSource(getService()).build());
            validateEventList(cdpAuditEvents, testDto, operationInfo);
        });
    }

    protected void validateEventList(List<AuditProto.CdpAuditEvent> cdpAuditEvents, T testDto, OperationInfo operationInfo) {
        String eventName = operationInfo.getEventName();
        if (shouldCheckRestEvents()) {
            List<AuditProto.CdpAuditEvent> restEvents = filterRestEvents(cdpAuditEvents, testDto, eventName);
            checkRestEvents(restEvents, eventName);
        }
        if (shouldCheckFlowEvents()) {
            List<AuditProto.CdpAuditEvent> flowEvents = filterFlowEvents(cdpAuditEvents, testDto, eventName);
            checkFlowEvents(flowEvents, testDto, operationInfo);
        }
    }

    protected AuditClient getAuditClient() {
        return auditClient;
    }

    private void checkFlowEvents(List<AuditProto.CdpAuditEvent> flowEvents, T testDto, OperationInfo operationInfo) {
        String eventName = operationInfo.getEventName();
        Set<String> firstStates = Objects.requireNonNull(operationInfo.getFirstStates(), "You should define the first state for the flow audit log check");
        Set<String> lastStates = Objects.requireNonNull(operationInfo.getLastStates(), "You should define the last state for the flow audit log check");
        if (flowEvents.isEmpty() || flowEvents.size() >= 2 && flowEvents.size() % 2 != 0) {
            throw new TestFailException(eventName + " flow audit log must contain minimum 2 items but has " + flowEvents.size());
        }
        if (flowEvents.stream().noneMatch(e -> firstStates.contains(getFlowState(e)))) {
            throw new TestFailException(eventName + " flow audit log must contain " + firstStates);
        }
        if (flowEvents.stream().noneMatch(e -> lastStates.contains(getFlowState(e)))) {
            throw new TestFailException(eventName + " flow audit log must contain " + lastStates);
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
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getString("flowState");
    }

    private String getCrn(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getString("clusterCrn");
    }

    private String getFlowId(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getString("flowId");
    }

    private String getUserCrn(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getString("userCrn");
    }

    private String getEnvironmentCrn(AuditProto.CdpAuditEvent e) {
        return new Json(e.getCdpServiceEvent().getAdditionalServiceEventDetails()).getString("environmentCrn");
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

    protected OperationInfo getStopOperationInfo() {
        throw new UnsupportedOperationException("Cannot check stop event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected OperationInfo getDeleteOperationInfo() {
        throw new UnsupportedOperationException("Cannot check delete event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected OperationInfo getStartOperationInfo() {
        throw new UnsupportedOperationException("Cannot check start event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected OperationInfo getCreateOperationInfo() {
        throw new UnsupportedOperationException("Cannot check create event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected OperationInfo getModifyOperationInfo() {
        throw new UnsupportedOperationException("Cannot check modify event for " + getClass().getSimpleName() + " because it is not supported");
    }

    protected boolean shouldCheckRestEvents() {
        return true;
    }

    protected boolean shouldCheckFlowEvents() {
        return true;
    }

    protected abstract Crn.Service getService();

    private void retry(OperationInfo operationInfo, Runnable runnable) {
        int maxRetry = 3;
        int attempt = 0;
        long waitingSec = 1;
        boolean running = true;
        while (running && attempt < maxRetry) {
            try {
                runnable.run();
                running = false;
            } catch (Exception e) {
                attempt++;
                LOGGER.info("Cannot validate the audit logs: {}, try the next attempt", operationInfo.getEventName(), e);
                waiting(waitingSec);
            }
        }
    }

    private void waiting(long waitingSec) {
        try {
            TimeUnit.SECONDS.sleep(waitingSec);
        } catch (InterruptedException e) {
            LOGGER.error("Wait is interrupted", e);
        }
    }
}
