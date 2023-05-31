package com.sequenceiq.cloudbreak.rotation.context;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.DatabaseRootPasswordSaltPillarGenerator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class DatabaseRootPasswordSaltPillarGeneratorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private DatabaseRootPasswordSaltPillarGenerator underTest;

    @Test
    void testGenerateSaltPillarForDatabaseRootPassword() {
        StackDto stackDto = new StackDto();
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenReturn(stackDto);
        Map<String, SaltPillarProperties> servicePillar = underTest.apply(RESOURCE_CRN);
        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(postgresConfigService, times(1)).decorateServicePillarWithPostgresIfNeeded(anyMap(), eq(stackDto));
    }

}