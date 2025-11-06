package com.sequenceiq.cloudbreak.notification.client.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDto;

class CreateOrUpdateDistributionListResponseConverterTest {

    private final CreateOrUpdateDistributionListResponseConverter underTest = new CreateOrUpdateDistributionListResponseConverter();

    @Test
    void testConvertWhenProtoIsNull() {
        CreateOrUpdateDistributionListResponseDto result = underTest.convert(null);
        assertNotNull(result, "Result dto should not be null even if input proto is null");
        assertNull(result.distributionLists(), "Distribution lists should be null when input proto is null");
    }

    @Test
    void testConvertWhenProtoHasDistributionListDetails() {
        NotificationAdminProto.DistributionListDetails details1 = NotificationAdminProto.DistributionListDetails.newBuilder()
                .setDistributionListId("dl-1")
                .setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:env1")
                .build();
        NotificationAdminProto.DistributionListDetails details2 = NotificationAdminProto.DistributionListDetails.newBuilder()
                .setDistributionListId("dl-2")
                .setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:env2")
                .build();
        NotificationAdminProto.CreateOrUpdateDistributionListResponse proto = NotificationAdminProto.CreateOrUpdateDistributionListResponse.newBuilder()
                .addDistributionListDetails(details1)
                .addDistributionListDetails(details2)
                .build();

        CreateOrUpdateDistributionListResponseDto result = underTest.convert(proto);

        assertNotNull(result);
        List<DistributionListDto> lists = result.distributionLists();
        assertNotNull(lists, "Converted list should not be null");
        assertEquals(2, lists.size(), "Exactly two distribution list entries expected");

        DistributionListDto first = lists.get(0);
        assertEquals("dl-1", first.distributionListId());
        assertEquals("crn:cdp:environments:us-west-1:tenant:environment:env1", first.resourceCrn());

        DistributionListDto second = lists.get(1);
        assertEquals("dl-2", second.distributionListId());
        assertEquals("crn:cdp:environments:us-west-1:tenant:environment:env2", second.resourceCrn());
    }

    @Test
    void testConvertWhenProtoHasNoDistributionListDetails() {
        NotificationAdminProto.CreateOrUpdateDistributionListResponse proto = NotificationAdminProto
                .CreateOrUpdateDistributionListResponse.newBuilder().build();
        CreateOrUpdateDistributionListResponseDto result = underTest.convert(proto);
        assertNotNull(result);
        assertNotNull(result.distributionLists(), "Should return an empty (not null) list when proto has none");
        assertTrue(result.distributionLists().isEmpty());
    }
}
