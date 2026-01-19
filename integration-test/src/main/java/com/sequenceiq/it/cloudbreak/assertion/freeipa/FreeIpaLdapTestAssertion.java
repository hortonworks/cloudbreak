package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaLdapTestAssertion {

    private FreeIpaLdapTestAssertion() {

    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validate() {
        return (testContext, entity, freeIpaClient) -> {
            DescribeLdapConfigResponse ldapConfigResponse =
                    freeIpaClient.getDefaultClient(testContext).getLdapConfigV1Endpoint().describe(entity.getResponse().getEnvironmentCrn());
            assertNotNull(ldapConfigResponse);
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(Locale.ROOT),
                    ldapConfigResponse.getDomain().toUpperCase(Locale.ROOT));
            return entity;
        };
    }
}
