package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SubnetSelectorStrategyTypeTest {

    @Test
    public void testDescriptionOfSinglePreferPrivate() {
        SubnetSelectorStrategyType subnetSelectorStrategyType = SubnetSelectorStrategyType.SINGLE_PREFER_PRIVATE;

        assertEquals("choose single subnet prefer private", subnetSelectorStrategyType.getDescription());
    }

    @Test
    public void testDescriptionOfMultiplePreferPrivate() {
        SubnetSelectorStrategyType subnetSelectorStrategyType = SubnetSelectorStrategyType.MULTIPLE_PREFER_PRIVATE;

        assertEquals("choose multiple subnets in different AZs prefer private", subnetSelectorStrategyType.getDescription());
    }

    @Test
    public void testDescriptionOfSinglePreferPublic() {
        SubnetSelectorStrategyType subnetSelectorStrategyType = SubnetSelectorStrategyType.SINGLE_PREFER_PUBLIC;

        assertEquals("choose single subnet prefer public", subnetSelectorStrategyType.getDescription());
    }

    @Test
    public void testDescriptionOfMultiplePreferPublic() {
        SubnetSelectorStrategyType subnetSelectorStrategyType = SubnetSelectorStrategyType.MULTIPLE_PREFER_PUBLIC;

        assertEquals("choose multiple subnets in different AZs prefer public", subnetSelectorStrategyType.getDescription());
    }
}
