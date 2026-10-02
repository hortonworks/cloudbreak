package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

class InstanceMetadataDeduplicatorTest {

    @Test
    void deduplicateByPrivateIpShouldKeepTheNewerInstanceWhenTwoInstancesShareTheSamePrivateIp() {
        InstanceMetaData older = instanceMetaData("i-older", "1.1.1.1", 1000L);
        InstanceMetaData newer = instanceMetaData("i-newer", "1.1.1.1", 2000L);

        Set<InstanceMetaData> result = InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(older, newer));

        assertThat(result).containsExactly(newer);
    }

    @Test
    void deduplicateByPrivateIpShouldKeepEveryInstanceWithDistinctPrivateIp() {
        InstanceMetaData first = instanceMetaData("i-first", "1.1.1.1", 1000L);
        InstanceMetaData second = instanceMetaData("i-second", "1.1.1.2", 2000L);

        Set<InstanceMetaData> result = InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(first, second));

        assertThat(result).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void deduplicateByPrivateIpShouldKeepTheInstanceWithStartDateWhenTheOtherOneHasNone() {
        InstanceMetaData withoutStartDate = instanceMetaData("i-without", "1.1.1.1", null);
        InstanceMetaData withStartDate = instanceMetaData("i-with", "1.1.1.1", 1000L);

        Set<InstanceMetaData> result = InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(withoutStartDate, withStartDate));

        assertThat(result).containsExactly(withStartDate);
    }

    @Test
    void deduplicateByPrivateIpShouldKeepEveryInstanceWithoutPrivateIp() {
        InstanceMetaData first = instanceMetaData("i-first", null, 1000L);
        InstanceMetaData second = instanceMetaData("i-second", null, 2000L);

        Set<InstanceMetaData> result = InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(first, second));

        assertThat(result).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void deduplicateByPrivateIpShouldKeepTheSameInstanceRegardlessOfTheInputOrderWhenTheStartDatesAreEqual() {
        InstanceMetaData first = instanceMetaData("i-first", "1.1.1.1", 1000L);
        InstanceMetaData second = instanceMetaData("i-second", "1.1.1.1", 1000L);

        assertThat(InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(first, second))).containsExactly(second);
        assertThat(InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(second, first))).containsExactly(second);
    }

    @Test
    void deduplicateByPrivateIpShouldKeepTheSameInstanceRegardlessOfTheInputOrderWhenNeitherInstanceHasStartDate() {
        InstanceMetaData first = instanceMetaData("i-first", "1.1.1.1", null);
        InstanceMetaData second = instanceMetaData("i-second", "1.1.1.1", null);

        assertThat(InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(first, second))).containsExactly(second);
        assertThat(InstanceMetadataDeduplicator.deduplicateByPrivateIp(List.of(second, first))).containsExactly(second);
    }

    private InstanceMetaData instanceMetaData(String instanceId, String privateIp, Long startDate) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setPrivateIp(privateIp);
        instanceMetaData.setStartDate(startDate);
        return instanceMetaData;
    }
}
