package com.sequenceiq.freeipa.api.v1.dns.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DnsRecordRegexpPatternsTest {

    static Object[][] cnameTargetSource() {
        return new Object[][]{
                // testCaseName  resultExpected
                {"www\\google.com", false},
                {"www\\\\google.com", false},
                {"www.go\\ogle.com", false},
                {"www.google.com", true},
                {"google.com", true},
                {"google.com.", false},
                {".google.com", false},
                {"goo-gle.com", true},
                {"www.goo-gle.com", true},
                {"google.com-", false},
                {"-google.com", false},
                {"*.google.com", false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cnameTargetSource")
    void testCnameTargetRegexp(String testTarget, boolean resultExpected) {
        Pattern pattern = Pattern.compile(DnsRecordRegexpPatterns.CNAME_TARGET_REGEXP);
        boolean result = pattern.matcher(testTarget).find();
        assertEquals(resultExpected, result);
    }

    static Object[][] cnameSource() {
        return new Object[][]{
                // testCaseName  resultExpected
                {"www\\google.com", false},
                {"www\\\\google.com", false},
                {"www.go\\ogle.com", false},
                {"www.google.com", true},
                {"google.com", true},
                {"google.com.", false},
                {".google.com", false},
                {"goo-gle.com", true},
                {"www.goo-gle.com", true},
                {"google.com-", false},
                {"-google.com", false},
                {"*.google.com", true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cnameSource")
    void testCnameRegexp(String testTarget, boolean resultExpected) {
        Pattern pattern = Pattern.compile(DnsRecordRegexpPatterns.DNS_CNAME_PATTERN);
        boolean result = pattern.matcher(testTarget).find();
        assertEquals(resultExpected, result);
    }

    static Object[][] hostnameSource() {
        return new Object[][]{
                // testCaseName  resultExpected
                {"www\\google.com", false},
                {"www\\\\google.com", false},
                {"www.go\\ogle.com", false},
                {"www.google.com", true},
                {"google.com", true},
                {"google.com.", false},
                {".google.com", false},
                {"goo-gle.com", true},
                {"www.goo-gle.com", true},
                {"google.com-", false},
                {"-google.com", false},
                {"*.google.com", false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hostnameSource")
    void testHostnameRegexp(String testTarget, boolean resultExpected) {
        Pattern pattern = Pattern.compile(DnsRecordRegexpPatterns.DNS_HOSTNAME_PATTERN);
        boolean result = pattern.matcher(testTarget).find();
        assertEquals(resultExpected, result);
    }

    static Object[][] zoneSource() {
        return new Object[][]{
                // testCaseName  resultExpected
                {"www\\google.com", false},
                {"www\\\\google.com", false},
                {"www.go\\ogle.com", false},
                {"www.google.com", true},
                {"google.com", true},
                {"google.com.", true},
                {".google.com", false},
                {"goo-gle.com", true},
                {"www.goo-gle.com", true},
                {"google.com-", false},
                {"-google.com", false},
                {"*.google.com", false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("zoneSource")
    void testZoneRegexp(String testTarget, boolean resultExpected) {
        Pattern pattern = Pattern.compile(DnsRecordRegexpPatterns.DNS_ZONE_PATTERN);
        boolean result = pattern.matcher(testTarget).find();
        assertEquals(resultExpected, result);
    }

}