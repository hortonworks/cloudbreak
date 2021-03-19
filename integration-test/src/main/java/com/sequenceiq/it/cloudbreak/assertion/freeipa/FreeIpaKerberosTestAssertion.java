package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaKerberosTestAssertion {

    private FreeIpaKerberosTestAssertion() {

    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validate() {
        return (testContext, entity, freeIpaClient) -> {
            DescribeKerberosConfigResponse kerberosConfigResponse =
                    freeIpaClient.getDefaultClient().getKerberosConfigV1Endpoint().describe(entity.getResponse().getEnvironmentCrn());
            assertNotNull(kerberosConfigResponse);
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(), kerberosConfigResponse.getDomain().toUpperCase());
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(), kerberosConfigResponse.getRealm().toUpperCase());
            return entity;
        };
    }
}
