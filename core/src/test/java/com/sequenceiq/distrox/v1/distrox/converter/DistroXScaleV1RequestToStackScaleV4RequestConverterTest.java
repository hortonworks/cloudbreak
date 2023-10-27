package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkScaleV1Request;

class DistroXScaleV1RequestToStackScaleV4RequestConverterTest {

    private DistroXScaleV1RequestToStackScaleV4RequestConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new DistroXScaleV1RequestToStackScaleV4RequestConverter();
    }

    @Test
    public void testConvertWithValidSource() {
        DistroXScaleV1Request source = new DistroXScaleV1Request();
        source.setDesiredCount(10);
        source.setGroup("test-group");
        source.setAdjustmentType(AdjustmentType.BEST_EFFORT);
        source.setThreshold(5L);
        source.setForced(true);

        NetworkScaleV1Request networkScaleRequest = new NetworkScaleV1Request();
        networkScaleRequest.setPreferredSubnetIds(List.of("subnet-1, subnet-2"));
        networkScaleRequest.setPreferredAvailabilityZones(Set.of("us-east-1, us-west-1"));
        source.setNetworkScaleRequest(networkScaleRequest);

        StackScaleV4Request result = converter.convert(source);

        assertEquals(source.getDesiredCount(), result.getDesiredCount());
        assertEquals(source.getGroup(), result.getGroup());
        assertEquals(source.getAdjustmentType(), result.getAdjustmentType());
        assertEquals(source.getThreshold(), result.getThreshold());
        assertEquals(source.getForced(), result.getForced());

        NetworkScaleV4Request networkScaleV4Request = result.getStackNetworkScaleV4Request();
        assertEquals(networkScaleRequest.getPreferredSubnetIds(), networkScaleV4Request.getPreferredSubnetIds());
        assertEquals(networkScaleRequest.getPreferredAvailabilityZones(), networkScaleV4Request.getPreferredAvailabilityZones());
    }

}