package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

@Entity
public class FlowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flowlog_generator")
    @SequenceGenerator(name = "flowlog_generator", sequenceName = "flowlog_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long stackId;

    private Long created = new Date().getTime();

    @Column(nullable = false)
    private String flowId;

    private String flowChainId;

    private String nextEvent;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String payload;

    private Class<?> payloadType;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String variables;

    private Class<?> flowType;

    private String currentState;

    @Column(nullable = false)
    private Boolean finalized = Boolean.FALSE;

    private String cloudbreakNodeId;

    @Enumerated(EnumType.STRING)
    private StateStatus stateStatus = StateStatus.PENDING;

    @Version
    private Long version;

    public FlowLog() {

    }

    public FlowLog(Long stackId, String flowId, String currentState, Boolean finalized, StateStatus stateStatus) {
        this.stackId = stackId;
        this.flowId = flowId;
        this.currentState = currentState;
        this.finalized = finalized;
        this.stateStatus = stateStatus;
    }

    public FlowLog(Long stackId, String flowId, String flowChainId, String nextEvent, String payload, Class<?> payloadType, String variables, Class<?> flowType,
            String currentState) {
        this.stackId = stackId;
        this.flowId = flowId;
        this.flowChainId = flowChainId;
        this.nextEvent = nextEvent;
        this.payload = payload;
        this.payloadType = payloadType;
        this.variables = variables;
        this.flowType = flowType;
        this.currentState = currentState;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowChainId() {
        return flowChainId;
    }

    public void setFlowChainId(String flowChainId) {
        this.flowChainId = flowChainId;
    }

    public String getNextEvent() {
        return nextEvent;
    }

    public void setNextEvent(String nextEvent) {
        this.nextEvent = nextEvent;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Class<?> getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(Class<?> payloadType) {
        this.payloadType = payloadType;
    }

    public Class<?> getFlowType() {
        return flowType;
    }

    public void setFlowType(Class<?> flowType) {
        this.flowType = flowType;
    }

    public String getVariables() {
        return variables;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public String getCloudbreakNodeId() {
        return cloudbreakNodeId;
    }

    public void setCloudbreakNodeId(String cloudbreakNodeId) {
        this.cloudbreakNodeId = cloudbreakNodeId;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public StateStatus getStateStatus() {
        return stateStatus;
    }

    public void setStateStatus(StateStatus stateStatus) {
        this.stateStatus = stateStatus;
    }

}
