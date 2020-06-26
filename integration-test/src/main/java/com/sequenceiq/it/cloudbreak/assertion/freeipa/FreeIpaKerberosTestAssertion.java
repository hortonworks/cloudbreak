package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;

public class FreeIpaKerberosTestAssertion {

    private FreeIpaKerberosTestAssertion() {

    }

    public static Assertion<FreeIPATestDto, FreeIPAClient> validate() {
        return (testContext, entity, freeIPAClient) -> {
            DescribeKerberosConfigResponse kerberosConfigResponse =
                    freeIPAClient.getFreeIpaClient().getKerberosConfigV1Endpoint().describe(entity.getResponse().getEnvironmentCrn());
            assertNotNull(kerberosConfigResponse);
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(), kerberosConfigResponse.getDomain().toUpperCase());
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(), kerberosConfigResponse.getRealm().toUpperCase());
            return entity;
        };
    }
}
