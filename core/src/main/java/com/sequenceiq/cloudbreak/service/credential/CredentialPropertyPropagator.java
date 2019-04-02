package com.sequenceiq.cloudbreak.service.credential;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;

@Component
public class CredentialPropertyPropagator {

    public Optional<Mappable> propagateCredentialProperty(CredentialV4Request request) {
        return propagate(request);
    }

    private Optional<Mappable> propagate(CredentialV4Request cred) {
        if (cred.getAws() != null) {
            return Optional.of(cred.getAws());
        } else if (cred.getAzure() != null) {
            return Optional.of(cred.getAzure());
        } else if (cred.getOpenstack() != null) {
            return Optional.of(cred.getOpenstack());
        } else if (cred.getYarn() != null) {
            return Optional.of(cred.getYarn());
        } else if (cred.getCumulus() != null) {
            return Optional.of(cred.getCumulus());
        } else if (cred.getMock() != null) {
            return Optional.of(cred.getMock());
        } else if (cred.getGcp() != null) {
            return Optional.of(cred.getGcp());
        }
        return Optional.empty();
    }

}
