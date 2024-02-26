package com.sequenceiq.environment.environment.service.externalizedcompute;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.dto.ExternalizedClusterCreateDto;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;

@Component
public class ExternalizedComputeClusterCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreationService.class);

    @Value("${environment.externalizedcompute.enabled}")
    private boolean externalizedComputeEnabled;

    @Inject
    private ExternalizedComputeClusterEndpoint endpoint;

    public void createExternalizedComputeCluster(String envCrn, ExternalizedClusterCreateDto dto) {
            if (isExternalizedClusterConfigured(dto)) {
                if (!externalizedComputeEnabled) {
                    throw new BadRequestException("Externalized compute not enabled");
                }
                try {
                    LOGGER.debug("Creating ExternalizedCompute cluster with name {}", dto.getName());
                    ExternalizedComputeClusterRequest request = new ExternalizedComputeClusterRequest();
                    request.setEnvironmentCrn(envCrn);
                    request.setName(dto.getName());
                    endpoint.create(request);
                } catch (Exception e) {
                    LOGGER.error("Could not create ExternalizedCompute cluster due to:", e);
                    throw e;
                }
            } else {
                LOGGER.debug("Creating ExternalizedCompute cluster is skipped because it was not configured");
            }
    }

    public boolean isExternalizedClusterConfigured(ExternalizedClusterCreateDto dto) {
        return dto != null && StringUtils.isNotEmpty(dto.getName());
    }
}
