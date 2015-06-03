package com.sequenceiq.cloudbreak.service.credential.gcp;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpStackUtil;

@Component
public class GcpCredentialHandler implements CredentialHandler<GcpCredential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentialHandler.class);
    @Inject
    private OpenSshPublicKeyValidator rsaPublicKeyValidator;
    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public GcpCredential init(GcpCredential gcpCredential) {
        rsaPublicKeyValidator.validate(gcpCredential);
        validateCredential(gcpCredential);
        return gcpCredential;
    }

    @Override
    public boolean delete(GcpCredential credential) {
        return true;
    }

    @Override
    public GcpCredential update(GcpCredential credential) throws Exception {
        return credential;
    }

    private void validateCredential(GcpCredential gcpCredential) {
        try {
            Compute compute = gcpStackUtil.buildCompute(gcpCredential);
            if (compute == null) {
                throw new BadRequestException("Problem with your credential key please use the correct format.");
            }
            gcpStackUtil.listDisks(compute, gcpCredential);
        } catch (Exception e) {
            String errorMessage = String.format("Could not validate credential [credential: '%s'], detailed message: %s",
                    gcpCredential.getName(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
