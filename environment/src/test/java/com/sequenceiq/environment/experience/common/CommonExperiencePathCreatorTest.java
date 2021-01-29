package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonExperiencePathCreatorTest {

    private static final String XP_PROTOCOL = "https";

    private CommonExperiencePathCreator underTest;

    @BeforeEach
    void setUp() {
        underTest = new CommonExperiencePathCreator(XP_PROTOCOL);
    }

    @Test
    void testCreatePathToExperienceShouldCombineThePathCorretly() {
        CommonExperience xp = createCommonExperience();
        String expected = XP_PROTOCOL + "://" + xp.getHostAddress() + ":" + xp.getPort() + xp.getInternalEnvEndpoint();

        String result = underTest.createPathToExperience(xp);

        assertEquals(expected, result);
    }

    private CommonExperience createCommonExperience() {
        CommonExperience cxp = new CommonExperience();
        cxp.setHostAddress("someHostAddress");
        cxp.setName("someXpName");
        cxp.setInternalEnvEndpoint("someInternalEnvEndpoint");
        cxp.setPort("somePort");
        return cxp;
    }

}