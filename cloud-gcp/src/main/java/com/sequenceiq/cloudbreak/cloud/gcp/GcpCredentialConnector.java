package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DiskList;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@Service
public class GcpCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentialConnector.class);

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Inject
    private GcpPlatformParameters gcpPlatformParameters;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        LOGGER.info("Verify credential: {}", authenticatedContext.getCloudCredential());
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, false);
        try {
            Compute compute = gcpContext.getCompute();
            if (compute == null) {
                throw new CloudConnectorException("Problem with your credential key please use the correct format.");
            }
            listDisks(gcpContext, compute);
        } catch (GoogleJsonResponseException e) {
            String errorMessage = String.format(e.getDetails().getMessage());
            LOGGER.error(errorMessage, e);
            return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.FAILED, e, errorMessage);
        } catch (Exception e) {
            String errorMessage = String.format("Could not verify credential [credential: '%s'], detailed message: %s", gcpContext.getName(), e.getMessage());
            LOGGER.error(errorMessage, e);
            return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.FAILED, e, errorMessage);
        }
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    private void listDisks(GcpContext gcpContext, Compute compute) throws IOException {
        List<Disk> disks = new ArrayList<>();
        for (AvailabilityZone gcpZone : gcpPlatformParameters.availabilityZones().getAllAvailabilityZone()) {
            try {
                Compute.Disks.List list = compute.disks().list(gcpContext.getProjectId(), gcpZone.value());
                DiskList execute = list.execute();
                disks.addAll(execute.getItems());
            } catch (NullPointerException ex) {
                disks.addAll(new ArrayList<Disk>());
            }
        }
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }
}
