package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.DatabaseVolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.RootVolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV4Response extends InstanceTemplateV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    private RootVolumeV4Response rootVolume;

    private VolumeV4Response ephemeralVolume;

    private Set<VolumeV4Response> attachedVolumes;

    private DatabaseVolumeV4Response databaseVolume;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RootVolumeV4Response getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(RootVolumeV4Response rootVolume) {
        this.rootVolume = rootVolume;
    }

    public VolumeV4Response getEphemeralVolume() {
        return ephemeralVolume;
    }

    public void setEphemeralVolume(VolumeV4Response ephemeralVolume) {
        this.ephemeralVolume = ephemeralVolume;
    }

    public Set<VolumeV4Response> getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeV4Response> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }

    public DatabaseVolumeV4Response getDatabaseVolume() {
        return databaseVolume;
    }

    public void setDatabaseVolume(DatabaseVolumeV4Response databaseVolume) {
        this.databaseVolume = databaseVolume;
    }
}
