package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.GcpNetwork;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@ExtendWith(MockitoExtension.class)
class DefaultAvailabilityZoneServiceTest {

    private static final String REGION = "us-west2";

    private static final String OTHER_REGION = "europe-west1";

    private static final int MAX_AVAILABILITY_ZONES = 3;

    @Mock
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private EnvironmentNetworkConverter environmentNetworkConverter;

    @InjectMocks
    private DefaultAvailabilityZoneService underTest;

    private Credential credential;

    @BeforeEach
    void setUp() {
        credential = new Credential();
        ReflectionTestUtils.setField(underTest, "maxAvailabilityZones", MAX_AVAILABILITY_ZONES);
    }

    @Test
    void populateIfMissingWritesTheAlphabeticallyFirstZonesUpToTheMaximum() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of());

        underTest.populateIfMissing(environment, network,
                cloudRegions(REGION, "us-west2-e", "us-west2-c", "us-west2-a", "us-west2-d", "us-west2-b"));

        verify(environmentNetworkConverter).getAvailabilityZones(network);
        verify(environmentNetworkConverter).updateAvailabilityZones(network, Set.of("us-west2-a", "us-west2-b", "us-west2-c"));
        verifyNoMoreInteractions(environmentNetworkConverter);
        verifyNoInteractions(platformParameterService);
    }

    @Test
    void populateIfMissingWritesEveryZoneWhenTheRegionHasFewerThanTheMaximum() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of());

        underTest.populateIfMissing(environment, network, cloudRegions(REGION, "us-west2-b", "us-west2-a"));

        verify(environmentNetworkConverter).updateAvailabilityZones(network, Set.of("us-west2-a", "us-west2-b"));
    }

    @Test
    void populateIfMissingWritesZonesWhenTheExistingZoneSetIsNull() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, null);

        underTest.populateIfMissing(environment, network, cloudRegions(REGION, "us-west2-a"));

        verify(environmentNetworkConverter).updateAvailabilityZones(network, Set.of("us-west2-a"));
    }

    @Test
    void populateIfMissingWritesOnlyTheZonesOfTheRegionOfTheEnvironment() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of());
        CloudRegions cloudRegions = new CloudRegions(
                Map.of(region(REGION), zones("us-west2-a", "us-west2-b"), region(OTHER_REGION), zones("europe-west1-a", "europe-west1-b")),
                Map.of(), Map.of(), Map.of(), REGION, true);

        underTest.populateIfMissing(environment, network, cloudRegions);

        verify(environmentNetworkConverter).updateAvailabilityZones(network, Set.of("us-west2-a", "us-west2-b"));
    }

    @Test
    void populateIfMissingDoesNotOverwriteExistingZones() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of("us-west2-a"));

        underTest.populateIfMissing(environment, network, cloudRegions(REGION, "us-west2-a", "us-west2-b", "us-west2-c"));

        verify(environmentNetworkConverter, never()).updateAvailabilityZones(any(), any());
    }

    @Test
    void populateIfMissingDoesNotWriteAnEmptySetWhenTheRegionHasNoZone() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of());

        underTest.populateIfMissing(environment, network, new CloudRegions(Map.of(), Map.of(), Map.of(), Map.of(), REGION, true));

        verify(environmentNetworkConverter, never()).updateAvailabilityZones(any(), any());
    }

    @Test
    void populateIfMissingDoesNotWriteWhenTheProviderDoesNotKnowTheRegionOfTheEnvironment() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of());

        underTest.populateIfMissing(environment, network, cloudRegions(OTHER_REGION, "europe-west1-a", "europe-west1-b"));

        verify(environmentNetworkConverter, never()).updateAvailabilityZones(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE", "YARN"})
    void populateIfMissingSkipsPlatformsWhereTheUserPicksTheZones(CloudPlatform cloudPlatform) {
        Environment environment = gcpEnvironment();
        environment.setCloudPlatform(cloudPlatform.name());
        environment.setNetwork(new AzureNetwork());

        underTest.populateIfMissing(environment, environment.getNetwork(), cloudRegions(REGION, "us-west2-a"));

        verifyNoInteractions(environmentNetworkConverterMap, environmentNetworkConverter, platformParameterService);
    }

    @Test
    void populateIfMissingSkipsWhenNetworkIsNull() {
        Environment environment = gcpEnvironment();
        environment.setNetwork(null);

        underTest.populateIfMissing(environment, null, cloudRegions(REGION, "us-west2-a"));

        verifyNoInteractions(environmentNetworkConverterMap, environmentNetworkConverter, platformParameterService);
    }

    @Test
    void populateIfMissingSkipsWhenRegionSetIsEmpty() {
        Environment environment = gcpEnvironment();
        environment.setRegions(Set.of());
        givenConverterReturningZones(environment.getNetwork(), Set.of());

        underTest.populateIfMissing(environment, environment.getNetwork(), cloudRegions(REGION, "us-west2-a"));

        verify(environmentNetworkConverter, never()).updateAvailabilityZones(any(), any());
        verifyNoInteractions(platformParameterService);
    }

    @Test
    void populateIfMissingSkipsWhenThereIsNoConverterForThePlatform() {
        Environment environment = gcpEnvironment();
        when(environmentNetworkConverterMap.get(CloudPlatform.GCP)).thenReturn(null);

        underTest.populateIfMissing(environment, environment.getNetwork(), cloudRegions(REGION, "us-west2-a"));

        verifyNoInteractions(environmentNetworkConverter, platformParameterService);
    }

    @Test
    void getRegionsQueriesTheRegionsOfTheCredential() {
        Environment environment = gcpEnvironment();
        CloudRegions expected = cloudRegions(REGION, "us-west2-a");
        when(platformParameterService.getRegionsByCredential(any(PlatformResourceRequest.class), eq(true))).thenReturn(expected);

        assertSame(expected, underTest.getRegions(environment));

        ArgumentCaptor<PlatformResourceRequest> requestCaptor = ArgumentCaptor.forClass(PlatformResourceRequest.class);
        verify(platformParameterService).getRegionsByCredential(requestCaptor.capture(), eq(true));
        assertSame(credential, requestCaptor.getValue().getCredential());
        assertEquals(CloudPlatform.GCP.name(), requestCaptor.getValue().getCloudPlatform());
    }

    @Test
    void populateIfMissingWritesTheZonesOfOneRegionWhenTheEnvironmentHasMoreThanOne() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        environment.setRegions(Set.of(environmentRegion(REGION), environmentRegion(OTHER_REGION)));
        givenConverterReturningZones(network, Set.of());
        CloudRegions cloudRegions = new CloudRegions(
                Map.of(region(REGION), zones("zone-a", "zone-b"), region(OTHER_REGION), zones("zone-a", "zone-b")),
                Map.of(), Map.of(), Map.of(), REGION, true);

        underTest.populateIfMissing(environment, network, cloudRegions);

        verify(environmentNetworkConverter).updateAvailabilityZones(network, Set.of("zone-a", "zone-b"));
    }

    @Test
    void isPopulationNeededWhenTheZonesAreMissing() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of());

        assertTrue(underTest.isPopulationNeeded(environment, network));
        verifyNoInteractions(platformParameterService);
    }

    @Test
    void isPopulationNeededWhenTheZonesAreMissingEvenIfTheyCannotBeDeterminedForLackOfRegion() {
        Environment environment = gcpEnvironment();
        environment.setRegions(Set.of());
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of());

        assertTrue(underTest.isPopulationNeeded(environment, network));
    }

    @Test
    void isPopulationNotNeededWhenTheZonesArePresent() {
        Environment environment = gcpEnvironment();
        BaseNetwork network = environment.getNetwork();
        givenConverterReturningZones(network, Set.of("us-west2-a"));

        assertFalse(underTest.isPopulationNeeded(environment, network));
    }

    @Test
    void isPopulationNotNeededWhenNetworkIsNull() {
        Environment environment = gcpEnvironment();

        assertFalse(underTest.isPopulationNeeded(environment, null));
    }

    private void givenConverterReturningZones(BaseNetwork network, Set<String> existingAvailabilityZones) {
        when(environmentNetworkConverterMap.get(CloudPlatform.GCP)).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.getAvailabilityZones(network)).thenReturn(existingAvailabilityZones);
    }

    private Environment gcpEnvironment() {
        Environment environment = new Environment();
        environment.setName("env");
        environment.setResourceCrn("crn:cdp:environments:us-west-1:accountId:environment:envId");
        environment.setCloudPlatform(CloudPlatform.GCP.name());
        environment.setCredential(credential);
        environment.setNetwork(new GcpNetwork());
        environment.setRegions(Set.of(environmentRegion(REGION)));
        return environment;
    }

    private Region environmentRegion(String name) {
        Region region = new Region();
        region.setName(name);
        return region;
    }

    private CloudRegions cloudRegions(String regionName, String... availabilityZones) {
        return new CloudRegions(Map.of(region(regionName), zones(availabilityZones)), Map.of(), Map.of(), Map.of(), regionName, true);
    }

    private List<AvailabilityZone> zones(String... availabilityZones) {
        return Arrays.stream(availabilityZones)
                .map(AvailabilityZone::availabilityZone)
                .toList();
    }
}
