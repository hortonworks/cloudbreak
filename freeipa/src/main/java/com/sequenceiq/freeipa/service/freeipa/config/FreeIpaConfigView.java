package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;

public class FreeIpaConfigView {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaConfigView.class);

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final String realm;

    private final String domain;

    private final String password;

    private final String reverseZones;

    private final String adminUser;

    private final String freeipaToReplicate;

    private final Set<Object> hosts;

    private final FreeIpaBackupConfigView backup;

    @SuppressWarnings("ExecutableStatementCount")
    private FreeIpaConfigView(Builder builder) {
        this.realm = builder.realm;
        this.domain = builder.domain;
        this.password = builder.password;
        this.reverseZones = builder.reverseZones;
        this.adminUser = builder.adminUser;
        this.freeipaToReplicate = builder.freeipaToReplicate;
        this.hosts = builder.hosts;
        this.backup = builder.backup;
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

    public String getReverseZones() {
        return reverseZones;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getFreeipaToReplicate() {
        return freeipaToReplicate;
    }

    public Set<Object> getHosts() {
        return hosts;
    }

    public FreeIpaBackupConfigView getBackup() {
        return backup;
    }

    @SuppressWarnings("ExecutableStatementCount")
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("realm", ObjectUtils.defaultIfNull(this.realm, EMPTY_CONFIG_DEFAULT));
        map.put("domain", ObjectUtils.defaultIfNull(this.domain, EMPTY_CONFIG_DEFAULT));
        map.put("password", ObjectUtils.defaultIfNull(this.password, EMPTY_CONFIG_DEFAULT));
        map.put("reverseZones", ObjectUtils.defaultIfNull(this.reverseZones, EMPTY_CONFIG_DEFAULT));
        map.put("admin_user", ObjectUtils.defaultIfNull(this.adminUser, EMPTY_CONFIG_DEFAULT));
        map.put("freeipa_to_replicate", ObjectUtils.defaultIfNull(this.freeipaToReplicate, EMPTY_CONFIG_DEFAULT));
        if (MapUtils.isNotEmpty(backup.toMap())) {
            map.put("backup", this.backup.toMap());
        }
        if (CollectionUtils.isNotEmpty(this.hosts)) {
            map.put("hosts", this.hosts);
        }
        return map;
    }

    public static final class Builder {

        private String realm;

        private String domain;

        private String password;

        private String reverseZones;

        private String adminUser;

        private String freeipaToReplicate;

        private Set<Object> hosts;

        private FreeIpaBackupConfigView backup;

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

        public Builder withReverseZones(String reverseZones) {
            this.reverseZones = reverseZones;
            return this;
        }

        public Builder withAdminUser(String adminUser) {
            this.adminUser = adminUser;
            return this;
        }

        public Builder withFreeIpaToReplicate(String freeipaToReplicate) {
            this.freeipaToReplicate = freeipaToReplicate;
            return this;
        }

        public Builder withHosts(Set<Node> hosts) {
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
    }
}
