package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.experience.config.ExperiencePathConfig;

@ExtendWith(MockitoExtension.class)
public class CommonExperiencePathCreatorTest {

    @InjectMocks
    private CommonExperiencePathCreator underTest;

    @Mock
    private ExperiencePathConfig componentsToReplace;

    @BeforeEach
    void setUp() {
        underTest.setUp();
    }

    @Test
    void testCreatePathToExperienceShouldCombineThePathCorretly() {
        CommonExperience xp = createCommonExperience();
        String expected = xp.getBaseAddress() + ":" + xp.getEnvironmentEndpointPort() + xp.getInternalEnvironmentEndpoint();

        String result = underTest.createPathToExperience(xp);

        assertEquals(expected, result);
    }

    @Test
    void testCreatePathToExperiencePolicyProviderShouldCombineThePathCorretly() {
        CommonExperience xp = createCommonExperience();
        String expected = xp.getBaseAddress() + ":" + xp.getEnvironmentEndpointPort() + xp.getInternalEnvironmentEndpoint() + xp.getPolicyEndpoint();

        String result = underTest.createPathToExperiencePolicyProvider(xp);

        assertEquals(expected, result);
    }

    private CommonExperience createCommonExperience() {
        CommonExperience cxp = new CommonExperience();
        cxp.setName("someXpName");
        cxp.setDescription("someDescription");
        cxp.setInternalEnvironmentEndpoint("someInternalEnvEndpoint");
        cxp.setAddress("https://someHostAddress:somePort");
        cxp.setBaseAddress("https://someHostAddress");
        cxp.setEnvironmentEndpointPort("somePort");
        cxp.setPolicyEndpoint("somePolicyPath");
        cxp.setBusinessName("someBusinessName");
        return cxp;
    }

}
