package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class ClusterManagerUpgradePreparationStateParamsProviderTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ClusterManagerUpgradePreparationStateParamsProvider underTest;

    @Mock
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @Mock
    private OsChangeService osChangeService;

    @Mock
    private StackImageService stackImageService;

    @Test
    void testCreateParamsForCmPackageDownload() throws CloudbreakImageNotFoundException {
        Image targetImage = Image.builder().withOsType(OsType.RHEL9.getOsType()).build();
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

        when(imageToClouderaManagerRepoConverter.convert(targetImage)).thenReturn(repo);
        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(
                com.sequenceiq.cloudbreak.cloud.model.Image.builder().withOsType(OsType.RHEL8.getOsType()).withArchitecture(Architecture.X86_64.getName())
                        .build());
        when(osChangeService.updateCmRepoInCaseOfOsChange(repo, OsType.RHEL8, OsType.RHEL9, Architecture.X86_64.getName())).thenReturn(repo);

        Map<String, SaltPillarProperties> actual = underTest.createParamsForCmPackageDownload(targetImage, STACK_ID);

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
        verify(osChangeService).updateCmRepoInCaseOfOsChange(clouderaManagerRepo, OsType.RHEL8, OsType.RHEL9, Architecture.X86_64.getName());
    }

    @Test
    void testCreateParamsForCmPackageDownloadShouldThrowExceptionWhenImageNotFound() throws CloudbreakImageNotFoundException {
        Image targetImage = Image.builder().withOsType(OsType.RHEL9.getOsType()).build();
        doThrow(CloudbreakImageNotFoundException.class).when(stackImageService).getCurrentImage(STACK_ID);

        String errorMessage = assertThrows(NotFoundException.class, () -> underTest.createParamsForCmPackageDownload(targetImage, STACK_ID)).getMessage();

        assertEquals("Image not found for stack", errorMessage);
    }

}