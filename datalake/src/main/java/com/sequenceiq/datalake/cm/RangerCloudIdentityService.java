package com.sequenceiq.datalake.cm;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.datalake.controller.exception.RangerCloudIdentitySyncException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RangerCloudIdentityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RangerCloudIdentityService.class);

    @Inject
    private ClouderaManagerRangerUtil clouderaManagerRangerUtil;

    @Inject
    private SdxService sdxService;

    private void setAzureCloudIdentityMapping(SdxCluster sdxCluster, Map<String, String> azureUserMapping, Map<String, String> azureGroupMapping) {
        String stackCrn = sdxCluster.getStackCrn();
        LOGGER.info("Updating azure cloud id mappings for datalake stack crn = {}", stackCrn);
        try {
            clouderaManagerRangerUtil.setAzureCloudIdentityMapping(stackCrn, azureUserMapping, azureGroupMapping);
        } catch (ApiException e) {
            LOGGER.error("Encountered api exception", e);
            throw new RangerCloudIdentitySyncException("Encountered api exception", e);
        }
    }

    public void setAzureCloudIdentityMapping(String environmentCrn, Map<String, String> azureUserMapping, Map<String, String> azureGroupMapping) {
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(environmentCrn);
        List<String> sdxClusterCrns = sdxClusters.stream().map(SdxCluster::getCrn).collect(Collectors.toList());
        LOGGER.info("Setting Azure cloud id mappings for datalake clusters = {}, environment = {}", sdxClusterCrns, environmentCrn);
        sdxClusters.forEach(sdxCluster -> setAzureCloudIdentityMapping(sdxCluster, azureUserMapping, azureGroupMapping));
    }
}
