package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Service
public class CloudIdentityRangerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudIdentityRangerSyncService.class);

    @Inject
    private ClouderaManagerRangerUtil clouderaManagerRangerUtil;

    @Inject
    private SdxEndpoint sdxEndpoint;

    public void updateAzureCloudIdentityMapping(String environmentCrn,
                                                Map<String, String> azureUserMapping,
                                                Map<String, String> azureGroupMapping) {
        LOGGER.info("Updating Azure cloud id mappings for environment = {}", environmentCrn);
        List<SdxClusterResponse> responses = sdxEndpoint.getByEnvCrn(environmentCrn);
        if (responses.isEmpty()) {
            LOGGER.info("Environment has no datalake clusters to sync");
        }
        responses.forEach(sdxClusterResponse -> {
            String stackCrn = sdxClusterResponse.getStackCrn();
            LOGGER.info("Updating azure cloud id mappings for datalake stack crn = {}, environment = {}", stackCrn, environmentCrn);
            try {
                clouderaManagerRangerUtil.updateAzureCloudIdentityMapping(stackCrn, azureUserMapping, azureGroupMapping);
            } catch (ApiException e) {
                throw new RuntimeException("Encountered api exception", e);
            }
        });
    }
}
