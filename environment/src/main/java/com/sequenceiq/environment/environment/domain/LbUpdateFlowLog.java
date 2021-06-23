package com.sequenceiq.environment.environment.domain;

import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "lbupdate_flowlog")
public class LbUpdateFlowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "lbupdate_flowlog_generator")
    @SequenceGenerator(name = "lbupdate_flowlog_generator", sequenceName = "lbupdate_flowlog_id_seq", allocationSize = 1)
    private Long id;

    private String environmentCrn;

    private String parentFlowId;

    private String childFlowId;

    private String childResourceName;

    private String childResourceCrn;

    private LoadBalancerUpdateStatus status;

    private String currentState;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getParentFlowId() {
        return parentFlowId;
    }

    public void setParentFlowId(String parentFlowId) {
        this.parentFlowId = parentFlowId;
    }

    public String getChildFlowId() {
        return childFlowId;
    }

    public void setChildFlowId(String childFlowId) {
        this.childFlowId = childFlowId;
    }

    public String getChildResourceName() {
        return childResourceName;
    }

    public void setChildResourceName(String childResourceName) {
        this.childResourceName = childResourceName;
    }

    public String getChildResourceCrn() {
        return childResourceCrn;
    }

    public void setChildResourceCrn(String childResourceCrn) {
        this.childResourceCrn = childResourceCrn;
    }

    public LoadBalancerUpdateStatus getStatus() {
        return status;
    }

    public void setStatus(LoadBalancerUpdateStatus status) {
        this.status = status;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
}
