package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class ClusterManagerUpgradePreparationStateParamsProviderTest {

    @InjectMocks
    private ClusterManagerUpgradePreparationStateParamsProvider underTest;

    @Mock
    private OsChangeService osChangeService;

    @Test
    void testCreateParamsForCmPackageDownload() {
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

        ClusterUpgradeProperties upgradeProperties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base-image", OsType.RHEL9, "x86_64", null, Set.of(), repo);
        when(osChangeService.updateCmRepoInCaseOfOsChange(any(), eq(OsType.RHEL8), eq(OsType.RHEL9), eq(Architecture.X86_64.getName())))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, SaltPillarProperties> actual = underTest.createParamsForCmPackageDownload(upgradeProperties);

        assertEquals(pillarKey, actual.keySet().iterator().next());
        SaltPillarProperties saltPillarProperties = actual.get(pillarKey);
        assertEquals("/cloudera-manager/repo-prepare.sls", saltPillarProperties.getPath());
        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertEquals(1, properties.size());
        Map<String, Object> clouderaManagerUpgradePrepare = (Map<String, Object>) properties.get("cloudera-manager-upgrade-prepare");
        assertEquals(1, clouderaManagerUpgradePrepare.size());
        ClouderaManagerRepo clouderaManagerRepo = (ClouderaManagerRepo) clouderaManagerUpgradePrepare.get("repo");
        assertNotSame(repo, clouderaManagerRepo);
        assertEquals(version, clouderaManagerRepo.getVersion());
        assertEquals(baseUrl, clouderaManagerRepo.getBaseUrl());
        assertEquals(gpgKeyUrl, clouderaManagerRepo.getGpgKeyUrl());
        assertEquals(buildNumber, clouderaManagerRepo.getBuildNumber());
        verify(osChangeService).updateCmRepoInCaseOfOsChange(clouderaManagerRepo, OsType.RHEL8, OsType.RHEL9, Architecture.X86_64.getName());
    }

    @Test
    void testOsChangeDoesNotModifyTargetRepository() {
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withBaseUrl("target-url").withGpgKeyUrl("target-key");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base-image", OsType.RHEL9, "x86_64", null, Set.of(), repo);
        when(osChangeService.updateCmRepoInCaseOfOsChange(any(), eq(OsType.RHEL8), eq(OsType.RHEL9), eq("x86_64")))
                .thenAnswer(invocation -> ((ClouderaManagerRepo) invocation.getArgument(0)).withBaseUrl("current-os-url"));

        Map<String, SaltPillarProperties> actual = underTest.createParamsForCmPackageDownload(properties);

        Map<?, ?> pillar = (Map<?, ?>) actual.get("cloudera-manager-upgrade-prepare").getProperties().get("cloudera-manager-upgrade-prepare");
        assertEquals("current-os-url", ((ClouderaManagerRepo) pillar.get("repo")).getBaseUrl());
        assertEquals("target-url", repo.getBaseUrl());
    }
}
