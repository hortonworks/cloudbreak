package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@Component
public class UpgradeEmbeddedDBStateParamsProvider {

    private static final String ORIGINAL_POSTGRES_VERSION_KEY = "original_postgres_version";

    private static final String ORIGINAL_POSTGRES_DIRECTORY_KEY = "original_postgres_directory";

    private static final String ORIGINAL_POSTGRES_BINARIES_KEY = "original_postgres_binaries";

    private static final String NEW_POSTGRES_VERSION_KEY = "new_postgres_version";

    private static final String DEFAULT_ORIGINAL_POSTGRES_VERSION = "10";

    private static final String ORIGINAL_POSTGRES_SUBDIRECTORY = "pgsql";

    private static final String ORIGINAL_POSTGRES_BINARIES_SUBDIRECTORY_PREFIX = "tmp/pgsql-";

    public Map<String, Object> createParamsForEmbeddedDBUpgrade(StackDto stackDto, String targetVersion) {
        String originalVersion = StringUtils.isNotEmpty(stackDto.getExternalDatabaseEngineVersion())
                ? stackDto.getExternalDatabaseEngineVersion() : DEFAULT_ORIGINAL_POSTGRES_VERSION;
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> postgresParams = new HashMap<>();
        params.put("postgres", postgresParams);
        Map<String, String> upgradeParams = new HashMap<>();
        postgresParams.put("upgrade", upgradeParams);
        upgradeParams.put(ORIGINAL_POSTGRES_VERSION_KEY, originalVersion);
        upgradeParams.put(ORIGINAL_POSTGRES_DIRECTORY_KEY, VolumeUtils.DATABASE_VOLUME + "/" + ORIGINAL_POSTGRES_SUBDIRECTORY);
        upgradeParams.put(ORIGINAL_POSTGRES_BINARIES_KEY, VolumeUtils.DATABASE_VOLUME + "/" + ORIGINAL_POSTGRES_BINARIES_SUBDIRECTORY_PREFIX + originalVersion);
        upgradeParams.put(NEW_POSTGRES_VERSION_KEY, targetVersion);
        return params;
    }
}
