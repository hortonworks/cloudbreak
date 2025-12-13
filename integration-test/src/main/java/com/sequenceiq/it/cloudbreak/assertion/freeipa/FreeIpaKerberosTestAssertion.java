package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaKerberosTestAssertion {

    private FreeIpaKerberosTestAssertion() {

    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validate() {
        return (testContext, entity, freeIpaClient) -> {
            DescribeKerberosConfigResponse kerberosConfigResponse =
                    freeIpaClient.getDefaultClient().getKerberosConfigV1Endpoint().describe(entity.getResponse().getEnvironmentCrn());
            assertNotNull(kerberosConfigResponse);
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(Locale.ROOT),
                    kerberosConfigResponse.getDomain().toUpperCase(Locale.ROOT));
            assertEquals(entity.getRequest().getFreeIpa().getDomain().toUpperCase(Locale.ROOT),
                    kerberosConfigResponse.getRealm().toUpperCase(Locale.ROOT));
            return entity;
        };
    }
}
