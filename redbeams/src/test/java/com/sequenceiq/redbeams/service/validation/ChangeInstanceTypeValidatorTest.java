package com.sequenceiq.redbeams.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class ChangeInstanceTypeValidatorTest {

    private static final String TARGET_INSTANCE_TYPE = "db.m5.xlarge";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenant:environment:env";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

    private final DatabaseStack databaseStack = new DatabaseStack(null, DatabaseServer.builder().withServerId("dbserver").build(), Map.of(), "");

    @Mock
    private DatabaseInstanceTypeValidator databaseInstanceTypeValidator;

    @InjectMocks
    private ChangeInstanceTypeValidator underTest;

    @ParameterizedTest
    @ValueSource(strings = {"db.m5.large", TARGET_INSTANCE_TYPE, "DB.M5.XLARGE"})
    void testValidWhenTheLiveInstanceTypeIsReported(String liveInstanceType) {
        assertDoesNotThrow(() -> validate(state(liveInstanceType)));

        verify(databaseInstanceTypeValidator).validate(TARGET_INSTANCE_TYPE, null, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void testInvalidWhenTheLiveInstanceTypeIsNotReported(String liveInstanceType) {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> validate(state(liveInstanceType)));

        assertEquals("The cloud provider did not report the current instance type of database server 'dbserver', so it can not be changed to "
                + "'db.m5.xlarge'.", exception.getMessage());
    }

    @Test
    void testValidWhenAModificationToTheTargetIsAlreadyInProgress() {
        assertDoesNotThrow(() -> validate(new DatabaseServerInstanceTypeState("DB.M5.XLARGE", 100L, true, true)));
    }

    @Test
    void testFailsFastWhenAModificationToADifferentInstanceTypeIsInProgress() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validate(new DatabaseServerInstanceTypeState("db.m5.large", 100L, true, true)));

        assertEquals("A modification is already in progress on database server 'dbserver', so its instance type can not be changed to "
                + "'db.m5.xlarge' now, retry once it finishes.", exception.getMessage());
    }

    @Test
    void testCapabilityWarningIsToleratedAndLogged() {
        when(databaseInstanceTypeValidator.validate(TARGET_INSTANCE_TYPE, null, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .thenReturn(Optional.of("Could not validate database instance type availability"));

        assertDoesNotThrow(() -> validate(state("db.m5.large")));
    }

    @Test
    void testCapabilityValidationFailurePropagates() {
        when(databaseInstanceTypeValidator.validate(TARGET_INSTANCE_TYPE, null, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .thenThrow(new BadRequestException("instance type not available in region"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validate(state("db.m5.large")));

        assertEquals("instance type not available in region", exception.getMessage());
    }

    private void validate(DatabaseServerInstanceTypeState liveState) {
        underTest.validate(databaseStack, liveState, TARGET_INSTANCE_TYPE, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
    }

    private static DatabaseServerInstanceTypeState state(String instanceType) {
        return new DatabaseServerInstanceTypeState(instanceType, 100L, false, false);
    }
}
