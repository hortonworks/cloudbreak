package com.sequenceiq.environment.parameters.service;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.repository.BaseParametersRepository;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.notification.domain.DistributionList;

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

    public void updateDistributionListDetails(Long environmentId, DistributionList distributionList) {
        if (distributionList == null) {
            LOGGER.warn("Distribution list id null for environment id: {}, nothing to do", environmentId);
        } else {
            Optional<BaseParameters> baseParametersOptional = baseParametersRepository.findByEnvironmentId(environmentId);
            if (baseParametersOptional.isEmpty()) {
                LOGGER.warn("Environment parameters not found for environment id: {}", environmentId);
            } else {
                BaseParameters baseParameters = baseParametersOptional.get();
                String uuid = distributionList.generateDistributionListUuid();

                baseParameters.setDistributionList(uuid);
                baseParametersRepository.save(baseParameters);
                LOGGER.debug("Distribution list updated for environment id: {} with uuid {}", environmentId, uuid);
            }
        }
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
