package com.sequenceiq.cloudbreak.template.views;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RdsViewTest {

    static Object[][] getConnectionURLOptionsDataProvider() {
        return new Object[][]{
                // connectionURL, resultExpected
                {null, ""},
                {"", ""},
                {"foo", ""},
                {"jdbc:postgresql://cluster-master0.bar.com:5432/hive", ""},
                {"jdbc:postgresql://cluster-master0.bar.com:5432/hive?option1=value1&option2=value2", "?option1=value1&option2=value2"},
        };
    }

    @ParameterizedTest(name = "connectionURL={0}")
    @MethodSource("getConnectionURLOptionsDataProvider")
    void getConnectionURLOptionsTest(String connectionURL, String resultExpected) {
        RdsView underTest = new RdsView();
        underTest.setConnectionURL(connectionURL);

        assertThat(underTest.getConnectionURLOptions()).isEqualTo(resultExpected);
    }

}