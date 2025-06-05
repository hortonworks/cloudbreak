package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

public class FreeIpaConfigView {

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final String realm;

    private final String domain;

    private final String password;

    private final String reverseZones;

    private final String adminUser;

    private final String freeipaToReplicate;

    private final String freeipaToReplicateIp;

    private final Set<Object> hosts;

    private final FreeIpaBackupConfigView backup;

    private final boolean dnssecValidationEnabled;

    private final boolean ccmv2Enabled;

    private final List<String> cidrBlocks;

    private final boolean ccmv2JumpgateEnabled;

    private final boolean secretEncryptionEnabled;

    private final String kerberosSecretLocation;

    private final String seLinux;

    private final String tlsVersionsSpaceSeparated;

    private final String tlsVersionsCommaSeparated;

    private final String tlsCipherSuites;

    private final String tlsCipherSuitesRedHat8;

    private final FreeIpaLbConfigView lbConfig;

    @SuppressWarnings("ExecutableStatementCount")
    private FreeIpaConfigView(Builder builder) {
        this.realm = builder.realm;
        this.domain = builder.domain;
        this.password = builder.password;
        this.dnssecValidationEnabled = builder.dnssecValidationEnabled;
        this.reverseZones = builder.reverseZones;
        this.adminUser = builder.adminUser;
        this.freeipaToReplicate = builder.freeipaToReplicate;
        this.freeipaToReplicateIp = builder.freeipaToReplicateIp;
        this.hosts = builder.hosts;
        this.backup = builder.backup;
        this.ccmv2Enabled = builder.ccmv2Enabled;
        this.cidrBlocks = builder.cidrBlocks;
        this.ccmv2JumpgateEnabled = builder.ccmv2JumpgateEnabled;
        this.secretEncryptionEnabled = builder.secretEncryptionEnabled;
        this.kerberosSecretLocation = builder.kerberosSecretLocation;
        this.seLinux = builder.seLinux;
        this.tlsVersionsSpaceSeparated = builder.tlsVersionsSpaceSeparated;
        this.tlsVersionsCommaSeparated = builder.tlsVersionsCommaSeparated;
        this.tlsCipherSuites = builder.tlsCipherSuites;
        this.tlsCipherSuitesRedHat8 = builder.tlsCipherSuitesRedHat8;
        this.lbConfig = builder.lbConfig;
    }

    public String getRealm() {
        return realm;
    }

    public String getDomain() {
        return domain;
    }

    public String getPassword() {
        return password;
    }

    public boolean isDnssecValidationEnabled() {
        return dnssecValidationEnabled;
    }

    public String getReverseZones() {
        return reverseZones;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getFreeipaToReplicate() {
        return freeipaToReplicate;
    }

    public String getFreeipaToReplicateIp() {
        return freeipaToReplicateIp;
    }

    public Set<Object> getHosts() {
        return hosts;
    }

    public FreeIpaBackupConfigView getBackup() {
        return backup;
    }

    public List<String> getCidrBlocks() {
        return cidrBlocks;
    }

    public boolean isCcmv2Enabled() {
        return ccmv2Enabled;
    }

    public boolean isCcmv2JumpgateEnabled() {
        return ccmv2JumpgateEnabled;
    }

    public boolean isSecretEncryptionEnabled() {
        return secretEncryptionEnabled;
    }

    public String getKerberosSecretLocation() {
        return kerberosSecretLocation;
    }

    public String getSeLinux() {
        return seLinux;
    }

    public String getTlsVersionsSpaceSeparated() {
        return tlsVersionsSpaceSeparated;
    }

    public String getTlsVersionsCommaSeparated() {
        return tlsVersionsCommaSeparated;
    }

    @SuppressWarnings("ExecutableStatementCount")
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("realm", ObjectUtils.defaultIfNull(this.realm, EMPTY_CONFIG_DEFAULT));
        map.put("domain", ObjectUtils.defaultIfNull(this.domain, EMPTY_CONFIG_DEFAULT));
        map.put("password", ObjectUtils.defaultIfNull(this.password, EMPTY_CONFIG_DEFAULT));
        map.put("dnssecValidationEnabled", this.dnssecValidationEnabled);
        map.put("reverseZones", ObjectUtils.defaultIfNull(this.reverseZones, EMPTY_CONFIG_DEFAULT));
        map.put("admin_user", ObjectUtils.defaultIfNull(this.adminUser, EMPTY_CONFIG_DEFAULT));
        map.put("freeipa_to_replicate", ObjectUtils.defaultIfNull(this.freeipaToReplicate, EMPTY_CONFIG_DEFAULT));
        map.put("freeipa_to_replicate_ip", ObjectUtils.defaultIfNull(this.freeipaToReplicateIp, EMPTY_CONFIG_DEFAULT));
        map.put("enable_ccmv2", this.ccmv2Enabled);
        map.put("enable_ccmv2_jumpgate", this.ccmv2JumpgateEnabled);
        if (MapUtils.isNotEmpty(backup.toMap())) {
            map.put("backup", this.backup.toMap());
        }
        if (CollectionUtils.isNotEmpty(this.hosts)) {
            map.put("hosts", this.hosts);
        }
        map.put("cidrBlocks", cidrBlocks);
        map.put("secretEncryptionEnabled", secretEncryptionEnabled);
        map.put("kerberosSecretLocation", kerberosSecretLocation);
        map.put("selinux_mode", seLinux);
        map.put("tlsv13Enabled", false);
        map.put("tlsVersionsSpaceSeparated", tlsVersionsSpaceSeparated);
        map.put("tlsVersionsCommaSeparated", tlsVersionsCommaSeparated);
        map.put("tlsCipherSuites", tlsCipherSuites);
        map.put("tlsCipherSuitesRedHat8", tlsCipherSuitesRedHat8);
        map.put("loadBalancer", lbConfig.toMap());
        return map;
    }

