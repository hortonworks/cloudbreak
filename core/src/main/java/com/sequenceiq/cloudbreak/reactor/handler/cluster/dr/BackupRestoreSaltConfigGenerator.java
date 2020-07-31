package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;

import static java.util.Collections.singletonMap;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BackupRestoreSaltConfigGenerator {
    // these values are tightly coupled to the `postgresql/disaster_recovery.sls` salt pillar in `orchestrator-salt`
    public static final String POSTGRESQL_DISASTER_RECOVERY_PILLAR_PATH = "/postgresql/disaster_recovery.sls";

    public static final String DISASTER_RECOVERY_KEY = "disaster_recovery";

    public static final String OBJECT_STORAGE_URL_KEY = "object_storage_url";

    public static final String AWS_REGION_KEY = "aws_region";

    public SaltConfig createSaltConfig(String location, String backupId, Stack stack) throws URISyntaxException {
        String fullLocation = buildFullLocation(location, backupId, stack.getCloudPlatform());

        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Map<String, String> disasterRecoveryValues = new HashMap<>();
        disasterRecoveryValues.put(OBJECT_STORAGE_URL_KEY, fullLocation);
        disasterRecoveryValues.put(AWS_REGION_KEY, stack.getRegion());

        servicePillar.put("disaster-recovery", new SaltPillarProperties(POSTGRESQL_DISASTER_RECOVERY_PILLAR_PATH,
            singletonMap(DISASTER_RECOVERY_KEY, disasterRecoveryValues)));

        return new SaltConfig(servicePillar);
    }

    private String buildFullLocation(String location, String backupId, String cloudPlatform) throws URISyntaxException {
        URI uri = new URI(location);
        String suffix = '/' + backupId + "_database_backup";
        String fullLocation;
        if (AWS.equalsIgnoreCase(cloudPlatform)) {
            fullLocation = "s3://" + uri.getSchemeSpecificPart().replaceAll("^/+", "");
        } else if (AZURE.equalsIgnoreCase(cloudPlatform)) {
            fullLocation = "abfs://" + uri.getSchemeSpecificPart().replaceAll("^/+", "");
        } else {
            throw new UnsupportedOperationException("Cloud platform not supported for backup/restore: " + cloudPlatform);
        }
        return fullLocation + suffix;
    }
}
