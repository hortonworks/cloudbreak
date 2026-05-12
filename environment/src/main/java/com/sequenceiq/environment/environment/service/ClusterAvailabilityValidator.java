package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class ClusterAvailabilityValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterAvailabilityValidator.class);

    private final DatahubService datahubService;

    private final SdxService sdxService;

    public ClusterAvailabilityValidator(DatahubService datahubService, SdxService sdxService) {
        this.datahubService = datahubService;
        this.sdxService = sdxService;
    }

    public void validateAllClustersAvailable(String environmentCrn) {
        LOGGER.debug("Validating all cluster availability for environment CRN: {}", environmentCrn);
        List<String> nonAvailable = new ArrayList<>();

        List<SdxClusterResponse> datalakes = sdxService.listByEnvironmentCrn(environmentCrn);
        datalakes.stream()
                .filter(sdx -> !SdxClusterStatusResponse.RUNNING.equals(sdx.getStatus()))
                .map(sdx -> String.format("Data Lake %s (status: %s)", sdx.getCrn(), sdx.getStatus()))
                .forEach(nonAvailable::add);

        datahubService.getStatusesByEnvironmentCrn(environmentCrn).getResponses().stream()
                .filter(dh -> !Status.AVAILABLE.equals(dh.getStatus()))
                .map(dh -> String.format("DataHub %s (status: %s)", dh.getCrn(), dh.getStatus()))
                .forEach(nonAvailable::add);

        if (!nonAvailable.isEmpty()) {
            String message = "Cross-realm trust setup cannot be triggered because the following clusters are not available: "
                    + String.join(", ", nonAvailable);
            LOGGER.warn(message);
            throw new BadRequestException(message);
        }
        LOGGER.debug("All clusters are available for environment CRN: {}", environmentCrn);
    }
}



