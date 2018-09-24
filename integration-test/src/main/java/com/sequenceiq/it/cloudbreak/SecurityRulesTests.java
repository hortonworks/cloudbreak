package com.sequenceiq.it.cloudbreak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.SecurityRules;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

public class SecurityRulesTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRulesTests.class);

    public SecurityRulesTests() {
    }

    public SecurityRulesTests(TestParameter tp) {
        setTestParameter(tp);
    }

    private String getJsonFile() throws IOException {
        return new String(StreamUtils.copyToByteArray(applicationContext.getResource("classpath:/templates/gatewayResponses.json").getInputStream()));
    }

    private List<SecurityRuleResponse> getCustomGatewaysResponses() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(getJsonFile(), mapper.getTypeFactory().constructCollectionType(List.class, SecurityRuleResponse.class));
    }

    private String setCustomGatewaysResponses(SecurityRules securityrules) {
        String expectedPort = "";

        try {
            List<SecurityRuleResponse> customGatewaySet = getCustomGatewaysResponses();
            expectedPort = getCustomGatewaysResponses().iterator().next().getPorts();

            securityrules.getResponse().setGateway(customGatewaySet);
        } catch (IOException e) {
            LOGGER.info("Set custom Gateways Responses Exception message ::: {}", e.getMessage());
        }

        return expectedPort;
    }

    @Test(priority = 1, groups = "securityrules")
    public void testListGateways() throws Exception {
        given(CloudbreakClient.created());
        given(SecurityRules.request(), " disk types request");
        when(SecurityRules.getDefaultSecurityRules(), " disk types are requested.");
        then(SecurityRules.assertThis(
                (securityrules, t) -> {
                    List<SecurityRuleResponse> getewaysList = securityrules.getResponse().getGateway();

                    getewaysList.forEach(gateway -> LOGGER.debug(" Security Rule gateway is ::: {}", gateway.getPorts()));
                    Assert.assertFalse(getewaysList.isEmpty(), "Security Rule Gateways should be present in response!");
                }), " Security Rule Gateways should be part of the response."
        );
    }

    @Test(priority = 2, groups = "securityrules")
    public void testListCores() throws Exception {
        given(CloudbreakClient.created());
        given(SecurityRules.request(), " disk types request");
        when(SecurityRules.getDefaultSecurityRules(), " disk types are requested.");
        then(SecurityRules.assertThis(
                (securityrules, t) -> {
                    List<SecurityRuleResponse> coresList = securityrules.getResponse().getCore();

                    coresList.forEach(core -> LOGGER.debug(" Security Rule core is ::: {}", core.getId()));
                    Assert.assertTrue(coresList.isEmpty(), "Security Rule Cores should be present in response!");
                }), " Security Rule Cores should be part of the response."
        );
    }

    @Test(priority = 3, groups = "securityrules")
    public void testSetGateway() throws Exception {
        given(CloudbreakClient.created());
        given(SecurityRules.request(), " disk types request");
        when(SecurityRules.getDefaultSecurityRules(), " disk types are requested.");
        then(SecurityRules.assertThis(
                (securityrules, t) -> {
                    String expectedPort = setCustomGatewaysResponses(securityrules);
                    List<SecurityRuleResponse> gatewaysList = securityrules.getResponse().getGateway();

                    gatewaysList.forEach(gateway -> {
                        LOGGER.debug(" Security Rule custom gateway is ::: {}", gateway.getPorts());
                        Assert.assertEquals(gateway.getPorts(), expectedPort, "Security Rule custom Gateways should be present in response!");
                    });
                }), " Security Rule custom Gateways should be part of the response."
        );
    }
}