package com.sequenceiq.cloudbreak.service.database;

import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class EnvironmentDatabaseServiceTest {
    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @InjectMocks
    private EnvironmentDatabaseService underTest;

    @Test
    void testSingleServerFallbackWhenPrivateSingleSetupAndSingleServerGiven() {
        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.validateOrModifyDatabaseTypeIfNeeded(SINGLE_SERVER));

        assertEquals(SINGLE_SERVER, actualResult);
    }

    @Test
    void testValidateWhenFlexibleEnabledAndNoDBTypeGiven() {
        AzureDatabaseType actualResult = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.validateOrModifyDatabaseTypeIfNeeded(null));

        assertEquals(FLEXIBLE_SERVER, actualResult);
    }

}