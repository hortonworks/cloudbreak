package com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
