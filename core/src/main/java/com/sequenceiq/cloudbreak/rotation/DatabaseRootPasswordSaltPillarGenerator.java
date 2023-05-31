package com.sequenceiq.cloudbreak.rotation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatabaseRootPasswordSaltPillarGenerator implements Function<String, Map<String, SaltPillarProperties>> {

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public Map<String, SaltPillarProperties> apply(String resourceCrn) {
        StackDto stackDto = stackDtoService.getByCrn(resourceCrn);
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stackDto);
        return servicePillar;
    }
}
