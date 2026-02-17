package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.parcel.ParcelAvailabilityRetrievalService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class OsChangeServiceTest {

    private static final int HTTP_OK = 200;

    private static final int HTTP_NOT_FOUND = 404;

    @InjectMocks
    private OsChangeService underTest;

    @Mock
    private ParcelAvailabilityRetrievalService parcelAvailabilityRetrievalService;

    @Test
    void testIsOsChangePermittedShouldReturnTrueWhenTheCurrentOsIsRhel8AndTheTargetIsRhel9AndTheArchIsX86() {
        Image targetImage = createTargetImage(RHEL9);
        Response response = mock(Response.class);
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(anyString())).thenReturn(response);
        when(response.getStatus()).thenReturn(HTTP_OK);

        boolean actual = underTest.isOsChangePermitted(targetImage, RHEL8, Set.of(RHEL8), Architecture.X86_64.getName());

        assertTrue(actual);
    }

    @Test
    void testIsOsChangePermittedShouldReturnTrueWhenTheCurrentOsIsRhel8AndTheTargetIsRhel9AndTheArchIsArm64() {
        Image targetImage = Image.builder()
                .withOs(RHEL9.getOs())
                .withOsType(RHEL9.getOsType())
                .withRepo(Map.of(RHEL9.getOsType(), "http://build-cache-azure.kc.cloudera.com/s3/build/75731052/cm7/7.13.2.0/redhat9arm64/yum/"))
                .build();
        Response response = mock(Response.class);
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(anyString())).thenReturn(response);
        when(response.getStatus()).thenReturn(HTTP_OK);

        boolean actual = underTest.isOsChangePermitted(targetImage, RHEL8, Set.of(RHEL8), Architecture.ARM64.getName());

        assertTrue(actual);
    }

    @Test
    void testIsOsChangePermittedShouldReturnTrueWhenTheCurrentOsIsCentos7AndTheTargetIsRhel8() {
        Image targetImage = createTargetImage(RHEL8);
        Response response = mock(Response.class);
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(anyString())).thenReturn(response);
        when(response.getStatus()).thenReturn(HTTP_OK);

        boolean actual = underTest.isOsChangePermitted(targetImage, CENTOS7, Set.of(CENTOS7), Architecture.X86_64.getName());

        assertTrue(actual);
    }

    @Test
    void testIsOsChangePermittedShouldReturnFalseWhenTheCurrentOsIsCentos7AndTheTargetIsRhel9() {
        Image targetImage = createTargetImage(RHEL9);

        boolean actual = underTest.isOsChangePermitted(targetImage, CENTOS7, Set.of(OsType.CENTOS7), Architecture.X86_64.getName());

        assertFalse(actual);
        verifyNoInteractions(parcelAvailabilityRetrievalService);
    }

    @Test
    void testIsOsChangePermittedShouldReturnFalseWhenTheCurrentOsIsRhel8AndTheTargetIsRhel9AndTheUpdatedRepoIsNotAvailable() {
        Image targetImage = createTargetImage(RHEL9);
        Response response = mock(Response.class);
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(anyString())).thenReturn(response);
        when(response.getStatus()).thenReturn(HTTP_NOT_FOUND);

        boolean actual = underTest.isOsChangePermitted(targetImage, RHEL8, Set.of(RHEL8), Architecture.X86_64.getName());

        assertFalse(actual);
    }

    @Test
    void testIsOsChangePermittedShouldReturnFalseWhenTheCmRepoUrlIsCorrupted() {
        Image targetImage = Image.builder()
                .withOs(RHEL9.getOs())
                .withOsType(RHEL9.getOsType())
                .withRepo(Map.of(RHEL9.getOsType(), "http://build-cache-azure.kc.cloudera.com/s3/build/75731052/cm7/7.13.2.0/yum/"))
                .build();

        boolean actual = underTest.isOsChangePermitted(targetImage, RHEL8, Set.of(RHEL8), Architecture.X86_64.getName());

        assertFalse(actual);
        verifyNoInteractions(parcelAvailabilityRetrievalService);
    }

    @Test
    void testIsOsChangePermittedShouldReturnFalseWhenClusterIsUsingMultipleOSTypes() {
        Image targetImage = createTargetImage(RHEL9);

        boolean actual = underTest.isOsChangePermitted(targetImage, RHEL8, Set.of(CENTOS7, RHEL8), Architecture.X86_64.getName());

        assertFalse(actual);
        verifyNoInteractions(parcelAvailabilityRetrievalService);
    }

    @Test
    void testUpdateCmRepoInCaseOfOsChangeShouldReturnTheUpdatedUrls() {
        String targetRepoUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9/yum/";
        String targetGpgKeyUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9/yum/RPM-GPG-KEY-cloudera";
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo()
                .withBaseUrl(targetRepoUrl)
                .withGpgKeyUrl(targetGpgKeyUrl);

        ClouderaManagerRepo actual = underTest.updateCmRepoInCaseOfOsChange(clouderaManagerRepo, RHEL8, RHEL9, Architecture.X86_64.getName());

        assertEquals("https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat8/yum/", actual.getBaseUrl());
        assertEquals("https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat8/yum/RPM-GPG-KEY-cloudera", actual.getGpgKeyUrl());
    }

    @Test
    void testUpdateCmRepoInCaseOfOsChangeShouldReturnTheUpdatedUrlsWhenTheArchIsArm64() {
        String targetRepoUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9arm64/yum/";
        String targetGpgKeyUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9arm64/yum/RPM-GPG-KEY-cloudera";
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo()
                .withBaseUrl(targetRepoUrl)
                .withGpgKeyUrl(targetGpgKeyUrl);

        ClouderaManagerRepo actual = underTest.updateCmRepoInCaseOfOsChange(clouderaManagerRepo, RHEL8, RHEL9, Architecture.ARM64.getName());

        assertEquals("https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat8arm64/yum/", actual.getBaseUrl());
        assertEquals("https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat8arm64/yum/RPM-GPG-KEY-cloudera", actual.getGpgKeyUrl());
    }

    @Test
    void testUpdateCmRepoInCaseOfOsChangeShouldReturnTheTargetUrlWhenTheUrlIsNotInTheExpectedFormat() {
        String targetRepoUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9";
        String targetGpgKeyUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9/yum/RPM-GPG-KEY-cloudera";
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo()
                .withBaseUrl(targetRepoUrl)
                .withGpgKeyUrl(targetGpgKeyUrl);

        ClouderaManagerRepo actual = underTest.updateCmRepoInCaseOfOsChange(clouderaManagerRepo, RHEL8, RHEL9, Architecture.X86_64.getName());

        assertEquals(targetRepoUrl, actual.getBaseUrl());
        assertEquals(targetGpgKeyUrl, actual.getGpgKeyUrl());
    }

    @Test
    void testUpdateCmRepoInCaseOfOsChangeShouldReturnTheTargetUrlWhenTheOsChangeIsNotSupported() {
        String targetRepoUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9/yum/";
        String targetGpgKeyUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9/yum/RPM-GPG-KEY-cloudera";
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo()
                .withBaseUrl(targetRepoUrl)
                .withGpgKeyUrl(targetGpgKeyUrl);

        ClouderaManagerRepo actual = underTest.updateCmRepoInCaseOfOsChange(clouderaManagerRepo, CENTOS7, RHEL9, Architecture.X86_64.getName());

        assertEquals(targetRepoUrl, actual.getBaseUrl());
        assertEquals(targetGpgKeyUrl, actual.getGpgKeyUrl());
    }

    @Test
    void testUpdateCmRepoInCaseOfOsChangeShouldReturnTheTargetUrlWhenTheOriginalOsIsNull() {
        String targetRepoUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9/yum/";
        String targetGpgKeyUrl = "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/redhat9/yum/RPM-GPG-KEY-cloudera";
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo()
                .withBaseUrl(targetRepoUrl)
                .withGpgKeyUrl(targetGpgKeyUrl);

        ClouderaManagerRepo actual = underTest.updateCmRepoInCaseOfOsChange(clouderaManagerRepo, null, RHEL9, Architecture.X86_64.getName());

        assertEquals(targetRepoUrl, actual.getBaseUrl());
        assertEquals(targetGpgKeyUrl, actual.getGpgKeyUrl());
    }

    private Image createTargetImage(OsType os) {
        return Image.builder()
                .withUuid("target-image")
                .withOs(os.getOs())
                .withOsType(os.getOsType())
                .withRepo(Map.of(os.getOsType(), "https://archive.cloudera.com/p/cm-public/7.12.0.400-57266911/" + os.getOsType() + "/yum/"))
                .build();
    }

}