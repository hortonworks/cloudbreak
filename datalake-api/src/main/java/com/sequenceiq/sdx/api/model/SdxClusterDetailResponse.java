package com.sequenceiq.sdx.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterDetailResponse extends SdxClusterResponse implements TaggedResponse {

    @Schema(description = ModelDescriptions.STACK_RESPONSE)
    private StackV4Response stackV4Response;

    public static SdxClusterDetailResponse create(SdxClusterResponse sdxClusterResponse, StackV4Response stackV4Response) {
        Builder builder = Builder.newSdxClusterDetailResponseBuilder();
        if (Objects.nonNull(sdxClusterResponse)) {
            builder.withCrn(sdxClusterResponse.getCrn())
                    .withName(sdxClusterResponse.getName())
                    .withStatus(sdxClusterResponse.getStatus())
                    .withStatusReason(sdxClusterResponse.getStatusReason())
                    .withEnvironmentName(sdxClusterResponse.getEnvironmentName())
                    .withEnvironmentCrn(sdxClusterResponse.getEnvironmentCrn())
                    .withStackCrn(sdxClusterResponse.getStackCrn())
                    .withClusterShape(sdxClusterResponse.getClusterShape())
                    .withCloudStorageBaseLocation(sdxClusterResponse.getCloudStorageBaseLocation())
                    .withCloudStorageFileSystemType(sdxClusterResponse.getCloudStorageFileSystemType())
                    .withRuntime(sdxClusterResponse.getRuntime())
                    .withRangerRazEnabled(sdxClusterResponse.getRangerRazEnabled())
                    .withRangerRmsEnabled(sdxClusterResponse.isRangerRmsEnabled())
                    .withSeLinux(sdxClusterResponse.getSeLinuxPolicy())
                    .withTags(sdxClusterResponse.getTags())
                    .withCertExpirationState(sdxClusterResponse.getCertExpirationState())
                    .withSdxClusterServiceVersion(sdxClusterResponse.getSdxClusterServiceVersion())
                    .withDetached(sdxClusterResponse.isDetached())
                    .withDetachedClusterName(sdxClusterResponse.getDetachedClusterName())
                    .withEnableMultiAz(sdxClusterResponse.isEnableMultiAz())
                    .withDatabaseEngineVersion(sdxClusterResponse.getDatabaseEngineVersion())
                    .withDatabaseServerCrn(sdxClusterResponse.getDatabaseServerCrn())
                    .withDatabaseAvailabilityType(Optional.ofNullable(sdxClusterResponse.getSdxDatabaseResponse()).map(SdxDatabaseResponse::getAvailabilityType)
                            .orElse(SdxDatabaseAvailabilityType.NONE))
                    .withCreated(sdxClusterResponse.getCreated())
                    .withProviderSyncStates(sdxClusterResponse.getProviderSyncStates());
        }
        return builder.withStackV4Response(stackV4Response).build();
    }

    public StackV4Response getStackV4Response() {
        return stackV4Response;
    }

    public void setStackV4Response(StackV4Response stackV4Response) {
        this.stackV4Response = stackV4Response;
    }

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(stackV4Response).map(stack -> stack.getTags()).map(tags -> tags.getTagValue(key)).orElse(null);
    }

    @Override
    public String toString() {
        return "SdxClusterDetailResponse{ " + super.toString() + " stackV4Response=" + stackV4Response + '}';
    }

    public static final class Builder {

        private StackV4Response stackV4Response;

        private String crn;

        private String name;

        private SdxClusterShape clusterShape;

        private SdxClusterStatusResponse status;

        private String statusReason;

        private String environmentName;

        private String environmentCrn;

        private String databaseServerCrn;

        private String stackCrn;

        private Long created;

        private String cloudStorageBaseLocation;

        private FileSystemType cloudStorageFileSystemType;

        private String runtime;

        private FlowIdentifier flowIdentifier;

        private boolean rangerRazEnabled;

        private boolean rangerRmsEnabled;

        private boolean enableMultiAz;

        private Map<String, String> tags = new HashMap<>();

        private CertExpirationState certExpirationState;

        private String sdxClusterServiceVersion;

        private boolean detached;

        private String databaseEngineVersion;

        private SdxDatabaseAvailabilityType databaseAvailabilityType;

        private String seLinux;

        private Set<ProviderSyncState> providerSyncStates;

        private String detachedClusterName;

        private Builder() {
        }

        public static Builder newSdxClusterDetailResponseBuilder() {
            return new Builder();
        }

        public Builder withStackV4Response(StackV4Response stackV4Response) {
            this.stackV4Response = stackV4Response;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDetachedClusterName(String detachedClusterName) {
            this.detachedClusterName = detachedClusterName;
            return this;
        }

        public Builder withClusterShape(SdxClusterShape clusterShape) {
            this.clusterShape = clusterShape;
            return this;
        }

        public Builder withStatus(SdxClusterStatusResponse status) {
            this.status = status;
            return this;
        }

        public Builder withStatusReason(String statusReason) {
            this.statusReason = statusReason;
            return this;
        }

        public Builder withEnvironmentName(String environmentName) {
            this.environmentName = environmentName;
            return this;
        }

        public Builder withEnvironmentCrn(String environmentCrn) {
            this.environmentCrn = environmentCrn;
            return this;
        }

        public Builder withDatabaseServerCrn(String databaseServerCrn) {
            this.databaseServerCrn = databaseServerCrn;
            return this;
        }

        public Builder withStackCrn(String stackCrn) {
            this.stackCrn = stackCrn;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public Builder withCloudStorageBaseLocation(String cloudStorageBaseLocation) {
            this.cloudStorageBaseLocation = cloudStorageBaseLocation;
            return this;
        }

        public Builder withCloudStorageFileSystemType(FileSystemType cloudStorageFileSystemType) {
            this.cloudStorageFileSystemType = cloudStorageFileSystemType;
            return this;
        }

        public Builder withRuntime(String runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder withFlowIdentifier(FlowIdentifier flowIdentifier) {
            this.flowIdentifier = flowIdentifier;
            return this;
        }

        public Builder withRangerRazEnabled(boolean rangerRazEnabled) {
            this.rangerRazEnabled = rangerRazEnabled;
            return this;
        }

        public Builder withSeLinux(String seLinux) {
            this.seLinux = seLinux;
            return this;
        }

        public Builder withRangerRmsEnabled(boolean rangerRmsEnabled) {
            this.rangerRmsEnabled = rangerRmsEnabled;
            return this;
        }

        public Builder withEnableMultiAz(boolean enableMultiAz) {
            this.enableMultiAz = enableMultiAz;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withCertExpirationState(CertExpirationState certExpirationState) {
            this.certExpirationState = certExpirationState;
            return this;
        }

        public Builder withProviderSyncStates(Set<ProviderSyncState> providerSyncStates) {
            this.providerSyncStates = providerSyncStates;
            return this;
        }

        public Builder withSdxClusterServiceVersion(String sdxClusterServiceVersion) {
            this.sdxClusterServiceVersion = sdxClusterServiceVersion;
            return this;
        }

        public Builder withDetached(boolean detached) {
            this.detached = detached;
            return this;
        }

        public Builder withDatabaseEngineVersion(String databaseEngineVersion) {
            this.databaseEngineVersion = databaseEngineVersion;
            return this;
        }

        public Builder withDatabaseAvailabilityType(SdxDatabaseAvailabilityType databaseAvailabilityType) {
            this.databaseAvailabilityType = databaseAvailabilityType;
            return this;
        }

        public SdxClusterDetailResponse build() {
            SdxClusterDetailResponse sdxClusterDetailResponse = new SdxClusterDetailResponse();
            sdxClusterDetailResponse.setStackV4Response(stackV4Response);
            sdxClusterDetailResponse.setCrn(crn);
            sdxClusterDetailResponse.setName(name);
            sdxClusterDetailResponse.setClusterShape(clusterShape);
            sdxClusterDetailResponse.setStatus(status);
            sdxClusterDetailResponse.setStatusReason(statusReason);
            sdxClusterDetailResponse.setEnvironmentName(environmentName);
            sdxClusterDetailResponse.setEnvironmentCrn(environmentCrn);
            sdxClusterDetailResponse.setDatabaseServerCrn(databaseServerCrn);
            sdxClusterDetailResponse.setStackCrn(stackCrn);
            sdxClusterDetailResponse.setCreated(created);
            sdxClusterDetailResponse.setCloudStorageBaseLocation(cloudStorageBaseLocation);
            sdxClusterDetailResponse.setCloudStorageFileSystemType(cloudStorageFileSystemType);
            sdxClusterDetailResponse.setRuntime(runtime);
            sdxClusterDetailResponse.setFlowIdentifier(flowIdentifier);
            sdxClusterDetailResponse.setRangerRazEnabled(rangerRazEnabled);
            sdxClusterDetailResponse.setEnableMultiAz(enableMultiAz);
            sdxClusterDetailResponse.setTags(tags);
            sdxClusterDetailResponse.setCertExpirationState(certExpirationState);
            sdxClusterDetailResponse.setSdxClusterServiceVersion(sdxClusterServiceVersion);
            sdxClusterDetailResponse.setDetached(detached);
            sdxClusterDetailResponse.setDetachedClusterName(detachedClusterName);
            sdxClusterDetailResponse.setSeLinuxPolicy(seLinux);
            sdxClusterDetailResponse.setDatabaseEngineVersion(databaseEngineVersion);
            sdxClusterDetailResponse.setProviderSyncStates(providerSyncStates);
            SdxDatabaseResponse sdxDatabaseResponse = new SdxDatabaseResponse();
            sdxDatabaseResponse.setAvailabilityType(databaseAvailabilityType);
            sdxDatabaseResponse.setDatabaseEngineVersion(databaseEngineVersion);
            sdxDatabaseResponse.setDatabaseServerCrn(databaseServerCrn);
            sdxClusterDetailResponse.setSdxDatabaseResponse(sdxDatabaseResponse);
            return sdxClusterDetailResponse;
        }
    }
}
