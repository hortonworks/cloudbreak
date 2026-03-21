package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENVIRONMENT_RESOURCE_ENCRYPTION_KEY;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.GuestOsFeature;
import com.google.api.services.compute.model.Image;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpImageUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class GcpImageRegisterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpImageRegisterService.class);

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    public void register(AuthenticatedContext authenticatedContext, String bucketName, String tarName, String gcpImageName, CloudStack cloudStack)
            throws IOException {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        String projectId = gcpStackUtil.getProjectId(credential);
        Compute compute = gcpComputeFactory.buildCompute(credential);

        Image gcpApiImage = new Image();
        gcpApiImage.setName(gcpImageName);
        Image.RawDisk rawDisk = new Image.RawDisk();
        rawDisk.setSource(String.format("http://storage.googleapis.com/%s/%s", bucketName, tarName));
        gcpApiImage.setRawDisk(rawDisk);
        GuestOsFeature uefiCompatible = new GuestOsFeature().setType("UEFI_COMPATIBLE");
        GuestOsFeature multiIpSubnet = new GuestOsFeature().setType("MULTI_IP_SUBNET");
        GuestOsFeature gvnicSubnet = new GuestOsFeature().setType(GcpImageUtil.GVNIC);
        gcpApiImage.setGuestOsFeatures(List.of(uefiCompatible, multiIpSubnet, gvnicSubnet));
        Optional.ofNullable(cloudStack.getParameters().get(ENVIRONMENT_RESOURCE_ENCRYPTION_KEY))
                .ifPresent(key -> gcpApiImage.setImageEncryptionKey(new CustomerEncryptionKey().setKmsKeyName(key)));
        try {
            Compute.Images.Insert ins = compute.images().insert(projectId, gcpApiImage);
            ins.execute();
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() != HttpStatus.SC_CONFLICT) {
                String detailedMessage = ex.getDetails().getMessage();
                String msg = String.format("Failed to create image with name '%s' in project '%s': %s", gcpImageName, projectId, detailedMessage);
                LOGGER.warn(msg, ex);
                throw ex;
            } else {
                LOGGER.info("No need to create image as it exists already with name '{}' in project '{}':", gcpImageName, projectId);
            }
        }
    }
}
