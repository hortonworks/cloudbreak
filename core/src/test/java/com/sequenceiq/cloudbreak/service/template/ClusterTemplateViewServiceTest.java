package com.sequenceiq.cloudbreak.service.template;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;

@RunWith(MockitoJUnitRunner.class)
public class ClusterTemplateViewServiceTest {

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @InjectMocks
    private ClusterTemplateViewService underTest;

    @Test
    public void testPrepareCreation() {
        expected.expect(BadRequestException.class);
        expected.expectMessage("Cluster template creation is not supported");

        underTest.prepareCreation(new ClusterTemplateView());
    }

    @Test
    public void testPrepareDeletion() {
        expected.expect(BadRequestException.class);
        expected.expectMessage("Cluster template deletion is not supported");

        underTest.prepareDeletion(new ClusterTemplateView());
    }

}
