package com.sequenceiq.environment.environment.service.externalizedcompute;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.ExternalizedClusterCreateDto;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;

@Component
public class ExternalizedComputeClusterCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreationService.class);

    @Inject
    private ExternalizedComputeClusterEndpoint endpoint;

    public void createExternalizedComputeCluster(String envCrn, ExternalizedClusterCreateDto dto) {
        try {
            if (isExternalizedClusterConfigured(dto)) {
                LOGGER.debug("Creating ExternalizedCompute cluster with name {}", dto.getName());
                ExternalizedComputeClusterRequest request = new ExternalizedComputeClusterRequest();
                request.setEnvironmentCrn(envCrn);
                request.setName(dto.getName());
                endpoint.create(request);
            } else {
                LOGGER.debug("Creating ExternalizedCompute cluster is skipped because it was not configured");
            }
        } catch (Exception e) {
            LOGGER.error("Could not create ExternalizedCompute cluster due to:", e);
        }
    }

    public boolean isExternalizedClusterConfigured(ExternalizedClusterCreateDto dto) {
        return dto != null && StringUtils.isNotEmpty(dto.getName());
    }
}
