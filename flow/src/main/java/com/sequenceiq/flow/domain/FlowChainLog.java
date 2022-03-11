package com.sequenceiq.flow.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class FlowChainLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flowchainlog_generator")
    @SequenceGenerator(name = "flowchainlog_generator", sequenceName = "flowchainlog_id_seq", allocationSize = 1)
    private Long id;

    private Long created = new Date().getTime();

    private String flowChainType;

    @Column(nullable = false)
    private String flowChainId;

    private String parentFlowChainId;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT", nullable = false)
    private String chain;

    private String flowTriggerUserCrn;

    private String triggerEvent;

    public FlowChainLog() {

    }

    public FlowChainLog(String flowChainType, String flowChainId, String parentFlowChainId, String chain, String flowTriggerUserCrn, String triggerEvent) {
        this.flowChainType = flowChainType;
        this.flowChainId = flowChainId;
        this.parentFlowChainId = parentFlowChainId;
        this.chain = chain;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.triggerEvent = triggerEvent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getFlowChainId() {
        return flowChainId;
    }

    public void setFlowChainId(String flowChainId) {
        this.flowChainId = flowChainId;
    }

    public String getParentFlowChainId() {
        return parentFlowChainId;
    }

    public void setParentFlowChainId(String parentFlowChainId) {
        this.parentFlowChainId = parentFlowChainId;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getFlowTriggerUserCrn() {
        return flowTriggerUserCrn;
    }

    public void setFlowTriggerUserCrn(String flowTriggerUserCrn) {
        this.flowTriggerUserCrn = flowTriggerUserCrn;
    }

    public String getFlowChainType() {
        return flowChainType;
    }

    public void setFlowChainType(String flowChainType) {
        this.flowChainType = flowChainType;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    @Override
    public String toString() {
        return "FlowChainLog{" +
                "id=" + id +
                ", created=" + created +
                ", flowChainType='" + flowChainType + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", parentFlowChainId='" + parentFlowChainId + '\'' +
                ", chain='" + chain + '\'' +
                ", flowTriggerUserCrn='" + flowTriggerUserCrn + '\'' +
                ", triggerEvent='" + triggerEvent + '\'' +
                '}';
    }
}
