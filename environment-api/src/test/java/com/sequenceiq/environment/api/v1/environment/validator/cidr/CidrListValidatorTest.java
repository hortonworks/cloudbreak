package com.sequenceiq.environment.api.v1.environment.validator.cidr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CidrListValidatorTest {

    @InjectMocks
    private CidrListValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateCidrDataProvider() {
        return new Object[][] {
                { List.of("0.0.0.0/0"),                                      true },
                { List.of("0.0.0.0/0","172.0.0.0/32"),                       true },
                { List.of("0.0.0.0/0","172.0.0.0/32","10.0.0.0/16"),         true },
                { List.of("::0/0"),                                          true },
                { List.of("fe80:0000:0000:0000:0204:61ff:fe9d:f156/128"),    true },
                { List.of("fe80::0204:61ff:fe9d:f156/9"),                    true },
                { List.of(),                                                 true },
                { List.of(""),                                               false },
                { List.of("0.0.0.0/0",""),                                   false },
                { List.of("0.0.0.0/0 "),                                     false },
                { List.of(","),                                              false },
                { List.of(",",","),                                          false },
                { List.of("abcds"),                                          false },
                { List.of("0.0.0.0"),                                        false },
                { List.of("192.168.1.1"),                                    false },
                { List.of("192.268.1.1/33"),                                 false },
                { List.of("0.0.0.0/x"),                                      false },
                { List.of("::0"),                                            false },
                { List.of("::x"),                                            false },
                { List.of("::0/x"),                                          false },
                { List.of("fe80:0000:0000:0000:0204:61ff:fe9d:f156"),        false },
                { List.of("fe80:0000:0000:0000:0204:61ff:fe9d:f156/x"),      false },
                { List.of("fe80:0000:0000:0000:0204:61ff:fe9d:f15x/128"),    false },
                { List.of("fe80:0000:0000:0000:0204:61ff:fe9d:f156/129"),    false },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "String is: {0}")
    @MethodSource("validateCidrDataProvider")
    void testIsClusterTemplateCloudPlatformValid(List<String> cidrs, boolean validExpected) {
        assertEquals(validExpected, underTest.isValid(cidrs, constraintValidatorContext));
    }

}