package com.sequenceiq.cloudbreak.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DatabaseSslDetailsTest {

    static Object[][] constructorTestDataProvider() {
        return new Object[][]{
                // sslCerts, sslEnabledForStack
                {Set.of(), false},
                {Set.of("foo"), false},
                {Set.of("foo"), true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestDataProvider")
    void constructorTest(Set<String> sslCerts, boolean sslEnabledForStack) {
        DatabaseSslDetails underTest = new DatabaseSslDetails(sslCerts, sslEnabledForStack);

        assertThat(underTest.getSslCerts()).isSameAs(sslCerts);
        assertThat(underTest.isSslEnabledForStack()).isEqualTo(sslEnabledForStack);
    }

    @Test
    void constructorTestNullCertsSet() {
        assertThrows(NullPointerException.class, () -> new DatabaseSslDetails(null, false));
    }

    @Test
    void setSslCertsTestNull() {
        DatabaseSslDetails underTest = new DatabaseSslDetails(Set.of(), false);

        assertThrows(NullPointerException.class, () -> underTest.setSslCerts(null));
    }

    @SafeVarargs
    private static <T> Set<T> linkedHashSet(T... elements) {
        return new LinkedHashSet<>(Arrays.asList(elements));
    }

    static Object[][] getSslCertBundleTestDataProvider() {
        return new Object[][]{
                // sslCerts, resultExpected
                {Set.of(), ""},
                {Set.of("foo"), "foo"},
                {linkedHashSet("foo", "bar"), "foo\nbar"},
                {linkedHashSet("foo", "bar", "baz"), "foo\nbar\nbaz"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getSslCertBundleTestDataProvider")
    void getSslCertBundleTest(Set<String> sslCerts, String resultExpected) {
        DatabaseSslDetails underTest = new DatabaseSslDetails(sslCerts, false);

        assertThat(underTest.getSslCertBundle()).isEqualTo(resultExpected);
    }

}