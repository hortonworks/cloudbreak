package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV1Request extends InstanceTemplateV1Base {

    private RootVolumeV1Request rootVolume;

    private VolumeV1Request ephemeralVolume;

    private Set<VolumeV1Request> attachedVolumes;

    private TemporaryStorage temporaryStorage;

    public RootVolumeV1Request getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(RootVolumeV1Request rootVolume) {
        this.rootVolume = rootVolume;
    }

    public VolumeV1Request getEphemeralVolume() {
        return ephemeralVolume;
    }

    public void setEphemeralVolume(VolumeV1Request ephemeralVolume) {
        this.ephemeralVolume = ephemeralVolume;
    }

    public Set<VolumeV1Request> getAttachedVolumes() {
        if (attachedVolumes == null) {
            return new HashSet<>();
        }
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeV1Request> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }

    public TemporaryStorage getTemporaryStorage() {
        return temporaryStorage;
    }

    public void setTemporaryStorage(TemporaryStorage temporaryStorage) {
        this.temporaryStorage = temporaryStorage;
    }
}
