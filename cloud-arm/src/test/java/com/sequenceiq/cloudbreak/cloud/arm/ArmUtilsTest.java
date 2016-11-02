package com.sequenceiq.cloudbreak.cloud.arm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Network;

public class ArmUtilsTest {

    private ArmUtils subject;

    private final String maxResourceNameLength = "50";

    @Before
    public void setUp() {
        subject = new ArmUtils();
        ReflectionTestUtils.setField(subject, "maxResourceNameLength", Integer.parseInt(maxResourceNameLength));
    }

    @Test
    public void shouldAdjustResourceNameLengthIfItsTooLong() {
        //GIVEN
        CloudContext context = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "dummy2");

        //WHEN
        String testResult = subject.getStackName(context);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", testResult);
        assertEquals("The resource name is not the excepted one!", "thisisaverylongazureresourcenamewhichneedstobe7899", testResult);
        Assert.assertTrue("The resource name length is wrong", testResult.length() == Integer.parseInt(maxResourceNameLength));

    }

    @Test
    public void testValidateSubnetRulesForInvalidSecGroupId() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForValidRangeWithOneRule() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule = new HashMap();
        Map securityRuleProperties = new HashMap();
        securityRules.add(securityRule);
        securityRule.put("properties", securityRuleProperties);
        securityRuleProperties.put("direction", "Inbound");
        securityRuleProperties.put("protocol", "Tcp");
        securityRuleProperties.put("access", "Allow");
        securityRuleProperties.put("destinationPortRange", "22-443");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForValidRangeWithOneGreaterRule() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule = new HashMap();
        Map securityRuleProperties = new HashMap();
        securityRules.add(securityRule);
        securityRule.put("properties", securityRuleProperties);
        securityRuleProperties.put("direction", "Inbound");
        securityRuleProperties.put("protocol", "Tcp");
        securityRuleProperties.put("access", "Allow");
        securityRuleProperties.put("destinationPortRange", "20-450");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForValidRangeWithTwoRule() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule1 = new HashMap();
        securityRules.add(securityRule1);
        Map securityRule2 = new HashMap();
        securityRules.add(securityRule2);
        Map securityRule1Properties = new HashMap();
        securityRule1.put("properties", securityRule1Properties);
        securityRule1Properties.put("direction", "Inbound");
        securityRule1Properties.put("protocol", "Tcp");
        securityRule1Properties.put("access", "Allow");
        securityRule1Properties.put("destinationPortRange", "20-25");
        Map securityRule2Properties = new HashMap();
        securityRule2.put("properties", securityRule2Properties);
        securityRule2Properties.put("direction", "Inbound");
        securityRule2Properties.put("protocol", "Tcp");
        securityRule2Properties.put("access", "Allow");
        securityRule2Properties.put("destinationPortRange", "250-500");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForMissing22Rule() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule = new HashMap();
        securityRules.add(securityRule);
        Map securityRule2Properties = new HashMap();
        securityRule.put("properties", securityRule2Properties);
        securityRule2Properties.put("direction", "Inbound");
        securityRule2Properties.put("protocol", "Tcp");
        securityRule2Properties.put("access", "Allow");
        securityRule2Properties.put("destinationPortRange", "250-500");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        try {
            subject.validateSubnetRules(azureRMClient, network);
        } catch (Exception e) {
            assertEquals("The specified subnet's security group does not allow traffic for port 22 and/or 443", e.getMessage());
        }
    }

    @Test
    public void testValidateSubnetRulesForValidIndividualRules() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule1 = new HashMap();
        securityRules.add(securityRule1);
        Map securityRule2 = new HashMap();
        securityRules.add(securityRule2);
        Map securityRule1Properties = new HashMap();
        securityRule1.put("properties", securityRule1Properties);
        securityRule1Properties.put("direction", "Inbound");
        securityRule1Properties.put("protocol", "Tcp");
        securityRule1Properties.put("access", "Allow");
        securityRule1Properties.put("destinationPortRange", "22");
        Map securityRule2Properties = new HashMap();
        securityRule2.put("properties", securityRule2Properties);
        securityRule2Properties.put("direction", "Inbound");
        securityRule2Properties.put("protocol", "Tcp");
        securityRule2Properties.put("access", "Allow");
        securityRule2Properties.put("destinationPortRange", "443");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForValidIndividualAndRangeRules() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule1 = new HashMap();
        securityRules.add(securityRule1);
        Map securityRule2 = new HashMap();
        securityRules.add(securityRule2);
        Map securityRule1Properties = new HashMap();
        securityRule1.put("properties", securityRule1Properties);
        securityRule1Properties.put("direction", "Inbound");
        securityRule1Properties.put("protocol", "Tcp");
        securityRule1Properties.put("access", "Allow");
        securityRule1Properties.put("destinationPortRange", "22");
        Map securityRule2Properties = new HashMap();
        securityRule2.put("properties", securityRule2Properties);
        securityRule2Properties.put("direction", "Inbound");
        securityRule2Properties.put("protocol", "Tcp");
        securityRule2Properties.put("access", "Allow");
        securityRule2Properties.put("destinationPortRange", "400-500");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForValidAllPortRule() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule1 = new HashMap();
        securityRules.add(securityRule1);
        Map securityRule1Properties = new HashMap();
        securityRule1.put("properties", securityRule1Properties);
        securityRule1Properties.put("direction", "Inbound");
        securityRule1Properties.put("protocol", "*");
        securityRule1Properties.put("access", "Allow");
        securityRule1Properties.put("destinationPortRange", "*");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForValidForMultiplePorts() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule1 = new HashMap();
        securityRules.add(securityRule1);
        Map securityRule2 = new HashMap();
        securityRules.add(securityRule2);
        Map securityRule3 = new HashMap();
        securityRules.add(securityRule3);
        Map securityRule3Properties = new HashMap();
        securityRule2.put("properties", securityRule3Properties);
        securityRule3Properties.put("direction", "Inbound");
        securityRule3Properties.put("protocol", "Tcp");
        securityRule3Properties.put("access", "Allow");
        securityRule3Properties.put("destinationPortRange", "8080");
        Map securityRule4Properties = new HashMap();
        Map securityRule4 = new HashMap();
        securityRules.add(securityRule4);
        securityRule4.put("properties", securityRule4Properties);
        securityRule4Properties.put("direction", "Inbound");
        securityRule4Properties.put("protocol", "Tcp");
        securityRule4Properties.put("access", "Allow");
        securityRule4Properties.put("destinationPortRange", "2181-40000");
        Map securityRule1Properties = new HashMap();
        securityRule1.put("properties", securityRule1Properties);
        securityRule1Properties.put("direction", "Inbound");
        securityRule1Properties.put("protocol", "Tcp");
        securityRule1Properties.put("access", "Allow");
        securityRule1Properties.put("destinationPortRange", "22");
        Map securityRule2Properties = new HashMap();
        securityRule2.put("properties", securityRule2Properties);
        securityRule2Properties.put("direction", "Inbound");
        securityRule2Properties.put("protocol", "Tcp");
        securityRule2Properties.put("access", "Allow");
        securityRule2Properties.put("destinationPortRange", "400-500");
        Map securityRule5Properties = new HashMap();
        Map securityRule5 = new HashMap();
        securityRules.add(securityRule4);
        securityRule5.put("properties", securityRule5Properties);
        securityRule5Properties.put("direction", "Inbound");
        securityRule5Properties.put("protocol", "Tcp");
        securityRule5Properties.put("access", "Allow");
        securityRule5Properties.put("destinationPortRange", "10000");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

    @Test
    public void testValidateSubnetRulesForMissing443Rule() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        Map securityRule1 = new HashMap();
        securityRules.add(securityRule1);
        Map securityRule1Properties = new HashMap();
        securityRule1.put("properties", securityRule1Properties);
        securityRule1Properties.put("direction", "Inbound");
        securityRule1Properties.put("protocol", "Tcp");
        securityRule1Properties.put("access", "Allow");
        securityRule1Properties.put("destinationPortRange", "22");
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        try {
            subject.validateSubnetRules(azureRMClient, network);
        } catch (Exception e) {
            assertEquals("The specified subnet's security group does not allow traffic for port 22 and/or 443", e.getMessage());
        }
    }

    @Test
    public void testValidateSubnetRulesForEmptyRules() {
        Map subnetProperties = new HashMap();
        Map networkSecGroup = new HashMap();
        subnetProperties.put("networkSecurityGroup", networkSecGroup);
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup");
        Map securityGroupProperties = new HashMap();
        List securityRules = new ArrayList();
        securityGroupProperties.put("securityRules", securityRules);
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        try {
            subject.validateSubnetRules(azureRMClient, network);
        } catch (Exception e) {
            assertEquals("The specified subnet's security group does not allow traffic for port 22 and/or 443", e.getMessage());
        }
    }

    @Test
    public void testValidateSubnetForNoSecurityGroup() {
        Map subnetProperties = new HashMap();
        AzureRMClient azureRMClient = mock(AzureRMClient.class);
        Network network = mock(Network.class);
        when(network.getStringParameter(anyString())).thenReturn("value");
        when(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties);

        subject.validateSubnetRules(azureRMClient, network);
    }

}
