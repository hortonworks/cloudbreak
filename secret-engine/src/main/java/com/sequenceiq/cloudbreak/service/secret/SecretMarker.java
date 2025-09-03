package com.sequenceiq.cloudbreak.service.secret;

public enum SecretMarker {
    // cluster manager secrets
    CB_CLUSTER_MANAGER_USER,
    CB_CLUSTER_MANAGER_PASSWORD,
    DP_CLUSTER_MANAGER_USER,
    DP_CLUSTER_MANAGER_PASSWORD,
    // freeipa secrets
    FREEIPA_ADMIN_PASSWORD,
    // external db secrets
    DBSTACK_ROOT_PWD,
    DBSERVER_CONFIG_ROOT_PWD,
    // freeipa user management secrets
    LDAP_CONFIG_BIND_PWD,
    KERBEROS_CONFIG_BIND_USER_PWD,
    // compute monitoring secrets
    NODE_STATUS_MONITOR_PWD,
    CLUSTER_MANAGER_MONITORING_PWD,
    CLUSTER_MANAGER_MONITORING_USER,
    // salt secrets
    SALT_PASSWORD,
    SALT_SIGN_PRIVATE_KEY,
    SALT_MASTER_PRIVATE_KEY,
    SALT_BOOT_PASSWORD,
    SALT_BOOT_SIGN_PRIVATE_KEY,
    // internal database secrets
    RDS_CONFIG_USERNAME,
    RDS_CONFIG_PASSWORD,
    // idbroker secrets
    IDBROKER_SIGN_KEY,
    IDBROKER_SIGN_PUB,
    IDBROKER_SIGN_CERT,
    // gateway secrets
    GATEWAY_SIGN_KEY,
    GATEWAY_SIGN_PUB,
    GATEWAY_SIGN_CERT,
    GATEWAY_TOKEN_CERT,
    GATEWAY_TOKEN_PUB,
    GATEWAY_TOKEN_KEY
}
