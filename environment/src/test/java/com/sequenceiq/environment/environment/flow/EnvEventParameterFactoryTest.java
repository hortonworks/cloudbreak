package com.sequenceiq.environment.environment.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class EnvEventParameterFactoryTest {

    private static final String CRN = "crn";

    private static final long RESOURCE_ID = 1L;

    @InjectMocks
    private EnvEventParameterFactory underTest;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Environment environment;

    @Test
    void getUserCrnByResourceIdEmpty() {
        Optional<String> result = underTest.getUserCrnByResourceId(RESOURCE_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCrnByResourceIdValue() {
        when(environmentService.findEnvironmentById(RESOURCE_ID)).thenReturn(Optional.of(environment));
        when(environment.getCreator()).thenReturn(CRN);

        Optional<String> result = underTest.getUserCrnByResourceId(RESOURCE_ID);

        assertEquals(CRN, result.get());
    }

}