    public static final class Builder {

        private String realm;

        private String domain;

        private String password;

        private String reverseZones;

        private String adminUser;

        private String freeipaToReplicate;

        private String freeipaToReplicateIp;

        private Set<Object> hosts;

        private boolean dnssecValidationEnabled;

        private FreeIpaBackupConfigView backup;

        private boolean ccmv2Enabled;

        private List<String> cidrBlocks;

        private boolean ccmv2JumpgateEnabled;

        private boolean secretEncryptionEnabled;

        private String kerberosSecretLocation;

        private String seLinux;

        private String tlsVersionsSpaceSeparated;

        private String tlsVersionsCommaSeparated;

        private String tlsCipherSuites;

        private String tlsCipherSuitesRedHat8;

        private FreeIpaLbConfigView lbConfig = new FreeIpaLbConfigView();

        public FreeIpaConfigView build() {
            return new FreeIpaConfigView(this);
        }

        public Builder withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        public Builder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withDnssecValidationEnabled(boolean dnssecEnabled) {
            this.dnssecValidationEnabled = dnssecEnabled;
            return this;
        }

        public Builder withReverseZones(String reverseZones) {
            this.reverseZones = reverseZones;
            return this;
        }

        public Builder withAdminUser(String adminUser) {
            this.adminUser = adminUser;
            return this;
        }

        public Builder withFreeIpaToReplicate(GatewayConfig freeipaToReplicate) {
            this.freeipaToReplicate = freeipaToReplicate.getHostname();
            this.freeipaToReplicateIp = freeipaToReplicate.getPrivateAddress();
            return this;
        }

        public Builder withHosts(Set<Node> hosts) {
            // IP is needed for backwards compatibility with FreeIPA HA prior to 2.28.0
            this.hosts = hosts.stream().map(n ->
                    Map.of("ip", n.getPrivateIp(),
                            "fqdn", n.getHostname()))
                    .collect(Collectors.toSet());
            return this;
        }

        public Builder withBackupConfig(FreeIpaBackupConfigView backupConfig) {
            this.backup = backupConfig;
            return this;
        }

        public Builder withCcmv2Enabled(boolean ccmv2Enabled) {
            this.ccmv2Enabled = ccmv2Enabled;
            return this;
        }

        public Builder withCidrBlocks(List<String> cidrBlocks) {
            this.cidrBlocks = cidrBlocks;
            return this;
        }

        public Builder withCcmv2JumpgateEnabled(boolean ccmv2JumpgateEnabled) {
            this.ccmv2JumpgateEnabled = ccmv2JumpgateEnabled;
            return this;
        }

        public Builder withSecretEncryptionEnabled(boolean secretEncryptionEnabled) {
            this.secretEncryptionEnabled = secretEncryptionEnabled;
            return this;
        }

        public Builder withTlsVersionsSpaceSeparated(String tlsVersions) {
            this.tlsVersionsSpaceSeparated = tlsVersions;
            return this;
        }

        public Builder withTlsVersionsCommaSeparated(String tlsVersions) {
            this.tlsVersionsCommaSeparated = tlsVersions;
            return this;
        }

        public Builder withTlsCipherSuites(String tlsCipherSuites) {
            this.tlsCipherSuites = tlsCipherSuites;
            return this;
        }

        public Builder withTlsCipherSuitesRedHat8(String tlsCipherSuitesRedHat8) {
            this.tlsCipherSuitesRedHat8 = tlsCipherSuitesRedHat8;
            return this;
        }

        public Builder withKerberosSecretLocation(String kerberosSecretLocation) {
            this.kerberosSecretLocation = kerberosSecretLocation;
            return this;
        }

        public Builder withSeLinux(String seLinux) {
            this.seLinux = seLinux;
            return this;
        }

        public Builder withLbConfig(FreeIpaLbConfigView lbConfig) {
            this.lbConfig = lbConfig;
            return this;
        }
    }
}
