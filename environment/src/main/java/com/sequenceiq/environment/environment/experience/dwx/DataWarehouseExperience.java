package com.sequenceiq.environment.environment.experience.dwx;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.experience.Experience;
import com.sequenceiq.environment.environment.experience.dwx.responses.CpInternalEnvironmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataWarehouseExperience implements Experience {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataWarehouseExperience.class);

    private final DWXApi dwxApi;

    public DataWarehouseExperience(DWXApi dwxApi) {
        this.dwxApi = dwxApi;
    }

    @Override
    public boolean hasExistingClusterForEnvironment(Environment environment) {
        CpInternalEnvironmentResponse cpInternalEnvironmentResponse = dwxApi.listClustersByEnvCrn(environment.getResourceCrn());
        return cpInternalEnvironmentResponse.getResults().size() > 0;
    }

}
