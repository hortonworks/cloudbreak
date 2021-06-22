package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class VaultPathBuilder {

    public enum SecretType {
        SERVICE_KEYTAB("ServiceKeytab"),
        HOST_KEYTAB("HostKeytab");

        private final String value;

        SecretType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum SecretSubType {
        KEYTAB("keytab"),
        SERVICE_PRINCIPAL("serviceprincipal");

        private final String value;

        SecretSubType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Service.class);

    private Optional<SecretType> secretType = Optional.empty();

    private Optional<String> accountId = Optional.empty();

    private Optional<SecretSubType> subType = Optional.empty();

    private Optional<String> environmentId = Optional.empty();

    private Optional<String> clusterId = Optional.empty();

    private Optional<String> serverHostName = Optional.empty();

    private Optional<String> serviceName = Optional.empty();

    public VaultPathBuilder withSecretType(SecretType secretType) {
        this.secretType = Optional.ofNullable(secretType);
        return this;
    }

    public VaultPathBuilder withAccountId(String accountId) {
        this.accountId = Optional.ofNullable(accountId);
        return this;
    }

    public VaultPathBuilder withSubType(SecretSubType subType) {
        this.subType = Optional.ofNullable(subType);
        return this;
    }

    public VaultPathBuilder withEnvironmentCrn(String environmentCrn) {
        this.environmentId = Optional.ofNullable(environmentCrn).map((String crn) -> {
            return Crn.safeFromString(crn).getResource();
        });
        return this;
    }

    public VaultPathBuilder withClusterCrn(String clusterCrn) {
        this.clusterId = Optional.ofNullable(clusterCrn).map((String crn) -> {
            return Crn.safeFromString(crn).getResource();
        });
        return this;
    }

    public VaultPathBuilder withServerHostName(String serverHostName) {
        this.serverHostName = Optional.ofNullable(serverHostName);
        return this;
    }

    public VaultPathBuilder withServiceName(String serviceName) {
        this.serviceName = Optional.ofNullable(serviceName);
        return this;
    }

    public String build() {
        // Sample Vault Path "/enginePath/appPath/account-id/Type/SubType/envId (/clusterId|IF_PRESENT) /hostname/serviceName"
        StringBuilder ret = new StringBuilder();
        List<Optional<String>> requiredEntries = Arrays.asList(
                accountId,
                secretType.map(SecretType::toString),
                subType.map(SecretSubType::toString),
                environmentId
        );

        requiredEntries.stream().forEachOrdered(entry -> {
            ret.append(entry.orElseThrow(() -> new IllegalStateException("Missing required vault path entry.")));
            ret.append("/");
        });

        if (clusterId.isPresent()) {
            ret.append(clusterId.get());
            ret.append("/");

        }

        if (serverHostName.isPresent()) {
            ret.append(serverHostName.get());
            if (secretType.get() == SecretType.SERVICE_KEYTAB) {
                ret.append("/");
                ret.append(serviceName.orElse(""));
            }
        }

        LOGGER.debug("Generated vault path: [{}]", ret);
        return ret.toString();
    }
}
