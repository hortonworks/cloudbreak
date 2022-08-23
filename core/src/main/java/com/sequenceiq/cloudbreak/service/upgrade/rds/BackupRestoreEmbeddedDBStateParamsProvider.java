package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BackupRestoreEmbeddedDBStateParamsProvider {

    private static final String EMBEDDED_DB_HOST_KEY = "embeddeddb_host";

    private static final String EMBEDDED_DB_PORT_KEY = "embeddeddb_port";

    private static final String EMBEDDED_DB_USER_KEY = "embeddeddb_user";

    private static final String EMBEDDED_DB_PASSWORD_KEY = "embeddeddb_password";

    public Map<String, Object> createParamsForBackupRestore() {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> postgresParams = new HashMap<>();
        params.put("postgres", postgresParams);
        Map<String, String> upgradeParams = new HashMap<>();
        postgresParams.put("upgrade", upgradeParams);
        upgradeParams.put(EMBEDDED_DB_HOST_KEY, "localhost");
        upgradeParams.put(EMBEDDED_DB_PORT_KEY, "5432");
        upgradeParams.put(EMBEDDED_DB_USER_KEY, "postgres");
        upgradeParams.put(EMBEDDED_DB_PASSWORD_KEY, "postgres");
        return params;
    }
}
