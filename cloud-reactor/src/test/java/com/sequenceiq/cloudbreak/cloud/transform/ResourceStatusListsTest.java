package com.sequenceiq.cloudbreak.cloud.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

class ResourceStatusListsTest {

    @Test
    void aggregateReason() {

        String aggregatedReason = ResourceStatusLists.aggregateReason(List.of(new CloudResourceStatus(mock(CloudResource.class), ResourceStatus.CREATED),
                new CloudResourceStatus(mock(CloudResource.class), ResourceStatus.CREATED, "asdf"),
                new CloudResourceStatus(mock(CloudResource.class), ResourceStatus.CREATED, "qwer")));
        assertEquals("asdf, qwer", aggregatedReason);
    }

    @Test
    void aggregateReasonUnknown() {

        String aggregatedReason = ResourceStatusLists.aggregateReason(List.of(new CloudResourceStatus(mock(CloudResource.class), ResourceStatus.CREATED),
                new CloudResourceStatus(mock(CloudResource.class), ResourceStatus.CREATED, " "),
                new CloudResourceStatus(mock(CloudResource.class), ResourceStatus.CREATED, "")));
        assertEquals("Unknown", aggregatedReason);
    }
}