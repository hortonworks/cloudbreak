package com.sequenceiq.it.cloudbreak.newway.assertion.kerberos;

import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;

public class KerberosTestAssertion {

    private KerberosTestAssertion() {

    }

    public static AssertionV2<StackEntity> validateCustomDomain(String realm) {
        return (testContext, entity, cloudbreakClient) -> {
            if (!realm.equals(entity.getResponse().getCustomDomains().getDomainName())) {
                throw new IllegalArgumentException(String.format("Custom domain name is not equals with kerberos realm. Expected: %s, got: %s", realm,
                        entity.getResponse().getCustomDomains().getDomainName()));
            }
            return entity;
        };
    }
}
