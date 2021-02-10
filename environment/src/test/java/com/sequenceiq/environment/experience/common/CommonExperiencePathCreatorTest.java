package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonExperiencePathCreatorTest {

    private CommonExperiencePathCreator underTest;

    @BeforeEach
    void setUp() {
        underTest = new CommonExperiencePathCreator();
    }

    @Test
    void testCreatePathToExperienceShouldCombineThePathCorretly() {
        CommonExperience xp = createCommonExperience();
        String expected = xp.getAddress() + xp.getInternalEnvironmentEndpoint();

        String result = underTest.createPathToExperience(xp);

        assertEquals(expected, result);
    }

    private CommonExperience createCommonExperience() {
        CommonExperience cxp = new CommonExperience();
        cxp.setName("someXpName");
        cxp.setDescription("someDescription");
        cxp.setInternalEnvironmentEndpoint("someInternalEnvEndpoint");
        cxp.setAddress("https://someHostAddress:somePort");
        return cxp;
    }

}
