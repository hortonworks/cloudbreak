package com.sequenceiq.flow.domain;

import java.util.Date;
import java.util.Queue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;

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

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String chainJackson;

    private String flowTriggerUserCrn;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String triggerEventJackson;

    public FlowChainLog() {

    }

    public FlowChainLog(String flowChainType, String flowChainId, String parentFlowChainId, String chainJackson, String flowTriggerUserCrn,
            String triggerEventJackson) {

        this.flowChainType = flowChainType;
        this.flowChainId = flowChainId;
        this.parentFlowChainId = parentFlowChainId;
        this.chainJackson = chainJackson;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.triggerEventJackson = triggerEventJackson;
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

    @SuppressWarnings("unchecked")
    public Queue<Selectable> getChainAsQueue() {
        return TypedJsonUtil.readValueUnchecked(chainJackson, Queue.class);
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

    public String getChainJackson() {
        return chainJackson;
    }

    public void setChainJackson(String chainJackson) {
        this.chainJackson = chainJackson;
    }

    public String getTriggerEventJackson() {
        return triggerEventJackson;
    }

    public void setTriggerEventJackson(String triggerEventJackson) {
        this.triggerEventJackson = triggerEventJackson;
    }
}
