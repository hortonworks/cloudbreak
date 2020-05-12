package com.sequenceiq.environment.parameters.service;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.repository.BaseParametersRepository;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;

@Service
public class ParametersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersService.class);

    private final BaseParametersRepository baseParametersRepository;

    private final Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    private final EnvironmentService environmentService;

    public ParametersService(BaseParametersRepository baseParametersRepository,
            Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap,
            EnvironmentService environmentService) {
        this.baseParametersRepository = baseParametersRepository;
        this.environmentParamsConverterMap = environmentParamsConverterMap;
        this.environmentService = environmentService;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseParameters> Optional<T> findByEnvironment(Long environmentId) {
        return baseParametersRepository.findByEnvironmentId(environmentId);
    }

    public BaseParameters saveParameters(Long environmentId, ParametersDto parametersDto) {
        return environmentService.findEnvironmentById(environmentId)
                .map(environment -> saveParameters(environment, parametersDto))
                .orElseThrow(() -> new IllegalStateException("Environment was not found by id " + environmentId));
    }

    public BaseParameters saveParameters(Environment environment, ParametersDto parametersDto) {
        BaseParameters savedParameters = null;
        if (parametersDto != null) {
            LOGGER.debug("Saving parameters for environment. Parameters: {}", parametersDto);
            EnvironmentParametersConverter environmentParametersConverter = environmentParamsConverterMap.get(getCloudPlatform(environment));
            if (environmentParametersConverter != null) {
                BaseParameters parameters = environmentParametersConverter.convert(environment, parametersDto);
                parameters.setId(getIfNotNull(parametersDto, ParametersDto::getId));
                parameters.setAccountId(environment.getAccountId());
                savedParameters = save(parameters);
            }
        }
        return savedParameters;
    }

    public boolean isS3GuardTableUsed(String accountId, String cloudPlatform, String location, String dynamoTableName) {
        return baseParametersRepository.isS3GuardTableUsed(accountId, cloudPlatform, EnvironmentStatus.AVAILABLE_STATUSES, location, dynamoTableName);
    }

    public void updateResourceGroupName(Environment environment, String resourceGroupName) {
        if (!CloudPlatform.AZURE.name().equals(environment.getCloudPlatform())) {
            return;
        }
        Optional<BaseParameters> baseParametersOptional = baseParametersRepository.findByEnvironmentId(environment.getId());
        if (baseParametersOptional.isEmpty()) {
            return;
        }

        BaseParameters baseParameters = baseParametersOptional.get();
        AzureParameters azureParameters = (AzureParameters) baseParameters;
        azureParameters.setResourceGroupName(resourceGroupName);
        baseParametersRepository.save(baseParameters);
    }

    private CloudPlatform getCloudPlatform(Environment environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }

    @SuppressWarnings("unchecked")
    private BaseParameters save(BaseParameters parameters) {
        Object saved = baseParametersRepository.save(parameters);
        return (BaseParameters) saved;
    }
}
