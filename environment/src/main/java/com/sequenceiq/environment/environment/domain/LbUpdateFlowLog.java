package com.sequenceiq.environment.environment.domain;

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
}
