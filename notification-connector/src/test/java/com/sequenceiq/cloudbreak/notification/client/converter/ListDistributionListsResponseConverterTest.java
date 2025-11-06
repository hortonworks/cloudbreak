package com.sequenceiq.cloudbreak.notification.client.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDetailsDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsResponseDto;

class ListDistributionListsResponseConverterTest {

    private final ListDistributionListsResponseConverter underTest = new ListDistributionListsResponseConverter();

    @Test
    void testConvertNullProto() {
        ListDistributionListsResponseDto dto = underTest.convert(null);
        assertNotNull(dto);
        assertNull(dto.distributionLists(), "distributionLists should be null when proto is null");
    }

    @Test
    void testConvertEmptyList() {
        NotificationAdminProto.ListDistributionListsResponse proto = NotificationAdminProto
                .ListDistributionListsResponse.newBuilder().build();
        ListDistributionListsResponseDto dto = underTest.convert(proto);
        assertNotNull(dto.distributionLists());
        assertTrue(dto.distributionLists().isEmpty());
    }

    @Test
    void testConvertWithMultipleEntriesAndDuplicateSlackChannels() {
        NotificationAdminProto.DistributionList dist1 = NotificationAdminProto.DistributionList.newBuilder()
                .setDistributionListId("dl-1")
                .setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:env1")
                .setResourceName("env1")
                .setParentResourceCrn("parent-crn-1")
                .addSlackChannelIds("slack-channel-A")
                .addSlackChannelIds("slack-channel-A")
                .setDistributionListManagementType(NotificationAdminProto.DistributionListManagementType.Value.USER_MANAGED)
                .addEmailAddresses("one@example.com")
                .addEmailAddresses("two@example.com")
                .build();

        NotificationAdminProto.DistributionList dist2 = NotificationAdminProto.DistributionList.newBuilder()
                .setDistributionListId("dl-2")
                .setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:env2")
                .setResourceName("env2")
                .setParentResourceCrn("parent-crn-2")
                .setDistributionListManagementType(NotificationAdminProto.DistributionListManagementType.Value.SYSTEM_MANAGED)
                // no slack channels, no email addresses
                .build();

        NotificationAdminProto.ListDistributionListsResponse proto = NotificationAdminProto
                .ListDistributionListsResponse.newBuilder()
                .addDistributionLists(dist1)
                .addDistributionLists(dist2)
                .build();

        ListDistributionListsResponseDto dto = underTest.convert(proto);
        List<DistributionListDetailsDto> converted = dto.distributionLists();
        assertEquals(2, converted.size());

        DistributionListDetailsDto c1 = converted.getFirst();
        assertEquals("dl-1", c1.distributionListId());
        assertEquals("crn:cdp:environments:us-west-1:tenant:environment:env1", c1.resourceCrn());
        assertEquals("env1", c1.resourceName());
        assertEquals("parent-crn-1", c1.parentResourceCrn());
        Set<String> slackIds = c1.slackChannelIds();
        assertEquals(1, slackIds.size(), "Duplicate slack channel IDs should be de-duplicated by Set.copyOf");
        assertTrue(slackIds.contains("slack-channel-A"));
        assertEquals("USER_MANAGED", c1.distributionListManagementType());
        assertEquals(List.of("one@example.com", "two@example.com"), c1.emailAddresses());

        // Ensure slack channel ids set is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> slackIds.add("another"));

        DistributionListDetailsDto c2 = converted.get(1);
        assertEquals("dl-2", c2.distributionListId());
        assertEquals("crn:cdp:environments:us-west-1:tenant:environment:env2", c2.resourceCrn());
        assertEquals("env2", c2.resourceName());
        assertEquals("parent-crn-2", c2.parentResourceCrn());
        assertNotNull(c2.slackChannelIds());
        assertTrue(c2.slackChannelIds().isEmpty());
        assertEquals("SYSTEM_MANAGED", c2.distributionListManagementType());
        assertNotNull(c2.emailAddresses());
        assertTrue(c2.emailAddresses().isEmpty());
    }
}

