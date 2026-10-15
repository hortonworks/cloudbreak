package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.validation.CmUrlProvider;
import com.sequenceiq.cloudbreak.service.upgrade.validation.ParcelUrlProvider;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
public class ParcelAvailabilityServiceTest {

    private static final String PARCEL_1 = "parcel1";

    private static final String PARCEL_2 = "parcel2";

    private static final String ARCHIVE_PARCEL = "https://archive.cloudera.com/parcel";

    private static final String CM_RPM = "https://archive.cloudera.com/cm-rpm";

    private static final String INTERNAL_PARCEL = "http://build-cache.vpc.cloudera.com/cfm/CFM-el8.parcel";

    private static final String INTERNAL_CM_RPM = "http://build-cache.vpc.cloudera.com/cm/cloudera-manager-server.el8.rpm";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ParcelAvailabilityService underTest;

    @Mock
    private ParcelUrlProvider parcelUrlProvider;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ParcelAvailabilityRetrievalService parcelAvailabilityRetrievalService;

    @Mock
    private CmUrlProvider cmUrlProvider;

    @Mock
    private StackDto stackDto;

    private final ClusterUpgradeProperties properties = createPrewarmedProperties();

    @Test
    public void testValidateParcelAvailabilityShouldReturnTheAvailableParcels() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(properties)).thenReturn(CM_RPM);

        Response response1 = createMockResponse(200, PARCEL_1);
        Response response2 = createMockResponse(200, PARCEL_2);
        Response response3 = createMockResponse(200, ARCHIVE_PARCEL);
        Response response4 = createMockResponse(200, CM_RPM);

        Set<Response> actual = underTest.validateAvailability(properties, STACK_ID);

        assertTrue(actual.contains(response1));
        assertTrue(actual.contains(response2));
        assertTrue(actual.contains(response3));
        assertFalse(actual.contains(response4));
        assertEquals(3, actual.size());
    }

    @Test
    public void testValidateParcelAvailabilityShouldNotThrowExceptionWhenANonArchiveParcelIsNotAvailable() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(properties)).thenReturn(CM_RPM);

        Response response1 = createMockResponse(404, PARCEL_1);
        Response response2 = createMockResponse(200, PARCEL_2);
        Response response3 = createMockResponse(200, ARCHIVE_PARCEL);
        Response response4 = createMockResponse(200, CM_RPM);

        Set<Response> actual = underTest.validateAvailability(properties, STACK_ID);

        assertTrue(actual.contains(response1));
        assertTrue(actual.contains(response2));
        assertTrue(actual.contains(response3));
        assertFalse(actual.contains(response4));
        assertEquals(3, actual.size());
    }

    @Test
    public void testValidateParcelAvailabilityShouldThrowExceptionWhenAParcelIsNotAvailable() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(properties)).thenReturn(CM_RPM);

        createMockResponse(404, ARCHIVE_PARCEL);
        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(200, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(properties, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldThrowExceptionWhenCmRpmIsNotAvailable() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(properties)).thenReturn(CM_RPM);

        createMockResponse(200, ARCHIVE_PARCEL);
        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(404, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(properties, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldThrowExceptionWhenTheWebTargetThrowsAnException() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(properties)).thenReturn(CM_RPM);

        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(ARCHIVE_PARCEL)).thenThrow(new BadRequestException());

        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(200, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(properties, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldNotThrowExceptionWhenTheWebTargetThrowsAnExceptionInCaseOfNonArchiveParcel() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(properties)).thenReturn(CM_RPM);

        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(PARCEL_1)).thenThrow(new BadRequestException());

        Response response3 = createMockResponse(200, ARCHIVE_PARCEL);
        Response response2 = createMockResponse(200, PARCEL_2);
        Response response4 = createMockResponse(200, CM_RPM);

        Set<Response> actual = underTest.validateAvailability(properties, STACK_ID);

        assertTrue(actual.contains(response2));
        assertTrue(actual.contains(response3));
        assertFalse(actual.contains(response4));
        assertEquals(2, actual.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {INTERNAL_PARCEL, INTERNAL_CM_RPM})
    void baseImageWithoutComponentsKeepsPrivateMirrorValidationBehavior(String unavailableUrl) {
        ClusterUpgradeProperties baseProperties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(baseProperties, stackDto)).thenReturn(Set.of(INTERNAL_PARCEL));
        when(cmUrlProvider.getCmRpmUrl(baseProperties)).thenReturn(INTERNAL_CM_RPM);
        Response parcelResponse = createMockResponse(INTERNAL_PARCEL.equals(unavailableUrl) ? 404 : 200, INTERNAL_PARCEL);
        createMockResponse(INTERNAL_CM_RPM.equals(unavailableUrl) ? 404 : 200, INTERNAL_CM_RPM);

        assertTrue(baseProperties.targetImage().isBaseImage());
        assertEquals(Set.of(parcelResponse), underTest.validateAvailability(baseProperties, STACK_ID));
    }

    @Test
    void baseImageUpgradeAllowsMirrorUnreachableFromControlPlane() {
        ClusterUpgradeProperties baseProperties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(baseProperties, stackDto)).thenReturn(Set.of(INTERNAL_PARCEL));
        when(cmUrlProvider.getCmRpmUrl(baseProperties)).thenReturn(INTERNAL_CM_RPM);
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(INTERNAL_PARCEL)).thenThrow(new BadRequestException());
        createMockResponse(200, INTERNAL_CM_RPM);

        assertTrue(underTest.validateAvailability(baseProperties, STACK_ID).isEmpty());
    }

    @Test
    void acceptsComponentsWithCmVersionWithoutBuildNumber() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "7.3.2", OsType.RHEL8,
                "x86_64", null, Set.of(), new ClouderaManagerRepo().withVersion("7.13.1.0").withBaseUrl("https://repo/cm/"));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(Set.of(INTERNAL_PARCEL));
        Response parcelResponse = createMockResponse(200, INTERNAL_PARCEL);
        createMockResponse(200, "https://repo/cm/repodata/repomd.xml");

        assertEquals(Set.of(parcelResponse), underTest.validateAvailability(properties, STACK_ID));
        verifyNoInteractions(cmUrlProvider);
    }

    @Test
    void acceptsAvailableInternalUrlsForBaseImageUpgradeAndExcludesCmFromParcelResponses() {
        ClusterUpgradeProperties baseProperties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(baseProperties, stackDto)).thenReturn(Set.of(INTERNAL_PARCEL));
        when(cmUrlProvider.getCmRpmUrl(baseProperties)).thenReturn(INTERNAL_CM_RPM);
        Response parcelResponse = createMockResponse(200, INTERNAL_PARCEL);
        createMockResponse(200, INTERNAL_CM_RPM);

        assertEquals(Set.of(parcelResponse), underTest.validateAvailability(baseProperties, STACK_ID));
    }

    private ClusterUpgradeProperties createPrewarmedProperties() {
        ClusterUpgradeProperties defaults = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ClusterUpgradeProperties.TargetImageUpgradeContext target = new ClusterUpgradeProperties.TargetImageUpgradeContext(
                "targetImageId", "catalog", "catalog-url", "7.2.18", "7.2.18", "123", Map.of(), Map.of(), OsType.RHEL8, "redhat8", "x86_64",
                null, null, null, new ImageStackDetails("7.2.18", new StackRepoDetails(Map.of(), Map.of()), "123"), Map.of(), null, null, null, Set.of(), null);
        return new ClusterUpgradeProperties(defaults.options(), defaults.currentImage(), target);
    }

    private Response createMockResponse(int status, String url) {
        Response response = Response.status(status).build();
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(url)).thenReturn(response);
        return response;
    }

    private Set<String> createRequiredParcelsSet() {
        Set<String> requiredParcelsFromImage = new HashSet<>();
        requiredParcelsFromImage.add(PARCEL_1);
        requiredParcelsFromImage.add(PARCEL_2);
        requiredParcelsFromImage.add(ARCHIVE_PARCEL);
        return requiredParcelsFromImage;
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 404})
    void validatesCmRepositoryMetadataWhenBuildNumberIsMissing(int status) {
        String baseUrl = "https://archive.cloudera.com/p/cm7/7.13.1.0/redhat8/yum";
        String metadataUrl = baseUrl + "/repodata/repomd.xml";
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "7.3.2", OsType.RHEL8,
                "x86_64", null, Set.of(), new ClouderaManagerRepo().withVersion("7.13.1.0").withBaseUrl(baseUrl));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(Set.of(INTERNAL_PARCEL));
        Response parcel = createMockResponse(200, INTERNAL_PARCEL);
        createMockResponse(status, metadataUrl);

        if (status == 200) {
            assertEquals(Set.of(parcel), underTest.validateAvailability(properties, STACK_ID));
        } else {
            UpgradeValidationFailedException failure = assertThrows(UpgradeValidationFailedException.class,
                    () -> underTest.validateAvailability(properties, STACK_ID));
            assertTrue(failure.getMessage().contains("Cloudera Manager repository metadata: " + metadataUrl));
        }
        verifyNoInteractions(cmUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://archive.cloudera.com/cm/", "https://private-mirror/cm/"})
    void preservesArchiveOnlyPolicyForUnreachableCmMetadata(String baseUrl) {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "7.3.2", OsType.RHEL8,
                "x86_64", null, Set.of(), new ClouderaManagerRepo().withVersion("7.13.1.0").withBaseUrl(baseUrl));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(properties, stackDto)).thenReturn(Set.of(INTERNAL_PARCEL));
        Response parcel = createMockResponse(200, INTERNAL_PARCEL);
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(baseUrl + "repodata/repomd.xml")).thenThrow(new BadRequestException("Unreachable"));

        if (baseUrl.contains("archive.cloudera.com")) {
            assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(properties, STACK_ID));
        } else {
            assertEquals(Set.of(parcel), underTest.validateAvailability(properties, STACK_ID));
        }
        verifyNoInteractions(cmUrlProvider);
    }

}
