package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@Component
public class UpgradeEmbeddedDBPreparationStateParamsProvider {

    private static final String ORIGINAL_POSTGRES_VERSION_KEY = "original_postgres_version";

    private static final String ORIGINAL_POSTGRES_BINARIES_KEY = "original_postgres_binaries";

    private static final String TEMP_DIRECTORY_KEY = "temp_directory";

    private static final String NEW_POSTGRES_VERSION_KEY = "new_postgres_version";

    private static final String DEFAULT_ORIGINAL_POSTGRES_VERSION = "10";

    @Value("${cb.db.env.upgrade.embedded.targetversion}")
    private TargetMajorVersion targetMajorVersion;

    public Map<String, Object> createParamsForEmbeddedDBUpgradePreparation(StackDto stackDto) {
        String originalVersion = StringUtils.isNotEmpty(stackDto.getStack().getExternalDatabaseEngineVersion())
                ? stackDto.getStack().getExternalDatabaseEngineVersion() : DEFAULT_ORIGINAL_POSTGRES_VERSION;
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> postgresParams = new HashMap<>();
        params.put("postgres", postgresParams);
        Map<String, String> upgradeParams = new HashMap<>();
        postgresParams.put("upgrade", upgradeParams);
        upgradeParams.put(ORIGINAL_POSTGRES_VERSION_KEY, originalVersion);
        upgradeParams.put(ORIGINAL_POSTGRES_BINARIES_KEY, "/usr/pgsql-" + originalVersion);
        upgradeParams.put(TEMP_DIRECTORY_KEY, VolumeUtils.DATABASE_VOLUME + "/tmp");
        upgradeParams.put(NEW_POSTGRES_VERSION_KEY, targetMajorVersion.getMajorVersion());
        return params;
    }
}
