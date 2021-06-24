package com.sequenceiq.authorization.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum LegacyResourceTypes {

    DATAHUB_RESOURCE_TYPE("datahub"),
    DATALAKE_RESOURCE_TYPE("datalake"),
    ENVIRONMENT_RESOURCE_TYPE("environment"),
    FREEIPA_RESOURCE_TYPE("freeipa"),
    KERBEROS_RESOURCE_TYPE("kerberos"),
    LDAP_RESOURCE_TYPE("ldap"),
    CREDENTIAL_RESOURCE_TYPE("credential"),
    LEGACY_RESOURCE_TYPE("stacks");

    private final String value;

    LegacyResourceTypes(String value) {
        this.value = value;
    }

    public String getTypeValue() {
        return value;
    }

    public static Set<String> getTypeValues() {
        return Arrays.asList(values()).stream().map(legacyResourceActionTypes -> legacyResourceActionTypes.getTypeValue()).collect(Collectors.toSet());
    }

}
