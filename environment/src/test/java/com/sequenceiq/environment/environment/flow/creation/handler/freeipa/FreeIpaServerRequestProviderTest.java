package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaServerRequestProviderTest {

    private static final String ENV_NAME = "envName";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String ENV_DOMAIN = "mydomain";

    @InjectMocks
    private FreeIpaServerRequestProvider underTest;

    @Test
    void testCreateWithEnvironmentDomain() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        FreeIpaServerRequest freeIpaServerRequest = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(environmentDto));

        assertEquals(ENV_DOMAIN, freeIpaServerRequest.getDomain());
    }

    private EnvironmentDto getEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENV_NAME);
        environmentDto.setDomain(ENV_DOMAIN);
        return environmentDto;
    }
}
