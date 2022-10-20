package com.sequenceiq.redbeams.aspect;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.logger.LogContextService;

public class ControllerLogContextAspectsTest {

    @Mock
    private LogContextService logContextService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private CodeSignature sig;

    @InjectMocks
    private ControllerLogContextAspects underTest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(joinPoint.getSignature()).thenReturn(sig);
    }

    @Test
    public void testMDCUpdates() {
        String[] paramNames = { "param1", "param2" };
        when(sig.getParameterNames()).thenReturn(paramNames);

        Object[] args = { "value1", "value2" };
        when(joinPoint.getArgs()).thenReturn(args);

        Object target = new Object();
        when(joinPoint.getTarget()).thenReturn(target);

        underTest.buildLogContextForControllerCalls(joinPoint);

        verify(logContextService).buildMDCParams(target, paramNames, args);
    }
}
