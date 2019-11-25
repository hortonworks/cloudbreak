package com.sequenceiq.environment.api.v1.environment.validator;


import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CidrValidatorTest {

    @InjectMocks
    private CidrValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateCidrDataProvider() {
        return new Object[][] {
                { "0.0.0.0/0",                                      true },
                { "0.0.0.0/0,172.0.0.0/32",                         true },
                { "0.0.0.0/0,172.0.0.0/32,10.0.0.0/16",             true },
                { "::0/0",                                          true },
                { "fe80:0000:0000:0000:0204:61ff:fe9d:f156/128",    true },
                { "fe80::0204:61ff:fe9d:f156/9",                    true },
                { null,                                             true },
                { "",                                               false },
                { "0.0.0.0/0,",                                     false },
                { "0.0.0.0/0 ",                                     false },
                { ",",                                              false },
                { ",,",                                             false },
                { "abcds",                                          false },
                { "0.0.0.0",                                        false },
                { "192.168.1.1",                                    false },
                { "192.268.1.1/33",                                 false },
                { "0.0.0.0/x",                                      false },
                { "::0",                                            false },
                { "::x",                                            false },
                { "::0/x",                                          false },
                { "fe80:0000:0000:0000:0204:61ff:fe9d:f156",        false },
                { "fe80:0000:0000:0000:0204:61ff:fe9d:f156/x",      false },
                { "fe80:0000:0000:0000:0204:61ff:fe9d:f15x/128",    false },
                { "fe80:0000:0000:0000:0204:61ff:fe9d:f156/129",    false },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "String is: {0}")
    @MethodSource("validateCidrDataProvider")
    void testIsClusterTemplateCloudPlatformValid(String cidrs, boolean validExpected) {
        assertEquals(validExpected, underTest.isValid(cidrs, constraintValidatorContext));
    }

}