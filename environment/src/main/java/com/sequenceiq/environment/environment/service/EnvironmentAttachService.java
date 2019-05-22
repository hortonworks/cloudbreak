package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAttachRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;

@Service
public class EnvironmentAttachService {

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentService environmentService;

    private final ConversionService conversionService;

    public EnvironmentAttachService(EnvironmentValidatorService validatorService,
            EnvironmentService environmentService, ConversionService conversionService) {
        this.validatorService = validatorService;
        this.environmentService = environmentService;
        this.conversionService = conversionService;
    }

    public DetailedEnvironmentResponse attachResources(String environmentName, EnvironmentAttachRequest request) {
        Environment environment = environmentService.getByNameForAccountId(environmentName, TEMP_ACCOUNT_ID);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }
}
