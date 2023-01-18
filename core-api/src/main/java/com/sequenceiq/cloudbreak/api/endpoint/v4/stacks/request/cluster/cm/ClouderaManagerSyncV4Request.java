package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerSyncV4Request implements JsonEntity {

    @Schema(description = ModelDescriptions.CmSyncRequest.IMAGE_IDS)
    private Set<String> candidateImageUuids = new HashSet<>();

    public Set<String> getCandidateImageUuids() {
        return candidateImageUuids;
    }

    public void setCandidateImageUuids(Set<String> candidateImageUuids) {
        this.candidateImageUuids = candidateImageUuids;
    }

    public ClouderaManagerSyncV4Request withCandidateImageUuids(Set<String> uuids) {
        candidateImageUuids = uuids;
        return this;
    }
}
