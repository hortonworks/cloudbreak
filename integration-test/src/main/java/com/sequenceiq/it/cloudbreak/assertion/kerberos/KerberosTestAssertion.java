package com.sequenceiq.it.cloudbreak.assertion.kerberos;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;

public class KerberosTestAssertion {

    private KerberosTestAssertion() {

    }

    public static Assertion<StackTestDto, CloudbreakClient> validateCustomDomain(String realm) {
        return (testContext, entity, cloudbreakClient) -> {
            if (!realm.equals(entity.getResponse().getCustomDomains().getDomainName())) {
                throw new IllegalArgumentException(String.format("Custom domain name is not equals with kerberos realm. Expected: %s, got: %s", realm,
                        entity.getResponse().getCustomDomains().getDomainName()));
            }
            return entity;
        };
    }
}
