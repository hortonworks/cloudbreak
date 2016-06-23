package com.sequenceiq.cloudbreak.cloud.arm

import org.junit.Assert.assertEquals
import org.mockito.Matchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.HashMap

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.Network

class ArmUtilsTest {

    private var subject: ArmUtils? = null
    private val maxResourceNameLength = "50"

    @Before
    fun setUp() {
        subject = ArmUtils()
        ReflectionTestUtils.setField(subject, "maxResourceNameLength", Integer.parseInt(maxResourceNameLength))
    }

    @Test
    fun shouldAdjustResourceNameLengthIfItsTooLong() {
        //GIVEN
        val context = CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "dummy2")

        //WHEN
        val testResult = subject!!.getStackName(context)

        //THEN
        Assert.assertNotNull("The generated name must not be null!", testResult)
        assertEquals("The resource name is not the excepted one!", "thisisaverylongazureresourcenamewhichneedstobe7899", testResult)
        Assert.assertTrue("The resource name length is wrong", testResult.length == Integer.parseInt(maxResourceNameLength))

    }

    @Test
    fun testValidateSubnetRulesForInvalidSecGroupId() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForValidRangeWithOneRule() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule = HashMap()
        val securityRuleProperties = HashMap()
        securityRules.add(securityRule)
        securityRule.put("properties", securityRuleProperties)
        securityRuleProperties.put("direction", "Inbound")
        securityRuleProperties.put("protocol", "Tcp")
        securityRuleProperties.put("access", "Allow")
        securityRuleProperties.put("destinationPortRange", "22-443")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForValidRangeWithOneGreaterRule() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule = HashMap()
        val securityRuleProperties = HashMap()
        securityRules.add(securityRule)
        securityRule.put("properties", securityRuleProperties)
        securityRuleProperties.put("direction", "Inbound")
        securityRuleProperties.put("protocol", "Tcp")
        securityRuleProperties.put("access", "Allow")
        securityRuleProperties.put("destinationPortRange", "20-450")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForValidRangeWithTwoRule() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule1 = HashMap()
        securityRules.add(securityRule1)
        val securityRule2 = HashMap()
        securityRules.add(securityRule2)
        val securityRule1Properties = HashMap()
        securityRule1.put("properties", securityRule1Properties)
        securityRule1Properties.put("direction", "Inbound")
        securityRule1Properties.put("protocol", "Tcp")
        securityRule1Properties.put("access", "Allow")
        securityRule1Properties.put("destinationPortRange", "20-25")
        val securityRule2Properties = HashMap()
        securityRule2.put("properties", securityRule2Properties)
        securityRule2Properties.put("direction", "Inbound")
        securityRule2Properties.put("protocol", "Tcp")
        securityRule2Properties.put("access", "Allow")
        securityRule2Properties.put("destinationPortRange", "250-500")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForMissing22Rule() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule = HashMap()
        securityRules.add(securityRule)
        val securityRule2Properties = HashMap()
        securityRule.put("properties", securityRule2Properties)
        securityRule2Properties.put("direction", "Inbound")
        securityRule2Properties.put("protocol", "Tcp")
        securityRule2Properties.put("access", "Allow")
        securityRule2Properties.put("destinationPortRange", "250-500")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        try {
            subject!!.validateSubnetRules(azureRMClient, network)
        } catch (e: Exception) {
            assertEquals("The specified subnet's security group does not allow traffic for port 22 and/or 443", e.message)
        }

    }

    @Test
    fun testValidateSubnetRulesForValidIndividualRules() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule1 = HashMap()
        securityRules.add(securityRule1)
        val securityRule2 = HashMap()
        securityRules.add(securityRule2)
        val securityRule1Properties = HashMap()
        securityRule1.put("properties", securityRule1Properties)
        securityRule1Properties.put("direction", "Inbound")
        securityRule1Properties.put("protocol", "Tcp")
        securityRule1Properties.put("access", "Allow")
        securityRule1Properties.put("destinationPortRange", "22")
        val securityRule2Properties = HashMap()
        securityRule2.put("properties", securityRule2Properties)
        securityRule2Properties.put("direction", "Inbound")
        securityRule2Properties.put("protocol", "Tcp")
        securityRule2Properties.put("access", "Allow")
        securityRule2Properties.put("destinationPortRange", "443")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForValidIndividualAndRangeRules() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule1 = HashMap()
        securityRules.add(securityRule1)
        val securityRule2 = HashMap()
        securityRules.add(securityRule2)
        val securityRule1Properties = HashMap()
        securityRule1.put("properties", securityRule1Properties)
        securityRule1Properties.put("direction", "Inbound")
        securityRule1Properties.put("protocol", "Tcp")
        securityRule1Properties.put("access", "Allow")
        securityRule1Properties.put("destinationPortRange", "22")
        val securityRule2Properties = HashMap()
        securityRule2.put("properties", securityRule2Properties)
        securityRule2Properties.put("direction", "Inbound")
        securityRule2Properties.put("protocol", "Tcp")
        securityRule2Properties.put("access", "Allow")
        securityRule2Properties.put("destinationPortRange", "400-500")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForValidAllPortRule() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule1 = HashMap()
        securityRules.add(securityRule1)
        val securityRule1Properties = HashMap()
        securityRule1.put("properties", securityRule1Properties)
        securityRule1Properties.put("direction", "Inbound")
        securityRule1Properties.put("protocol", "*")
        securityRule1Properties.put("access", "Allow")
        securityRule1Properties.put("destinationPortRange", "*")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForValidForMultiplePorts() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule1 = HashMap()
        securityRules.add(securityRule1)
        val securityRule2 = HashMap()
        securityRules.add(securityRule2)
        val securityRule3 = HashMap()
        securityRules.add(securityRule3)
        val securityRule3Properties = HashMap()
        securityRule2.put("properties", securityRule3Properties)
        securityRule3Properties.put("direction", "Inbound")
        securityRule3Properties.put("protocol", "Tcp")
        securityRule3Properties.put("access", "Allow")
        securityRule3Properties.put("destinationPortRange", "8080")
        val securityRule4Properties = HashMap()
        val securityRule4 = HashMap()
        securityRules.add(securityRule4)
        securityRule4.put("properties", securityRule4Properties)
        securityRule4Properties.put("direction", "Inbound")
        securityRule4Properties.put("protocol", "Tcp")
        securityRule4Properties.put("access", "Allow")
        securityRule4Properties.put("destinationPortRange", "2181-40000")
        val securityRule1Properties = HashMap()
        securityRule1.put("properties", securityRule1Properties)
        securityRule1Properties.put("direction", "Inbound")
        securityRule1Properties.put("protocol", "Tcp")
        securityRule1Properties.put("access", "Allow")
        securityRule1Properties.put("destinationPortRange", "22")
        val securityRule2Properties = HashMap()
        securityRule2.put("properties", securityRule2Properties)
        securityRule2Properties.put("direction", "Inbound")
        securityRule2Properties.put("protocol", "Tcp")
        securityRule2Properties.put("access", "Allow")
        securityRule2Properties.put("destinationPortRange", "400-500")
        val securityRule5Properties = HashMap()
        val securityRule5 = HashMap()
        securityRules.add(securityRule4)
        securityRule5.put("properties", securityRule5Properties)
        securityRule5Properties.put("direction", "Inbound")
        securityRule5Properties.put("protocol", "Tcp")
        securityRule5Properties.put("access", "Allow")
        securityRule5Properties.put("destinationPortRange", "10000")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

    @Test
    fun testValidateSubnetRulesForMissing443Rule() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val securityRule1 = HashMap()
        securityRules.add(securityRule1)
        val securityRule1Properties = HashMap()
        securityRule1.put("properties", securityRule1Properties)
        securityRule1Properties.put("direction", "Inbound")
        securityRule1Properties.put("protocol", "Tcp")
        securityRule1Properties.put("access", "Allow")
        securityRule1Properties.put("destinationPortRange", "22")
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        try {
            subject!!.validateSubnetRules(azureRMClient, network)
        } catch (e: Exception) {
            assertEquals("The specified subnet's security group does not allow traffic for port 22 and/or 443", e.message)
        }

    }

    @Test
    fun testValidateSubnetRulesForEmptyRules() {
        val subnetProperties = HashMap()
        val networkSecGroup = HashMap()
        subnetProperties.put("networkSecurityGroup", networkSecGroup)
        networkSecGroup.put("id", "/subscriptions/94/resourceGroups/secgroup/providers/Microsoft.Network/networkSecurityGroups/secgroup")
        val securityGroupProperties = HashMap()
        val securityRules = ArrayList()
        securityGroupProperties.put("securityRules", securityRules)
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(azureRMClient.getSecurityGroupProperties(anyString(), anyString())).thenReturn(securityGroupProperties)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        try {
            subject!!.validateSubnetRules(azureRMClient, network)
        } catch (e: Exception) {
            assertEquals("The specified subnet's security group does not allow traffic for port 22 and/or 443", e.message)
        }

    }

    @Test
    fun testValidateSubnetForNoSecurityGroup() {
        val subnetProperties = HashMap()
        val azureRMClient = mock<AzureRMClient>(AzureRMClient::class.java)
        val network = mock<Network>(Network::class.java)
        `when`(network.getStringParameter(anyString())).thenReturn("value")
        `when`(azureRMClient.getSubnetProperties(anyString(), anyString(), anyString())).thenReturn(subnetProperties)

        subject!!.validateSubnetRules(azureRMClient, network)
    }

}
