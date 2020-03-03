package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

public class SubnetSelectorStrategyTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final SubnetSelectorStrategy subnetSelectorStrategy = new SubnetSelectorStrategyTestImpl();

    @Test
    public void testWhenSubnetsNullThenErrorMessage() {

        SubnetSelectionResult result = subnetSelectorStrategy.select(null);

        assertTrue(result.hasError());
        assertEquals("There are no subnets in this network.", result.getErrorMessage());
    }

    @Test
    public void testWhenSubnetsEmptyThenErrorMessage() {

        SubnetSelectionResult result = subnetSelectorStrategy.select(List.of());

        assertTrue(result.hasError());
        assertEquals("There are no subnets in this network.", result.getErrorMessage());
    }

    @Test
    public void testWhenFewerSubnetsThanNeededThenErrorMessage() {

        SubnetSelectionResult result = subnetSelectorStrategy.select(new SubnetBuilder().withPrivateSubnet(AZ_A).build());

        assertTrue(result.hasError());
        assertEquals("There are not enough subnets in this network, found: 1, expected: 2.", result.getErrorMessage());
    }

    private static class SubnetSelectorStrategyTestImpl extends SubnetSelectorStrategy {
        @Override
        protected SubnetSelectionResult selectInternal(List<CloudSubnet> subnets) {
            return new SubnetSelectionResult(List.of());
        }

        @Override
        public SubnetSelectorStrategyType getType() {
            return SubnetSelectorStrategyType.MULTIPLE_PREFER_PUBLIC;
        }

        @Override
        protected int getMinimumNumberOfSubnets() {
            return 2;
        }
    }
}
