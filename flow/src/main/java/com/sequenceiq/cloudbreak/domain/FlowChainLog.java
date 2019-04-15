package com.sequenceiq.cloudbreak.domain;

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

    @Column(nullable = false)
    private String flowChainId;

    private String parentFlowChainId;

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT", nullable = false)
    private String chain;

    public FlowChainLog() {

    }

    public FlowChainLog(String flowChainId, String parentFlowChainId, String chain) {
        this.flowChainId = flowChainId;
        this.parentFlowChainId = parentFlowChainId;
        this.chain = chain;
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
}
