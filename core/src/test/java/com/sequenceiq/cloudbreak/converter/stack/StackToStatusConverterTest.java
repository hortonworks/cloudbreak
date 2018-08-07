package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

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
