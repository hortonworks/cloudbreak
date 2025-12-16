package com.sequenceiq.flow.domain;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.converter.ClassValueConverter;
import com.sequenceiq.flow.converter.OperationTypeConverter;
import com.sequenceiq.flow.converter.StateStatusConverter;

@Entity
public class FlowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flowlog_generator")
    @SequenceGenerator(name = "flowlog_generator", sequenceName = "flowlog_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long resourceId;

    private Long created = new Date().getTime();

    @Column(nullable = false)
    private String flowId;

    private String flowChainId;

    private String nextEvent;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String payloadJackson;

    @Convert(converter = ClassValueConverter.class)
    private ClassValue payloadType;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String variablesJackson;

    @Convert(converter = ClassValueConverter.class)
    private ClassValue flowType;

    private String currentState;

    @Column(nullable = false)
    private Boolean finalized = Boolean.FALSE;

    private String cloudbreakNodeId;

    @Convert(converter = StateStatusConverter.class)
    private StateStatus stateStatus = StateStatus.PENDING;

    @Version
    private Long version;

    private String resourceType;

    private String flowTriggerUserCrn;

    @Convert(converter = OperationTypeConverter.class)
    private OperationType operationType = OperationType.UNKNOWN;

    @Column
    private Long endTime;

    private String reason;

    public FlowLog() {

    }

    public FlowLog(Long resourceId,
            String flowId,
            String currentState,
            Boolean finalized,
            StateStatus stateStatus,
            OperationType operationType) {
        this.resourceId = resourceId;
        this.flowId = flowId;
        this.currentState = currentState;
        this.finalized = finalized;
        this.stateStatus = stateStatus;
        this.operationType = operationType;
        this.endTime = null;
    }

    public FlowLog(Long resourceId,
            String flowId,
            String flowChainId,
            String flowTriggerUserCrn,
            String nextEvent,
            String payloadJackson,
            ClassValue payloadType,
            String variablesJackson,
            ClassValue flowType,
            String currentState) {
        this.resourceId = resourceId;
        this.flowId = flowId;
        this.flowChainId = flowChainId;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.nextEvent = nextEvent;
        this.payloadJackson = payloadJackson;
        this.payloadType = payloadType;
        this.variablesJackson = variablesJackson;
        this.flowType = flowType;
        this.currentState = currentState;
        this.endTime = null;
    }

    public FlowLog(Long resourceId,
            String flowId,
            String flowChainId,
            String flowTriggerUserCrn,
            String nextEvent,
            String payloadJackson,
            ClassValue payloadType,
            String variablesJackson,
            ClassValue flowType,
            String currentState,
            Long endTime) {
        this(resourceId, flowId, flowChainId, flowTriggerUserCrn, nextEvent,
                payloadJackson, payloadType, variablesJackson, flowType, currentState);
        this.endTime = endTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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

    public ClassValue getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(ClassValue payloadType) {
        this.payloadType = payloadType;
    }

    public ClassValue getFlowType() {
        return flowType;
    }

    public void setFlowType(ClassValue flowType) {
        this.flowType = flowType;
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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFlowTriggerUserCrn() {
        return flowTriggerUserCrn;
    }

    public void setFlowTriggerUserCrn(String flowTriggerUserCrn) {
        this.flowTriggerUserCrn = flowTriggerUserCrn;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getPayloadJackson() {
        return payloadJackson;
    }

    public void setPayloadJackson(String payloadJackson) {
        this.payloadJackson = payloadJackson;
    }

    public String getVariablesJackson() {
        return variablesJackson;
    }

    public void setVariablesJackson(String variablesJackson) {
        this.variablesJackson = variablesJackson;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String minimizedString() {
        return "FlowLog{" +
                "resourceId=" + resourceId +
                ", created=" + created +
                ", flowId='" + flowId + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", cloudbreakNodeId='" + cloudbreakNodeId + '\'' +
                ", currentState='" + currentState + '\'' +
                ", stateStatus=" + stateStatus +
                ", nextEvent=" + nextEvent +
                ", operationType=" + operationType +
                ", endTime=" + endTime +
                '}';
    }

    @Override
    public String toString() {
        return minimizedString();
    }

    public boolean isFlowType(Class<?> flowTypeClass) {
        return flowType != null
                && flowType.isOnClassPath()
                && flowTypeClass.equals(flowType.getClassValue());
    }
}
