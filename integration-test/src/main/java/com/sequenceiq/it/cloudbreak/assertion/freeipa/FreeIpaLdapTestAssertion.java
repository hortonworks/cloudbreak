package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaLdapTestAssertion {

    private FreeIpaLdapTestAssertion() {

    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validate() {
        return (testContext, entity, freeIpaClient) -> {
            DescribeLdapConfigResponse ldapConfigResponse =
                    freeIpaClient.getDefaultClient().getLdapConfigV1Endpoint().describe(entity.getResponse().getEnvironmentCrn());
            assertNotNull(ldapConfigResponse);
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(), ldapConfigResponse.getDomain().toUpperCase());
            return entity;
        };
    }
}
