package com.sequenceiq.cloudbreak.service.environment;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.cdp.shaded.org.apache.commons.lang3.StringUtils;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentConfigProvider {

    @Inject
    private EnvironmentClientService environmentClientService;

    public boolean isChildEnvironment(String environmentCrn) {
        try {
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByCrn(environmentCrn);
            return StringUtils.isNoneEmpty(environmentResponse.getParentEnvironmentCrn());
        } catch (Exception ex) {
            throw new CloudbreakServiceException("Failed to get environment details from Environment service due to:", ex);
        }
    }
}
