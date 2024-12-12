package com.sequenceiq.cloudbreak.service.java.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
public class AllowableJavaConfigurationsTest {

    @InjectMocks
    private AllowableJavaConfigurations allowableJavaConfigurations;

    @BeforeEach
    public void beforeTests() {
        JavaConfiguration java11Config = new JavaConfiguration();
        java11Config.setVersion(11);
        java11Config.setMinRuntimeVersion("7.2.1");
        JavaConfiguration java17Config = new JavaConfiguration();
        java17Config.setVersion(17);
        java17Config.setMinRuntimeVersion("7.3.1");
        allowableJavaConfigurations.setJavaVersions(List.of(java11Config, java17Config));
    }

    @Test
    public void testCheckValidConfigurations() {
        allowableJavaConfigurations.checkValidConfiguration(11, "7.3.1");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.3.1");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.3.2");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.5.2");
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> allowableJavaConfigurations.checkValidConfiguration(17, "7.2.18"));
        assertEquals("The requested Java version 17 is not compatible with the runtime version 7.2.18", badRequestException.getMessage());
        badRequestException = assertThrows(BadRequestException.class,
                () -> allowableJavaConfigurations.checkValidConfiguration(17, "7.1.0"));
        assertEquals("The requested Java version 17 is not compatible with the runtime version 7.1.0", badRequestException.getMessage());
    }

}