package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultPathBuilder {

    private static final String VAULT_SECRET_TYPE = "ServiceKeytab";

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtV1Service.class);

    private boolean generateClusterCrnIfNotPresent;

    private Optional<String> accountId = Optional.empty();

    private Optional<String> subType = Optional.empty();

    private Optional<String> environmentCrn = Optional.empty();

    private Optional<String> clusterCrn = Optional.empty();

    private Optional<String> serverHostName = Optional.empty();

    private Optional<String> serviceName = Optional.empty();

    public VaultPathBuilder enableGeneratingClusterCrnIfNotPresent() {
        generateClusterCrnIfNotPresent = true;
        return this;
    }

    public VaultPathBuilder withAccountId(String accountId) {
        this.accountId = Optional.ofNullable(accountId);
        return this;
    }

    public VaultPathBuilder withSubType(String subType) {
        this.subType = Optional.ofNullable(subType);
        return this;
    }

    public VaultPathBuilder withEnvironmentCrn(String environmentCrn) {
        String envCrn = StringUtils.substringAfterLast(environmentCrn, ":");
        this.environmentCrn = Optional.ofNullable(envCrn);
        return this;
    }

    public VaultPathBuilder withClusterCrn(String clusterCrn) {
        String crn = StringUtils.substringAfterLast(clusterCrn, ":");
        this.clusterCrn = Optional.ofNullable(crn);
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
        // Sample Vault Path "/enginePath/appPath/account-id/Type/SubType/envCrn/clusterCrn/hostname/serviceName"
        StringBuilder ret = new StringBuilder();
        List<Optional<String>> requiredEntries = Arrays.asList(
                accountId,
                Optional.of(VAULT_SECRET_TYPE),
                subType,
                environmentCrn
        );

        requiredEntries.stream().forEachOrdered(entry -> {
            ret.append(entry.orElseThrow(() -> new IllegalStateException("Missing required vault path entry.")));
            ret.append("/");
        });

        if (clusterCrn.isPresent() || generateClusterCrnIfNotPresent) {
            ret.append(clusterCrn.orElseGet(() -> {
                LOGGER.debug("Cluster CRN not provided. Auto-generating one");
                return generateClusterCrn(accountId.get(), environmentCrn.get());
            }));
            ret.append("/");

            if (serverHostName.isPresent()) {
                ret.append(serverHostName.get());
                ret.append("/");
                ret.append(serviceName.orElse(""));
            }
        }

        LOGGER.debug("Generated vault path: [{}]", ret);
        return ret.toString();
    }

    private String generateClusterCrn(String accountId, String envCrn) {
        return accountId + "-" + envCrn;
    }

}
