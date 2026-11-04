package com.sequenceiq.environment.environment.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;

@ExtendWith(MockitoExtension.class)
class EnvironmentStackPatchResourceServiceTest {

    @InjectMocks
    private EnvironmentStackPatchResourceService underTest;

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, false, false",
            "ENV_STOPPED, false, false",
            "DELETE_FAILED, false, true",
            "CREATE_FAILED, false, true",
            "FREEIPA_DELETED_ON_PROVIDER_SIDE, false, false",
            "CREATION_INITIATED, false, true",
            "DELETE_INITIATED, false, true",
            "ARCHIVED, true, true",
            "AVAILABLE, true, true"
    })
    void isUnschedulable(EnvironmentStatus status, boolean archived, boolean expected) {
        Environment environment = new Environment();
        environment.setStatus(status);
        environment.setArchived(archived);

        assertEquals(expected, underTest.isUnschedulable(environment));
    }
}
