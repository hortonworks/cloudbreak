package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;

import static java.util.Collections.singletonMap;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BackupRestoreSaltConfigGenerator {

    public SaltConfig createSaltConfig(String location, String backupId, String cloudPlatform) throws URISyntaxException {
        String fullLocation = buildFullLocation(location, backupId, cloudPlatform);

        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        servicePillar.put("disaster-recovery", new SaltPillarProperties("/postgresql/disaster_recovery.sls",
            singletonMap("disaster_recovery", singletonMap("object_storage_url", fullLocation))));
        return new SaltConfig(servicePillar);
    }

    String buildFullLocation(String location, String backupId, String cloudPlatform) throws URISyntaxException {
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
