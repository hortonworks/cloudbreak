package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.converter.TunnelConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.parameters.dao.converter.CloudStorageValidationConverter;
import com.sequenceiq.environment.parameters.dao.converter.IdBrokerMappingSourceConverter;

@Embeddable
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentalFeatures implements Serializable {

    @Convert(converter = TunnelConverter.class)
    private Tunnel tunnel;

    @Convert(converter = IdBrokerMappingSourceConverter.class)
    @Column(name = "idbroker_mapping_source")
    private IdBrokerMappingSource idBrokerMappingSource;

    @Convert(converter = CloudStorageValidationConverter.class)
    private CloudStorageValidation cloudStorageValidation;

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public IdBrokerMappingSource getIdBrokerMappingSource() {
        return idBrokerMappingSource;
    }

    public void setIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
        this.idBrokerMappingSource = idBrokerMappingSource;
    }

    public CloudStorageValidation getCloudStorageValidation() {
        return cloudStorageValidation;
    }

    public void setCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
        this.cloudStorageValidation = cloudStorageValidation;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return tunnel == null && idBrokerMappingSource == null && cloudStorageValidation == null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Tunnel tunnel;

        private IdBrokerMappingSource idBrokerMappingSource;

        private CloudStorageValidation cloudStorageValidation;

        private Builder() {
        }

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
            return this;
        }

        public Builder withCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
            this.cloudStorageValidation = cloudStorageValidation;
            return this;
        }

        public ExperimentalFeatures build() {
            ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();
            experimentalFeatures.setTunnel(tunnel);
            experimentalFeatures.setIdBrokerMappingSource(idBrokerMappingSource);
            experimentalFeatures.setCloudStorageValidation(cloudStorageValidation);
            return experimentalFeatures;
        }
    }
}
