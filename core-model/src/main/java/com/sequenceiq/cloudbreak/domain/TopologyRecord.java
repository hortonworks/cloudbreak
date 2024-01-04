package com.sequenceiq.cloudbreak.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TopologyRecord implements Serializable {
    @Column(columnDefinition = "TEXT", nullable = false)
    private String hypervisor;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rack;

    private TopologyRecord() {
    }

    public TopologyRecord(String hypervisor, String rack) {
        this.hypervisor = hypervisor;
        this.rack = rack;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }
}
