package com.sequenceiq.environment.parameters.service;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.EnvironmentStatus;
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

    public BaseParameters saveParameters(Environment environment, ParametersDto parametersDto) {
        BaseParameters savedParameters = null;
        if (parametersDto != null) {
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

    private CloudPlatform getCloudPlatform(Environment environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }

    @SuppressWarnings("unchecked")
    private BaseParameters save(BaseParameters parameters) {
        Object saved = baseParametersRepository.save(parameters);
        return (BaseParameters) saved;
    }
}
