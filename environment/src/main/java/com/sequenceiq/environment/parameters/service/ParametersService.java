package com.sequenceiq.environment.parameters.service;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.repository.BaseParametersRepository;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;

@Service
public class ParametersService {

    @Inject
    private BaseParametersRepository baseParametersRepository;

    @Inject
    private Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    @SuppressWarnings("unchecked")
    public <T extends BaseParameters> Optional<T> findByEnvironment(Long environmentId) {
        return baseParametersRepository.findByEnvironmentId(environmentId);
    }

    public BaseParameters saveParameters(Environment environment, ParametersDto parametersDto, String accountId) {
        BaseParameters savedParameters = null;
        if (parametersDto != null) {
            EnvironmentParametersConverter environmentParametersConverter = environmentParamsConverterMap.get(getCloudPlatform(environment));
            if (environmentParametersConverter != null) {
                BaseParameters parameters = environmentParametersConverter.convert(environment, parametersDto);
                parameters.setId(getIfNotNull(parametersDto, ParametersDto::getId));
                parameters.setAccountId(accountId);
                savedParameters = save(parameters);
            }
        }
        return savedParameters;
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
