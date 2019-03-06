package com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.base;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.MpackDetailsDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class ManagementPackV4Base implements JsonEntity {
    @NotNull
    @Size(max = 100, min = 5, message = "The length of the management pack's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The management pack's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @Pattern(regexp = "^http[s]?://.*",
            message = "The URL should start with the protocol (http, https)")
    @ApiModelProperty(MpackDetailsDescription.MPACK_URL)
    private String mpackUrl;

    @ApiModelProperty(MpackDetailsDescription.PURGE)
    private boolean purge;

    @ApiModelProperty(MpackDetailsDescription.PURGE_LIST)
    private List<String> purgeList = Collections.emptyList();

    @ApiModelProperty(MpackDetailsDescription.FORCE)
    private boolean force;

    @ApiModelProperty(MpackDetailsDescription.IGNORE_WARNING)
    private boolean ignoreValidation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMpackUrl() {
        return mpackUrl;
    }

    public void setMpackUrl(String mpackUrl) {
        this.mpackUrl = mpackUrl;
    }

    public boolean isPurge() {
        return purge;
    }

    public void setPurge(boolean purge) {
        this.purge = purge;
    }

    public List<String> getPurgeList() {
        return purgeList;
    }

    public void setPurgeList(List<String> purgeList) {
        this.purgeList = purgeList;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isIgnoreValidation() {
        return ignoreValidation;
    }

    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }
}
