package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;

@Embeddable
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentalFeatures implements Serializable {

    @Enumerated(EnumType.STRING)
    private Tunnel tunnel;

    @Enumerated(EnumType.STRING)
    @Column(name = "idbroker_mapping_source")
    private IdBrokerMappingSource idBrokerMappingSource;

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

    @JsonIgnore
    public boolean isEmpty() {
        return tunnel == null && idBrokerMappingSource == null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Tunnel tunnel;

        private IdBrokerMappingSource idBrokerMappingSource;

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

        public ExperimentalFeatures build() {
            ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();
            experimentalFeatures.setTunnel(tunnel);
            experimentalFeatures.setIdBrokerMappingSource(idBrokerMappingSource);
            return experimentalFeatures;
        }
    }
}
