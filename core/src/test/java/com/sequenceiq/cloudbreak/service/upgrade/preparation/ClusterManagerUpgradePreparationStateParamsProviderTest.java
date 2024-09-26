package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@ExtendWith(MockitoExtension.class)
class ClusterManagerUpgradePreparationStateParamsProviderTest {

    @InjectMocks
    private ClusterManagerUpgradePreparationStateParamsProvider underTest;

    @Mock
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @Test
    void testCreateParamsForCmPackageDownload() {
        Image image = Image.builder().build();
        String pillarKey = "cloudera-manager-upgrade-prepare";
        String version = "7.2.0";
        String baseUrl = "http://cloudera-manager-repo";
        String gpgKeyUrl = "http://cloudera-manager-repo/gpgkey";
        String buildNumber = "1234";
        ClouderaManagerRepo repo = new ClouderaManagerRepo()
                .withVersion(version)
                .withBaseUrl(baseUrl)
                .withGpgKeyUrl(gpgKeyUrl)
                .withBuildNumber(buildNumber);

        when(imageToClouderaManagerRepoConverter.convert(image)).thenReturn(repo);

        Map<String, SaltPillarProperties> actual = underTest.createParamsForCmPackageDownload(image);

        assertEquals(pillarKey, actual.keySet().iterator().next());
        SaltPillarProperties saltPillarProperties = actual.get(pillarKey);
        assertEquals("/cloudera-manager/repo-prepare.sls", saltPillarProperties.getPath());
        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertEquals(1, properties.size());
        Map<String, Object> clouderaManagerUpgradePrepare = (Map<String, Object>) properties.get("cloudera-manager-upgrade-prepare");
        assertEquals(1, clouderaManagerUpgradePrepare.size());
        ClouderaManagerRepo clouderaManagerRepo = (ClouderaManagerRepo) clouderaManagerUpgradePrepare.get("repo");
        assertEquals(version, clouderaManagerRepo.getVersion());
        assertEquals(baseUrl, clouderaManagerRepo.getBaseUrl());
        assertEquals(gpgKeyUrl, clouderaManagerRepo.getGpgKeyUrl());
        assertEquals(buildNumber, clouderaManagerRepo.getBuildNumber());
    }

}