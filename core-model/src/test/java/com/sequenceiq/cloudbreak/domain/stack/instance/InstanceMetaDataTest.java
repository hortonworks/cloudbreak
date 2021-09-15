package com.sequenceiq.cloudbreak.domain.stack.instance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class InstanceMetaDataTest {

    private static final String PRIVATE_IP = "10.20.30.40";

    private static final String PUBLIC_IP = "1.2.3.4";

    static Object[][] getIpWrapperDataProvider() {
        return new Object[][]{
                // testCaseName publicIp preferPrivateIp expectedResult
                {"publicIp=null, preferPrivateIp=false", null, false, PRIVATE_IP},
                {"publicIp=PUBLIC_IP, preferPrivateIp=false", PUBLIC_IP, false, PUBLIC_IP},
                {"publicIp=null, preferPrivateIp=true", null, true, PRIVATE_IP},
                {"publicIp=PUBLIC_IP, preferPrivateIp=true", PUBLIC_IP, true, PRIVATE_IP},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getIpWrapperDataProvider")
    void getIpWrapperTest(String testCaseName, String publicIp, boolean preferPrivateIp, String expectedResult) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPrivateIp(PRIVATE_IP);
        instanceMetaData.setPublicIp(publicIp);

        String result = instanceMetaData.getIpWrapper(preferPrivateIp);

        assertThat(result).isEqualTo(expectedResult);
    }

}