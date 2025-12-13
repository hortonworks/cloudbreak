package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.core.management.Region;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
public class AzureRegionProviderTest {

    private static final String ENABLED_REGIONS_FILE = "enabled-regions";

    @InjectMocks
    private AzureRegionProvider underTest;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private String testRegionsJson;

    @BeforeEach
    public void before() throws IOException {
        ReflectionTestUtils.setField(underTest, "armZoneParameterDefault", "North Europe");
        ReflectionTestUtils.setField(underTest, "azureAvailabilityZones", Set.of("1", "2", "3"));
        testRegionsJson = getTestRegions();
        when(cloudbreakResourceReaderService.resourceDefinition("azure", ENABLED_REGIONS_FILE)).thenReturn(testRegionsJson);
        underTest.init();
    }

    @Test
    public void testRegionsShouldReturnTheEnabledRegions() throws IOException {
        Collection<Region> azureRegions = getAzureRegions();

        CloudRegions actual = underTest.regions(null, azureRegions, List.of());

        assertRegionNames(actual, azureRegions);
        assertCoordinates(actual);
        assertEquals("North Europe", actual.getDefaultRegion());
    }

    @Test
    public void testRegionsShouldReturnTheEnabledRegionsWhenAzureResponseContainsUnsupportedRegions() {
        Collection<Region> azureRegions = getAzureRegionsWithUnsupportedRegion();
        Collection<Region> supportedAzureRegions = getAzureRegions();

        CloudRegions actual = underTest.regions(null, azureRegions, List.of());

        assertRegionNames(actual, supportedAzureRegions);
        assertEquals("North Europe", actual.getDefaultRegion());
    }

    @Test
    public void testFilterEnabledRegionsNoRegionGiven() {
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> regions = Map.of(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate("westus"),
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus2"), azureCoordinate("westus2"));
        ReflectionTestUtils.setField(underTest, "enabledRegions", regions);

        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> actualRegions = underTest.filterEnabledRegions();

        assertEquals(regions, actualRegions);
    }

    @Test
    public void testFilterEnabledRegionsAllRegionsAreGiven() {
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> regions = Map.of(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate("westus"),
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus2"), azureCoordinate("westus2"));
        ReflectionTestUtils.setField(underTest, "enabledRegions", regions);

        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> actualRegions = underTest.filterEnabledRegions(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), com.sequenceiq.cloudbreak.cloud.model.Region.region("westus2"));

        assertEquals(regions, actualRegions);
    }

    @Test
    public void testFilterEnabledRegionsRegionIsGiven() {
        AzureCoordinate azureCoordinate1 = azureCoordinate("westus");
        AzureCoordinate azureCoordinate2 = azureCoordinate("westus2");
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> regions = Map.of(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate1,
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus2"), azureCoordinate2);
        ReflectionTestUtils.setField(underTest, "enabledRegions", regions);

        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> actualRegions = underTest.filterEnabledRegions(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"));

        assertEquals(Map.of(com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate1), actualRegions);
    }

    @Test
    public void testFilterEnabledRegionsNullRegionGiven() {
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> regions = Map.of(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate("westus"),
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus2"), azureCoordinate("westus2"));
        ReflectionTestUtils.setField(underTest, "enabledRegions", regions);

        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> actualRegions = underTest.filterEnabledRegions(null);

        assertEquals(regions, actualRegions);
    }

    @Test
    public void testFilterEnabledRegionsInvalidRegionsAreGiven() {
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> regions = Map.of(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate("westus"),
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus2"), azureCoordinate("westus2"));
        ReflectionTestUtils.setField(underTest, "enabledRegions", regions);

        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> actualRegions =
                underTest.filterEnabledRegions(null, com.sequenceiq.cloudbreak.cloud.model.Region.region(""));

        assertEquals(regions, actualRegions);
    }

    @Test
    public void testFilterEnabledRegionsValidAndInvalidRegionsAreGiven() {
        AzureCoordinate azureCoordinate = azureCoordinate("westus");
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> regions = Map.of(
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate,
                com.sequenceiq.cloudbreak.cloud.model.Region.region("westus2"), azureCoordinate("westus2"));
        ReflectionTestUtils.setField(underTest, "enabledRegions", regions);

        Map<com.sequenceiq.cloudbreak.cloud.model.Region, AzureCoordinate> actualRegions =
                underTest.filterEnabledRegions(null, com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"));

        assertEquals(Map.of(com.sequenceiq.cloudbreak.cloud.model.Region.region("westus"), azureCoordinate), actualRegions);
    }

    private String getTestRegions() throws IOException {
        return FileReaderUtils.readFileFromClasspath("/json/azure-regions-test.json");
    }

    private Collection<Region> getAzureRegions() {
        return Set.of(Region.ASIA_EAST, Region.US_WEST, Region.CANADA_CENTRAL, Region.EUROPE_WEST);
    }

    private Collection<Region> getAzureRegionsWithUnsupportedRegion() {
        return Set.of(Region.ASIA_EAST, Region.US_WEST, Region.CANADA_CENTRAL, Region.EUROPE_WEST, Region.GOV_US_ARIZONA);
    }

    private void assertRegionNames(CloudRegions actual, Collection<Region> azureRegions) {
        assertTrue(azureRegions.stream()
                .allMatch(region -> actual.getCloudRegions().keySet()
                        .stream().map(com.sequenceiq.cloudbreak.cloud.model.Region::getRegionName)
                        .collect(Collectors.toSet())
                        .contains(region.label())));
    }

    private void assertCoordinates(CloudRegions actual) throws IOException {
        RegionCoordinateSpecifications regionCoordinateSpecifications = getRegionsFromFile();
        assertTrue(actual.getCoordinates().values().stream()
                .allMatch(coordinate -> regionCoordinateSpecifications.getItems().stream()
                        .anyMatch(region -> region.getLatitude().equals(coordinate.getLatitude().toString()) &&
                                region.getLongitude().equals(coordinate.getLongitude().toString()))));

    }

    private RegionCoordinateSpecifications getRegionsFromFile() throws IOException {
        return JsonUtil.readValue(testRegionsJson, RegionCoordinateSpecifications.class);

    }

    private AzureCoordinate azureCoordinate(String name) {
        return AzureCoordinate.AzureCoordinateBuilder.builder()
                .longitude("1")
                .latitude("1")
                .displayName(name)
                .key(name + "key")
                .k8sSupported(false)
                .entitlements(List.of())
                .build();
    }
}