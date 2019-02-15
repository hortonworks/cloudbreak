package com.sequenceiq.periscope.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@RunWith(MockitoJUnitRunner.class)
public class RejectedThreadServiceTest {

    private final RejectedThreadService underTest = new RejectedThreadService();

    @Test
    public void testCreateWhenAutoscaleStackResponse() {
        AutoscaleStackV4Response response = new AutoscaleStackV4Response();
        response.setStackId(1L);
        EvaluatorExecutor task = new TestEvaluatorExecutor(getContext(response));

        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());
    }

    @Test
    public void testCreateWhenAutoscaleStackResponseCountEqualsTwo() {
        AutoscaleStackV4Response response = new AutoscaleStackV4Response();
        response.setStackId(1L);
        EvaluatorExecutor task = new TestEvaluatorExecutor(getContext(response));

        underTest.create(task);
        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());
        Assert.assertEquals(2L, allRejectedCluster.get(0).getRejectedCount());
    }

    @Test
    public void testCreateWhenAutoscaleStackResponseAndRemove() {
        AutoscaleStackV4Response response = new AutoscaleStackV4Response();
        response.setStackId(1L);
        EvaluatorExecutor task = new TestEvaluatorExecutor(getContext(response));

        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());

        underTest.remove(response);

        allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertTrue(allRejectedCluster.isEmpty());
    }

    @Test
    public void testCreateWhenClusterId() {
        Long data = 1L;
        EvaluatorExecutor task = new TestEvaluatorExecutor(getContext(data));

        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());
    }

    @Test
    public void testCreateWhenClusterIdCountEqualsTwo() {
        Long data = 1L;
        EvaluatorExecutor task = new TestEvaluatorExecutor(getContext(data));

        underTest.create(task);
        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());
        Assert.assertEquals(2L, allRejectedCluster.get(0).getRejectedCount());
    }

    @Test
    public void testCreateWhenClusterIdAndRemove() {
        Long data = 1L;
        EvaluatorExecutor task = new TestEvaluatorExecutor(getContext(data));

        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());

        underTest.remove(data);

        allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertTrue(allRejectedCluster.isEmpty());
    }

    private EvaluatorContext getContext(Object data) {
        EvaluatorContext context = mock(EvaluatorContext.class);
        when(context.getData()).thenReturn(data);
        return context;
    }

    private static class TestEvaluatorExecutor extends EvaluatorExecutor {

        private final EvaluatorContext context;

        TestEvaluatorExecutor(EvaluatorContext context) {
            this.context = context;
        }

        @Nonnull
        @Override
        public EvaluatorContext getContext() {
            return context;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public void setContext(EvaluatorContext context) {

        }

        @Override
        public void execute() {

        }
    }
}
