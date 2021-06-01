package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SubnetSelectorStrategyTypeTest {

    @Test
    public void testDescriptionOfMultiplePreferPrivate() {
        SubnetFilterStrategyType subnetSelectorStrategyType = SubnetFilterStrategyType.MULTIPLE_PREFER_PRIVATE;

        assertEquals("choose multiple subnets in different AZs prefer private", subnetSelectorStrategyType.getDescription());
    }

    @Test
    public void testDescriptionOfMultiplePreferPublic() {
        SubnetFilterStrategyType subnetSelectorStrategyType = SubnetFilterStrategyType.MULTIPLE_PREFER_PUBLIC;

        assertEquals("choose multiple subnets in different AZs prefer public", subnetSelectorStrategyType.getDescription());
    }
}
