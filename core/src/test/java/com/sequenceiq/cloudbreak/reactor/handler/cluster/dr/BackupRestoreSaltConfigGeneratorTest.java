package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static org.junit.Assert.assertEquals;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;

import java.net.URISyntaxException;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {BackupRestoreSaltConfigGenerator.class})
public class BackupRestoreSaltConfigGeneratorTest {

    private static final String BACKUP_ID = "backupId";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Inject
    private BackupRestoreSaltConfigGenerator saltConfigGenerator;

    @Test
    public void testCreateSaltConfig() throws URISyntaxException {
        String cloudPlatform = "aws";
        String location = "/test/backups";
        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, cloudPlatform);
        Map<String, Object> properties = saltConfig.getServicePillarConfig().values().iterator().next().getProperties();
        assertEquals(1, properties.size());
        Object object = properties.values().iterator().next();
        assert object instanceof Map;
        Map<String, String> map = (Map<String, String>) object;
        assertEquals(1, map.size());
        assertEquals("object_storage_url", map.keySet().iterator().next());
        assertEquals("s3://test/backups/backupId_database_backup", map.values().iterator().next());
    }
}
