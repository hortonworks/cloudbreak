package com.sequenceiq.freeipa.flow.freeipa.upscale.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

class PrivateIdProviderTest {

    @Test
    void testGetFirstValidPriviateIdNoIds() {
        PrivateIdProvider underTest = new PrivateIdProvider();
        assertEquals(0, underTest.getFirstValidPrivateId(createInstanceGroup(Set.of())));
    }

    @Test
    void testGetFirstValidPriviateIdWithOneId() {
        PrivateIdProvider underTest = new PrivateIdProvider();
        assertEquals(1, underTest.getFirstValidPrivateId(createInstanceGroup(Set.of(0L))));
        assertEquals(2, underTest.getFirstValidPrivateId(createInstanceGroup(Set.of(1L))));
        assertEquals(3, underTest.getFirstValidPrivateId(createInstanceGroup(Set.of(2L))));
    }

    @Test
    void testGetFirstValidPriviateIdWithMultipleIds() {
        PrivateIdProvider underTest = new PrivateIdProvider();
        assertEquals(2, underTest.getFirstValidPrivateId(createInstanceGroup(Set.of(0L, 1L))));
        assertEquals(6, underTest.getFirstValidPrivateId(createInstanceGroup(Set.of(1L, 3L, 5L))));
    }

    private Set<InstanceGroup> createInstanceGroup(Set<Long> ids) {
        Set<InstanceMetaData> instanceMetaDataSet = ids.stream()
                .map(id -> {
                    InstanceMetaData im = mock(InstanceMetaData.class);
                    when(im.getPrivateId()).thenReturn(id);
                    return im;
                })
                .collect(Collectors.toSet());
        InstanceGroup ig = mock(InstanceGroup.class);
        when(ig.getAllInstanceMetaData()).thenReturn(instanceMetaDataSet);
        return Set.of(ig);
    }
}