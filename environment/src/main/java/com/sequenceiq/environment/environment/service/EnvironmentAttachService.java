package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;

import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAttachRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.proxy.ProxyConfig;
import com.sequenceiq.environment.proxy.ProxyConfigService;

@Service
public class EnvironmentAttachService {

    private final ProxyConfigService proxyConfigService;

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentService environmentService;

    private final ConversionService conversionService;

    public EnvironmentAttachService(ProxyConfigService proxyConfigService, EnvironmentValidatorService validatorService,
            EnvironmentService environmentService, ConversionService conversionService) {
        this.proxyConfigService = proxyConfigService;
        this.validatorService = validatorService;
        this.environmentService = environmentService;
        this.conversionService = conversionService;
    }

    public DetailedEnvironmentResponse attachResources(String environmentName, EnvironmentAttachRequest request) {
        Set<ProxyConfig> proxiesToAttach = proxyConfigService.findByNamesInAccount(request.getProxies(), TEMP_ACCOUNT_ID);
        ValidationResult validationResult = validatorService.validateProxyAttach(request, proxiesToAttach);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        Environment environment = environmentService.getByNameForAccountId(environmentName, TEMP_ACCOUNT_ID);
        environment = doAttach(proxiesToAttach, environment);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    private Environment doAttach(Set<ProxyConfig> proxiesToAttach, Environment environment) {
        proxiesToAttach.removeAll(environment.getProxyConfigs());
        environment.getProxyConfigs().addAll(proxiesToAttach);
        environment = environmentService.save(environment);
        return environment;
    }
}
