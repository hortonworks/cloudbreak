package com.sequenceiq.cloudbreak.quartz;

import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;

@ExtendWith(MockitoExtension.class)
public class TracedQuartzJobTest {

    @Test
    public void testFillMdcContextWhenNoBuilderImplemented() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        TracedQuartzJobTestClass underTest = new TracedQuartzJobTestClass();
        IllegalArgumentException actual = Assertions.assertThrows(IllegalArgumentException.class, () -> underTest.fillMdcContext(context));
        Assertions.assertEquals("Please implement one of them: getMdcContextObject() or getMdcContextConfigProvider()", actual.getMessage());
    }

    @Test
    public void testFillMdcContextWhenProviderBuilderImplementedButEmptyAndNoException() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        TracedQuartzJobTestClass underTest = new TracedQuartzJobTestClass() {
            @Override
            protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
                return Optional.empty();
            }
        };
        underTest.fillMdcContext(context);
    }

    @Test
    public void testFillMdcContextWhenObjectBuilderImplementedButEmptyAndNoException() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        TracedQuartzJobTestClass underTest = new TracedQuartzJobTestClass() {
            @Override
            protected Optional<Object> getMdcContextObject() {
                return Optional.empty();
            }
        };
        underTest.fillMdcContext(context);
    }
}
