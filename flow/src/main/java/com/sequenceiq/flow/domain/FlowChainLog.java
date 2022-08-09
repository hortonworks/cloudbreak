package com.sequenceiq.flow.domain;

import java.util.Date;
import java.util.Queue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.cedarsoftware.util.io.JsonReader;
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

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT", nullable = false)
    private String chain;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String chainJackson;

    private String flowTriggerUserCrn;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String triggerEvent;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    private String triggerEventJackson;

    public FlowChainLog() {

    }

    public FlowChainLog(String flowChainType, String flowChainId, String parentFlowChainId, String chain, String chainJackson, String flowTriggerUserCrn,
            String triggerEvent, String triggerEventJackson) {

        this.flowChainType = flowChainType;
        this.flowChainId = flowChainId;
        this.parentFlowChainId = parentFlowChainId;
        this.chain = chain;
        this.chainJackson = chainJackson;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.triggerEvent = triggerEvent;
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

    public String getChain() {
        return chain;
    }

    public Queue<Selectable> getChainAsQueue() {
        if (null != chainJackson) {
            return TypedJsonUtil.readValueWithJsonIoFallback(chainJackson, chain, Queue.class);
        } else {
            return (Queue<Selectable>) JsonReader.jsonToJava(chain);
        }
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
