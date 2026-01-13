package com.sequenceiq.cloudbreak.logger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControllerLogContextAspectsTest {

    @Mock
    private LogContextService logContextService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private CodeSignature sig;

    @InjectMocks
    private ControllerLogContextAspects underTest;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(sig);
    }

    @Test
    void testMDCUpdates() {
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
