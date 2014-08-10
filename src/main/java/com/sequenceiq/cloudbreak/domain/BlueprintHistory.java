package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "blueprinthistory")
public class BlueprintHistory extends AbstractHistory {


    private Long blueprintId;
    private String blueprintName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String blueprintText;
    private String blueprintDescription;
    private int hostGroupCount;

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }

    public String getBlueprintDescription() {
        return blueprintDescription;
    }

    public void setBlueprintDescription(String blueprintDescription) {
        this.blueprintDescription = blueprintDescription;
    }

    public int getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(int hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
    }

}

