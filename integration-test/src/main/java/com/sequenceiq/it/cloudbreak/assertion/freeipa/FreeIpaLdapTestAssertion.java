package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;

public class FreeIpaLdapTestAssertion {

    private FreeIpaLdapTestAssertion() {

    }

    public static Assertion<FreeIPATestDto, FreeIPAClient> validate() {
        return (testContext, entity, freeIPAClient) -> {
            DescribeLdapConfigResponse ldapConfigResponse =
                    freeIPAClient.getFreeIpaClient().getLdapConfigV1Endpoint().describe(entity.getResponse().getEnvironmentCrn());
            assertNotNull(ldapConfigResponse);
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(), ldapConfigResponse.getDomain().toUpperCase());
            return entity;
        };
    }
}
