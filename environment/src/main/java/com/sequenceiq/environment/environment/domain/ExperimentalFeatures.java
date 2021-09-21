package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.converter.TunnelConverter;
import com.sequenceiq.common.api.type.CcmV2TlsType;
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

    private boolean overrideTunnel;

    @Convert(converter = IdBrokerMappingSourceConverter.class)
    @Column(name = "idbroker_mapping_source")
    private IdBrokerMappingSource idBrokerMappingSource;

    @Convert(converter = CloudStorageValidationConverter.class)
    private CloudStorageValidation cloudStorageValidation;

    private CcmV2TlsType ccmV2TlsType;

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

    public boolean isOverrideTunnel() {
        return overrideTunnel;
    }

    public void setOverrideTunnel(boolean overrideTunnel) {
        this.overrideTunnel = overrideTunnel;
    }

    public CcmV2TlsType getCcmV2TlsType() {
        return ccmV2TlsType;
    }

    public void setCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
        this.ccmV2TlsType = ccmV2TlsType;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return tunnel == null && idBrokerMappingSource == null && cloudStorageValidation == null && ccmV2TlsType == null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Tunnel tunnel;

        private boolean overrideTunnel;

        private IdBrokerMappingSource idBrokerMappingSource;

        private CloudStorageValidation cloudStorageValidation;

        private CcmV2TlsType ccmV2TlsType;

        private Builder() {
        }

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withOverrideTunnel(Boolean overrideTunnel) {
            if (overrideTunnel == null || !overrideTunnel) {
                this.overrideTunnel = false;
            } else {
                this.overrideTunnel = true;
            }
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

        public Builder withCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
            this.ccmV2TlsType = ccmV2TlsType;
            return this;
        }

        public ExperimentalFeatures build() {
            ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();
            experimentalFeatures.setTunnel(tunnel);
            experimentalFeatures.setIdBrokerMappingSource(idBrokerMappingSource);
            experimentalFeatures.setCloudStorageValidation(cloudStorageValidation);
            experimentalFeatures.setOverrideTunnel(overrideTunnel);
            experimentalFeatures.setCcmV2TlsType(ccmV2TlsType);
            return experimentalFeatures;
        }

    }

}
