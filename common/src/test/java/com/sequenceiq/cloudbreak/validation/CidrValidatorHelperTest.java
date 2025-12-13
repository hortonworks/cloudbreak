package com.sequenceiq.cloudbreak.validation;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class CidrValidatorHelperTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateCidrDataProvider() {
        return new Object[][]{
                {"0.0.0.0/0", true},
                {"::0/0", true},
                {"fe80:0000:0000:0000:0204:61ff:fe9d:f156/128", true},
                {"fe80::0204:61ff:fe9d:f156/9", true},
                {null, false},
                {"", false},
                {"0.0.0.0/0,", false},
                {"0.0.0.0/0 ", false},
                {",", false},
                {",,", false},
                {"abcds", false},
                {"0.0.0.0", false},
                {"192.168.1.1", false},
                {"192.268.1.1/33", false},
                {"0.0.0.0/x", false},
                {"::0", false},
                {"::x", false},
                {"::0/x", false},
                {"fe80:0000:0000:0000:0204:61ff:fe9d:f156", false},
                {"fe80:0000:0000:0000:0204:61ff:fe9d:f156/x", false},
                {"fe80:0000:0000:0000:0204:61ff:fe9d:f15x/128", false},
                {"fe80:0000:0000:0000:0204:61ff:fe9d:f156/129", false},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @TestFactory
    Collection<DynamicTest> testFactoryForCidr() {
        return Arrays.stream(validateCidrDataProvider())
                .map(x -> DynamicTest.dynamicTest(String.format("%s should be %b", (String) x[0], (Boolean) x[1]),
                        () -> assertEquals(CidrValidatorHelper.isCidrPatternMatched((String) x[0]), (Boolean) x[1])))
                .collect(Collectors.toList());
    }

}