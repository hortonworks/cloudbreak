package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;

import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

public class SubnetSelectorStrategyTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final SubnetSelectorStrategy subnetSelectorStrategy = new SubnetSelectorStrategyTestImpl();

    @Test
    public void testWhenSubnetsNullThenThrowsBadRequest() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(
                "Error when selecting subnets with strategy 'choose multiple subnets in different AZs prefer public': There are no subnets in this network.");

        subnetSelectorStrategy.select(null);
    }

    @Test
    public void testWhenSubnetsEmptyThenThrowsBadRequest() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("There are no subnets in this network.");

        subnetSelectorStrategy.select(List.of());
    }

    @Test
    public void testWhenFewerSubnetsThanNeededThen() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("There are not enough subnets in this network, found: 1, expected: 2.");

        subnetSelectorStrategy.select(new SubnetBuilder().withPrivateSubnet(AZ_A).build());
    }

    private static class SubnetSelectorStrategyTestImpl extends SubnetSelectorStrategy {
        @Override
        protected List<CloudSubnet> selectInternal(List<CloudSubnet> subnets) {
            return List.of();
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
