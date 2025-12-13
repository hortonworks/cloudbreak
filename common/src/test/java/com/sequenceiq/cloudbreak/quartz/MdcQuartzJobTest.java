package com.sequenceiq.cloudbreak.quartz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;

@ExtendWith(MockitoExtension.class)
public class MdcQuartzJobTest {

    @Spy
    private MdcQuartzJob underTest;

    @Test
    public void testFillMdcContextWhenNoBuilderImplemented() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        MdcQuartzJobTestClass underTest = new MdcQuartzJobTestClass();
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> underTest.fillMdcContext(context));
        assertEquals("Please implement one of them: getMdcContextObject() or getMdcContextConfigProvider()", actual.getMessage());
    }

    @Test
    public void testFillMdcContextWhenProviderBuilderImplementedButEmptyAndNoException() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        MdcQuartzJobTestClass underTest = new MdcQuartzJobTestClass() {
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
        MdcQuartzJobTestClass underTest = new MdcQuartzJobTestClass() {
            @Override
            protected Optional<Object> getMdcContextObject() {
                return Optional.empty();
            }
        };
        underTest.fillMdcContext(context);
    }

    @Test
    public void testExecuteInternalWhenARuntimeExceptionHasBeenThrown() throws JobExecutionException, ClassNotFoundException {
        JobExecutionContext context = mock(JobExecutionContext.class, Answers.RETURNS_DEEP_STUBS);
        when(context.getJobDetail().getJobClass()).thenReturn(Class.forName(MdcQuartzJob.class.getName()).getClass().cast(Job.class));
        doNothing().when(underTest).fillMdcContext(context);
        doThrow(new RuntimeException("uh-oh something wrong happened")).when(underTest).executeTracedJob(any());

        JobExecutionException jobExecutionException = assertThrows(
                JobExecutionException.class,
                () -> underTest.executeInternal(context));

        assertEquals("java.lang.RuntimeException: uh-oh something wrong happened", jobExecutionException.getMessage());
    }
}
