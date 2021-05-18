package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV4Request extends InstanceTemplateV4Base {

    private RootVolumeV4Request rootVolume;

    private VolumeV4Request ephemeralVolume;

    private Set<VolumeV4Request> attachedVolumes;

    private TemporaryStorage temporaryStorage;

    public RootVolumeV4Request getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(RootVolumeV4Request rootVolume) {
        this.rootVolume = rootVolume;
    }

    public VolumeV4Request getEphemeralVolume() {
        return ephemeralVolume;
    }

    public void setEphemeralVolume(VolumeV4Request ephemeralVolume) {
        this.ephemeralVolume = ephemeralVolume;
    }

    public Set<VolumeV4Request> getAttachedVolumes() {
        if (attachedVolumes == null) {
            return new HashSet<>();
        }
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeV4Request> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }

    public TemporaryStorage getTemporaryStorage() {
        return temporaryStorage;
    }

    public void setTemporaryStorage(TemporaryStorage temporaryStorage) {
        this.temporaryStorage = temporaryStorage;
    }
}
