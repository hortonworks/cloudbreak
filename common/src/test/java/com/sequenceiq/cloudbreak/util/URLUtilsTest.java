package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class URLUtilsTest {

    static Object[][] encodeStringDataProvider() {
        return new Object[][]{
                // testCaseName text expectedResult
                {"text=<empty string>", "", ""},
                {"text=abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz"},
                {"text=ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"},
                {"text=1234567890", "1234567890", "1234567890"},
                {"text=.-*_", ".-*_", ".-*_"},
                {"text=foo bar", "foo @bar", "foo%20%40bar"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encodeStringDataProvider")
    void encodeStringTestWhenSuccess(String testCaseName, String text, String expectedResult) {
        String result = URLUtils.encodeString(text);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void encodeStringTestWhenNPE() {
        assertThrows(NullPointerException.class, () -> URLUtils.encodeString(null));
    }

}