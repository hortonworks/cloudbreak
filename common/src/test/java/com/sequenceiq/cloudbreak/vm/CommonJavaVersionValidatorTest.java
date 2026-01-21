package com.sequenceiq.cloudbreak.vm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
public class CommonJavaVersionValidatorTest {

    private static final int JAVA_VERSION_11 = 11;

    @Mock
    private VirtualMachineConfiguration virtualMachineConfiguration;

    @InjectMocks
    private CommonJavaVersionValidator underTest;

    @Test
    public void shouldFailIfJavaNotSupportedForRuntime() {
        when(virtualMachineConfiguration.getSupportedJavaVersionsByRuntime()).thenReturn(Map.of("7.3.2", Set.of()));
        when(virtualMachineConfiguration.isJavaVersionSupported(anyString(), anyInt())).thenReturn(Boolean.FALSE);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validateByVmConfiguration("7.3.2", JAVA_VERSION_11),
                "Java version 11 is not supported for runtime 7.3.2.");
    }

    @Test
    public void shouldFailIfJavaNotSupportedForDefault() {
        when(virtualMachineConfiguration.getSupportedJavaVersionsByRuntime()).thenReturn(Map.of("7.3.2", Set.of()));
        when(virtualMachineConfiguration.isJavaVersionSupported(anyInt())).thenReturn(Boolean.FALSE);

        assertThrows(BadRequestException.class, () -> underTest.validateByVmConfiguration("7.3.1", JAVA_VERSION_11),
                "Java version 11 is not supported.");

        assertThrows(BadRequestException.class, () -> underTest.validateByVmConfiguration(null, JAVA_VERSION_11),
                "Java version 11 is not supported.");
    }
}
