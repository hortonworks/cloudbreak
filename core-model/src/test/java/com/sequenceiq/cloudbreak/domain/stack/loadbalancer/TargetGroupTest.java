package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.type.TargetGroupType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TargetGroupTest {

    @Test
    public void testSetCannotContainDuplicateTypes() {
        TargetGroup tg1 = new TargetGroup();
        tg1.setType(TargetGroupType.KNOX);
        TargetGroup tg2 = new TargetGroup();
        tg1.setType(TargetGroupType.KNOX);
        TargetGroup tg3 = new TargetGroup();
        tg1.setType(TargetGroupType.CM);

        Set<TargetGroup> targetGroups = new HashSet<>();
        targetGroups.add(tg1);
        targetGroups.add(tg2);
        targetGroups.add(tg3);

        assertEquals(Set.of(tg1, tg3), targetGroups);
    }
}
