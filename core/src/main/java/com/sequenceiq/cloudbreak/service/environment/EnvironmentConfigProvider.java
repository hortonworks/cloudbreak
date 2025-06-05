package com.sequenceiq.cloudbreak.service.environment;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentConfigProvider.class);

    @Inject
    private EnvironmentService environmentClientService;

    public boolean isChildEnvironment(String environmentCrn) {
        DetailedEnvironmentResponse environment = getEnvironmentByCrn(environmentCrn);
        return StringUtils.isNoneEmpty(environment.getParentEnvironmentCrn());
    }

    public String getParentEnvironmentCrn(String environmentCrn) {
            String result = environmentCrn;
            DetailedEnvironmentResponse environmentResponse = getEnvironmentByCrn(environmentCrn);
            String parentEnvironmentCrn = environmentResponse.getParentEnvironmentCrn();
            if (StringUtils.isNotBlank(parentEnvironmentCrn)) {
                LOGGER.debug("The environment('{}') is a child, returning with the crn('{}') of the parent environment", environmentCrn, parentEnvironmentCrn);
                result = parentEnvironmentCrn;
            }
            return result;
    }

    public DetailedEnvironmentResponse getEnvironmentByCrn(String environmentCrn) {
        try {
            LOGGER.info("Fetch environment details by crn:'{}' from Environment service", environmentCrn);
            return environmentClientService.getByCrn(environmentCrn);
        } catch (Exception ex) {
            LOGGER.warn(String.format("Failed to get environment by crn:'%s' from Environment service", environmentCrn), ex);
            throw new CloudbreakServiceException("Failed to get environment details from Environment service due to:", ex);
        }
    }
}
