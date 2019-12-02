package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class AzureRegionProviderTest {

    private static final String ENABLED_REGIONS_FILE = "enabled-regions";

    @InjectMocks
    private AzureRegionProvider underTest;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private String testRegionsJson;

    @Before
    public void before() throws IOException {
        ReflectionTestUtils.setField(underTest, "armZoneParameterDefault", "North Europe");
        testRegionsJson = getTestRegions();
        when(cloudbreakResourceReaderService.resourceDefinition("azure", ENABLED_REGIONS_FILE)).thenReturn(testRegionsJson);
        underTest.init();
    }

    @Test
    public void testRegionsShouldReturnTheEnabledRegions() throws IOException {
        Collection<Region> azureRegions = getAzureRegions();

        CloudRegions actual = underTest.regions(null, azureRegions);

        assertRegionNames(actual, azureRegions);
        assertCoordinates(actual);
        Assert.assertEquals("North Europe", actual.getDefaultRegion());
    }

    @Test
    public void testRegionsShouldReturnTheEnabledRegionsWhenAzureResponseContainsUnsupportedRegions() {
        Collection<Region> azureRegions = getAzureRegionsWithUnsupportedRegion();
        Collection<Region> supportedAzureRegions = getAzureRegions();

        CloudRegions actual = underTest.regions(null, azureRegions);

        assertRegionNames(actual, supportedAzureRegions);
        Assert.assertEquals("North Europe", actual.getDefaultRegion());
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
        Assert.assertTrue(azureRegions.stream()
                .allMatch(region -> actual.getCloudRegions().keySet()
                        .stream().map(com.sequenceiq.cloudbreak.cloud.model.Region::getRegionName)
                        .collect(Collectors.toSet())
                        .contains(region.label())));
    }

    private void assertCoordinates(CloudRegions actual) throws IOException {
        RegionCoordinateSpecifications regionCoordinateSpecifications = getRegionsFromFile();
        Assert.assertTrue(actual.getCoordinates().values().stream()
                .allMatch(coordinate -> regionCoordinateSpecifications.getItems().stream()
                        .anyMatch(region -> region.getLatitude().equals(coordinate.getLatitude().toString()) &&
                                region.getLongitude().equals(coordinate.getLongitude().toString()))));

    }

    private RegionCoordinateSpecifications getRegionsFromFile() throws IOException {
        return JsonUtil.readValue(testRegionsJson, RegionCoordinateSpecifications.class);

    }

}