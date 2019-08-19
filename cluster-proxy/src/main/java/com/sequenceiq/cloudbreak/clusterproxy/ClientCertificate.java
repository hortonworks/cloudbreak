package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientCertificate {
    @JsonProperty
    private String keyRef;

    @JsonProperty
    private String certificateRef;

    @JsonCreator
    public ClientCertificate(String keyRef, String certificateRef) {
        this.keyRef = keyRef;
        this.certificateRef = certificateRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClientCertificate that = (ClientCertificate) o;

        return Objects.equals(keyRef, that.keyRef)
                && Objects.equals(certificateRef, that.certificateRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyRef, certificateRef);
    }

    @Override
    public String toString() {
        return "ClientCertificate{"
                + "keyRef='" + keyRef + '\''
                + "certificateRef='" + certificateRef + '\''
                + '}';
    }
}
