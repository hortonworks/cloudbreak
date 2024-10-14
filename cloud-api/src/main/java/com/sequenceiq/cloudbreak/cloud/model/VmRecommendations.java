package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VmRecommendations {

    private VmRecommendation master;

    private VmRecommendation worker;

    private VmRecommendation broker;

    private VmRecommendation quorum;

    public VmRecommendation getMaster() {
        return master;
    }

    public void setMaster(VmRecommendation master) {
        this.master = master;
    }

    public VmRecommendation getWorker() {
        return worker;
    }

    public void setWorker(VmRecommendation worker) {
        this.worker = worker;
    }

    public VmRecommendation getBroker() {
        return broker;
    }

    public void setBroker(VmRecommendation broker) {
        this.broker = broker;
    }

    public VmRecommendation getQuorum() {
        return quorum;
    }

    public void setQuorum(VmRecommendation quorum) {
        this.quorum = quorum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VmRecommendations that = (VmRecommendations) o;
        return Objects.equals(master, that.master) &&
                Objects.equals(worker, that.worker) &&
                Objects.equals(broker, that.broker) &&
                Objects.equals(quorum, that.quorum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(master, worker, broker, quorum);
    }
}
