package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "clusterhistory")
public class ClusterHistory extends AbstractHistory {

    private Date creationStarted;
    private Date creationFinished;
    private String status;
    private String statusReason;
    private String blueprintId;

    public Date getCreationStarted() {
        return creationStarted;
    }

    public void setCreationStarted(Date creationStarted) {
        this.creationStarted = creationStarted;
    }

    public Date getCreationFinished() {
        return creationFinished;
    }

    public void setCreationFinished(Date creationFinished) {
        this.creationFinished = creationFinished;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }
}
