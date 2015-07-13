package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;

public class StackToStatusConverterTest extends AbstractEntityConverterTest<Stack> {

    private StackToStatusConverter underTest;

    @Before
    public void setUp() {
        underTest = new StackToStatusConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Map<String, Object> result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, result.get("id"));
        assertEquals(Status.AVAILABLE.name(), result.get("status"));
        assertEquals(Status.AVAILABLE.name(), result.get("clusterStatus"));
    }

    @Override
    public Stack createSource() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        return stack;
    }
}
