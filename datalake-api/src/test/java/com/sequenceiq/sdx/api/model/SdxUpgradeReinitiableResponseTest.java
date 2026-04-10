package com.sequenceiq.sdx.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;

public class SdxUpgradeReinitiableResponseTest {

    static Stream<Arguments> testFromUpgradeReinitiableV4Response() {
        return Stream.of(
                Arguments.of(
                        new UpgradeReinitiableV4Response(UpgradeReinitiateStatus.NON_REINITIABLE),
                        new SdxUpgradeReinitiableResponse(UpgradeReinitiateStatus.NON_REINITIABLE, null)
                ),
                Arguments.of(
                        new UpgradeReinitiableV4Response(UpgradeReinitiateStatus.REINITIABLE, "Upgrade can be reinitiated"),
                        new SdxUpgradeReinitiableResponse(UpgradeReinitiateStatus.REINITIABLE, "Upgrade can be reinitiated")
                ),
                Arguments.of(
                        new UpgradeReinitiableV4Response(UpgradeReinitiateStatus.REINITIABLE, "Upgrade can be reinitiated"),
                        new SdxUpgradeReinitiableResponse(UpgradeReinitiateStatus.REINITIABLE, "Upgrade can be reinitiated")
                )
        );
    }

    @MethodSource
    @ParameterizedTest
    void testFromUpgradeReinitiableV4Response(UpgradeReinitiableV4Response upgradeReinitiableV4Response, SdxUpgradeReinitiableResponse expected) {
        assertEquals(expected, SdxUpgradeReinitiableResponse.from(upgradeReinitiableV4Response));
    }
}
