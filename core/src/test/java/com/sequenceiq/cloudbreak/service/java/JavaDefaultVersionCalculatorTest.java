package com.sequenceiq.cloudbreak.service.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.java.vm.DefaultJavaConfigurations;
import com.sequenceiq.cloudbreak.service.java.vm.JavaConfiguration;

@ExtendWith(MockitoExtension.class)
class JavaDefaultVersionCalculatorTest {

    @Mock
    private DefaultJavaConfigurations defaultJavaConfigurations;

    @InjectMocks
    private JavaDefaultVersionCalculator underTest;

    static Object[][] scenarios() {
        return new Object[][] {
                // runtime javaVersion expectedVersion
                { "7.2.7",     null,         8  },
                { "7.2.8",     null,         8  },
                { "7.2.9",     null,         8  },
                { "7.2.10",    null,         8  },
                { "7.2.11",    null,         8  },
                { "7.2.12",    null,         8  },
                { "7.2.14",    null,         8  },
                { "7.2.15",    null,         8  },
                { "7.2.16",    null,         8  },
                { "7.2.17",    null,         8  },
                { "7.2.18",    null,         8  },
                { "7.3.1",     null,         17 },
                { "7.2.7",     8,            8  },
                { "7.2.8",     8,            8  },
                { "7.2.9",     8,            8  },
                { "7.2.10",    11,           11 },
                { "7.2.11",    11,           11 },
                { "7.2.12",    17,           17 },
                { "7.2.14",    17,           17 },
                { "7.2.15",    17,           17 },
                { "7.2.16",    17,           17 },
                { "7.2.17",    8,            8  },
                { "7.2.18",    8,            8  },
                { "7.3.1",     11,           11 },
        };
    }

    @ParameterizedTest(name = "runtime: {0} javaVersion: {1} expectedVersion: {2}")
    @MethodSource("scenarios")
    void constructionTest(String runtime, Integer javaVersion, int expectedVersion) {
        if (javaVersion == null) {
            JavaConfiguration java8 = new JavaConfiguration();
            java8.setVersion(8);
            java8.setMinRuntimeVersion("7.1.0");
            java8.setMaxRuntimeVersion("7.2.18");

            JavaConfiguration java17 = new JavaConfiguration();
            java17.setVersion(17);
            java17.setMinRuntimeVersion("7.3.1");

            when(defaultJavaConfigurations.defaultJavaConfigurationsAsList())
                    .thenReturn(List.of(java8, java17));
        }

        int result = underTest.calculate(javaVersion, runtime);

        assertEquals(expectedVersion, result);
    }

    @Test
    void testNotSupportedVersion() {
        JavaConfiguration java8 = new JavaConfiguration();
        java8.setVersion(8);
        java8.setMinRuntimeVersion("7.1.0");
        java8.setMaxRuntimeVersion("7.2.18");

        JavaConfiguration java17 = new JavaConfiguration();
        java17.setVersion(17);
        java17.setMinRuntimeVersion("7.3.1");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.calculate(null, "7.0.0"));
        assertEquals("The runtimeVersion 7.0.0 is not supported in CDP. Please change the runtime version.", exception.getMessage());
    }
}