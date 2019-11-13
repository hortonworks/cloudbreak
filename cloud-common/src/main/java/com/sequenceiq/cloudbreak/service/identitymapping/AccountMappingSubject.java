package com.sequenceiq.cloudbreak.service.identitymapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AccountMappingSubject {

    /**
     * Immutable set of data access service users. Always disjoint from {@link #RANGER_AUDIT_USERS}.
     *
     * Adapted from {@code com.cloudera.thunderhead.service.idbrokermappingmanagement.server.IdBrokerMappingManagementService.DATA_ACCESS_SERVICES}.
     */
    public static final Set<String> DATA_ACCESS_USERS = Set.of("hbase", "hdfs", "hive", "impala", "yarn", "dpprofiler", "zeppelin", "kudu", "hue");

    /**
     * Immutable set of service users that are not data access ones but do need Ranger Audit access. Always disjoint from {@link #DATA_ACCESS_USERS}.
     *
     * Adapted from {@code com.cloudera.thunderhead.service.idbrokermappingmanagement.server.IdBrokerMappingManagementService.BASELINE_SERVICES}.
     */
    public static final Set<String> RANGER_AUDIT_USERS = Set.of("kafka", "solr", "knox", "atlas");

    /**
     * Convenience immutable set comprising the union of {@link #DATA_ACCESS_USERS} and {@link #RANGER_AUDIT_USERS}.
     */
    public static final Set<String> DATA_ACCESS_AND_RANGER_AUDIT_USERS;

    static {
        Set<String> dataAccessAndRangerAuditUsers = new HashSet<>(DATA_ACCESS_USERS);
        dataAccessAndRangerAuditUsers.addAll(RANGER_AUDIT_USERS);
        DATA_ACCESS_AND_RANGER_AUDIT_USERS = Collections.unmodifiableSet(dataAccessAndRangerAuditUsers);
    }

    private AccountMappingSubject() {
        // Prohibit instantiation
    }

}
