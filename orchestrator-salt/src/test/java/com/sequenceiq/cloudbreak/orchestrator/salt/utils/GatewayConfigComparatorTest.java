package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

class GatewayConfigComparatorTest {

    @Test
    public void testSortingOrderWhenNonPrimaryHasTheLatestSalt() {
        GatewayConfig nonPrimaryWithLatestSalt = GatewayConfig.builder().withInstanceId("gw0").withPrimary(false).withSaltVersion("4.0").build();
        GatewayConfig primaryWithNewSalt = GatewayConfig.builder().withInstanceId("gw1").withPrimary(true).withSaltVersion("3.0").build();
        GatewayConfig nonPrimaryWithNewSalt = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).withSaltVersion("3.0").build();
        GatewayConfig primaryWithOldSalt = GatewayConfig.builder().withInstanceId("gw3").withPrimary(true).withSaltVersion("2.0").build();
        GatewayConfig nonPrimaryWithOldSalt = GatewayConfig.builder().withInstanceId("gw4").withPrimary(false).withSaltVersion("2.0").build();
        GatewayConfig primaryWithoutSalt = GatewayConfig.builder().withInstanceId("gw5").withPrimary(true).withSaltVersion(null).build();
        GatewayConfig otherWithoutSalt = GatewayConfig.builder().withInstanceId("gw6").withPrimary(false).withSaltVersion(null).build();

        List<GatewayConfig> gateways = Arrays.asList(primaryWithoutSalt, otherWithoutSalt, primaryWithOldSalt, nonPrimaryWithOldSalt, nonPrimaryWithLatestSalt,
                nonPrimaryWithNewSalt, primaryWithNewSalt);

        List<GatewayConfig> sorted = gateways.stream().sorted(new GatewayConfigComparator()).toList();

        assertEquals("gw0", sorted.get(0).getInstanceId());
        assertEquals("gw1", sorted.get(1).getInstanceId());
        assertEquals("gw2", sorted.get(2).getInstanceId());
        assertEquals("gw3", sorted.get(3).getInstanceId());
        assertEquals("gw4", sorted.get(4).getInstanceId());
        assertEquals("gw5", sorted.get(5).getInstanceId());
        assertEquals("gw6", sorted.get(6).getInstanceId());
    }

    @Test
    public void testSortingOrderWhenPrimaryAndNonPrimaryHasTheLatestSalt() {
        GatewayConfig nonPrimaryWithLatestSalt = GatewayConfig.builder().withInstanceId("gw0").withPrimary(true).withSaltVersion("4.0").build();
        GatewayConfig primaryWithNewSalt = GatewayConfig.builder().withInstanceId("gw1").withPrimary(false).withSaltVersion("4.0").build();
        GatewayConfig nonPrimaryWithNewSalt = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).withSaltVersion("3.0").build();
        GatewayConfig primaryWithOldSalt = GatewayConfig.builder().withInstanceId("gw3").withPrimary(true).withSaltVersion("2.0").build();
        GatewayConfig nonPrimaryWithOldSalt = GatewayConfig.builder().withInstanceId("gw4").withPrimary(false).withSaltVersion("2.0").build();
        GatewayConfig primaryWithoutSalt = GatewayConfig.builder().withInstanceId("gw5").withPrimary(true).withSaltVersion(null).build();
        GatewayConfig otherWithoutSalt = GatewayConfig.builder().withInstanceId("gw6").withPrimary(false).withSaltVersion(null).build();

        List<GatewayConfig> gateways = Arrays.asList(primaryWithoutSalt, otherWithoutSalt, primaryWithOldSalt, nonPrimaryWithOldSalt, nonPrimaryWithLatestSalt,
                nonPrimaryWithNewSalt, primaryWithNewSalt);

        List<GatewayConfig> sorted = gateways.stream().sorted(new GatewayConfigComparator()).toList();

        assertEquals("gw0", sorted.get(0).getInstanceId());
        assertEquals("gw1", sorted.get(1).getInstanceId());
        assertEquals("gw2", sorted.get(2).getInstanceId());
        assertEquals("gw3", sorted.get(3).getInstanceId());
        assertEquals("gw4", sorted.get(4).getInstanceId());
        assertEquals("gw5", sorted.get(5).getInstanceId());
        assertEquals("gw6", sorted.get(6).getInstanceId());
    }

}