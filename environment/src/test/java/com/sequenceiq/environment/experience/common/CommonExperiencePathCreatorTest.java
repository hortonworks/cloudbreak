package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommonExperiencePathCreatorTest {

    @InjectMocks
    private CommonExperiencePathCreator underTest;

    @Test
    void testCreatePathToExperienceShouldCombineThePathCorrectly() {
        CommonExperience xp = createCommonExperience();
        String expected = xp.getBaseAddress() + ":" + xp.getEnvironmentEndpointPort() + xp.getInternalEnvironmentEndpoint();

        String result = underTest.createPathToExperience(xp);

        assertEquals(expected, result);
    }

    @Test
    void testCreatePathToExperiencePolicyProviderShouldCombineThePathCorrectly() {
        CommonExperience xp = createCommonExperience();
        String expected = xp.getBaseAddress() + ":" + xp.getPolicyPort() + xp.getPolicyEndpoint();

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
        cxp.setPolicyEndpoint("/somePolicyPath");
        cxp.setPolicyPort("8081");
        cxp.setBusinessName("someBusinessName");
        return cxp;
    }

}
