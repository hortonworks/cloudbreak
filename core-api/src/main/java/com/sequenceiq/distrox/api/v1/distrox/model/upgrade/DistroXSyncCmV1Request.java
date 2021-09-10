package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public class DistroXSyncCmV1Request {

    @ApiModelProperty(ModelDescriptions.CmSyncRequest.IMAGE_IDS)
    private Set<String> candidateImageUuids = new HashSet<>();

    public Set<String> getCandidateImageUuids() {
        return candidateImageUuids;
    }

    public void setCandidateImageUuids(Set<String> candidateImageUuids) {
        this.candidateImageUuids = candidateImageUuids;
    }

    @Override
    public String toString() {
        return "DistroXSyncCmV1Request{" +
                "candidateImageUuids=" + candidateImageUuids +
                '}';
    }
}
