package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStatusConverter;
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
        StackStatusV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(Long.valueOf(1L), result.getId());
        assertEquals(Status.AVAILABLE, result.getStatus());
        assertEquals(Status.AVAILABLE, result.getClusterStatus());
    }

    @Override
    public Stack createSource() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        return stack;
    }
}
