package com.sequenceiq.periscope.service;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@RunWith(MockitoJUnitRunner.class)
public class RejectedThreadServiceTest {

    private final RejectedThreadService underTest = new RejectedThreadService();

    @Test
    public void testCreateWhenAutoscaleStackResponse() {
        AutoscaleStackResponse response = new AutoscaleStackResponse();
        response.setStackId(1L);
        EvaluatorContext context = () -> response;
        EvaluatorExecutor task = new TestEvaluatorExecutor(context);

        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());
    }

    @Test
    public void testCreateWhenAutoscaleStackResponseCountEqualsTwo() {
        AutoscaleStackResponse response = new AutoscaleStackResponse();
        response.setStackId(1L);
        EvaluatorContext context = () -> response;
        EvaluatorExecutor task = new TestEvaluatorExecutor(context);

        underTest.create(task);
        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());
        Assert.assertEquals(2L, allRejectedCluster.get(0).getRejectedCount());
    }

    @Test
    public void testCreateWhenAutoscaleStackResponseAndRemove() {
        AutoscaleStackResponse response = new AutoscaleStackResponse();
        response.setStackId(1L);
        EvaluatorContext context = () -> response;
        EvaluatorExecutor task = new TestEvaluatorExecutor(context);

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
        EvaluatorContext context = () -> data;
        EvaluatorExecutor task = new TestEvaluatorExecutor(context);

        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());
    }

    @Test
    public void testCreateWhenClusterIdCountEqualsTwo() {
        Long data = 1L;
        EvaluatorContext context = () -> data;
        EvaluatorExecutor task = new TestEvaluatorExecutor(context);

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
        EvaluatorContext context = () -> data;
        EvaluatorExecutor task = new TestEvaluatorExecutor(context);

        underTest.create(task);

        List<RejectedThread> allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertFalse(allRejectedCluster.isEmpty());
        Assert.assertEquals(1L, allRejectedCluster.get(0).getId());

        underTest.remove(data);

        allRejectedCluster = underTest.getAllRejectedCluster();
        Assert.assertTrue(allRejectedCluster.isEmpty());
    }

    private static class TestEvaluatorExecutor implements EvaluatorExecutor {

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
        public void setContext(EvaluatorContext context) {

        }

        @Override
        public void run() {

        }
    }
}
