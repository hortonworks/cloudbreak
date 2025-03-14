<#setting number_format="computer">
<#macro zoneredundant>
<#if multiAz>,
"zones": [
           "1",
           "2",
           "3"
         ]
</#if>
</#macro>
{
    "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
        <#if existingVPC>
        "resourceGroupName" : {
          "type": "string",
          "defaultValue" : "${resourceGroupName}"
        },
        "existingVNETName" : {
          "type": "string",
          "defaultValue" : "${existingVNETName}"
        },
        <#else>
        "virtualNetworkNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        </#if>
        "publicIPNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "nicNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "region" : {
          "type" : "string",
          "defaultValue" : "${region}"
        },
        "subnet1Name": {
            "type": "string",
            "defaultValue": "${stackname}subnet"
        }
    },
  	"variables" : {
      <#list igs as group>
      "${group.compressedName}secGroupName": "${group.compressedName}${stackname}sg",
      </#list>
      <#if existingVPC>
      "vnetID": "[resourceId(parameters('resourceGroupName'),'Microsoft.Network/virtualNetworks',parameters('existingVNETName'))]"
      <#else>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks',parameters('virtualNetworkNamePrefix'))]"
      </#if>
  	},
    "resources": [
             <#list groups?keys as instanceGroup>
             <#list groups[instanceGroup] as instance>
                <#if !noPublicIp>
                 {
                   "apiVersion": "2023-06-01",
                   "type": "Microsoft.Network/publicIPAddresses",
                   "name": "[concat(parameters('publicIPNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "tags": {
                     <#if userDefinedTags?? && userDefinedTags?has_content>
                         <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                        </#list>
                     </#if>
                     },
                   <#if multiAz && instance.instance.availabilityZone?? && instance.instance.availabilityZone?has_content>
                     "zones": [
                       "${instance.instance.availabilityZone}"
                     ],
                   </#if>
                   "sku": {
                       "name": "Standard",
                       "tier": "Regional"
                   },
                   "properties": {
                       "publicIPAllocationMethod": "Static"
                   }
                 },
                 </#if>
                 {
                   "apiVersion": "2023-06-01",
                   "type": "Microsoft.Network/networkInterfaces",
                   "name": "[concat(parameters('nicNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "dependsOn": [
                       <#if !noPublicIp>
                           "[concat('Microsoft.Network/publicIPAddresses/', parameters('publicIPNamePrefix'), '${instance.instanceId}')]"
                       </#if>

                       <#if loadBalancerMapping[instance.groupName]?? && (loadBalancerMapping[instance.groupName]?size > 0)>
                           <#if !noPublicIp || !existingVPC>,</#if>
                           <#list loadBalancerMapping[instance.groupName] as loadBalancer>
                               <#--
                                  we have to define a dependency between the NIC and the address pool it belongs to
                                  this makes every instance in the gateway group depend on every load balancer address pool
                                  when we add support for multiple load balancers, this will have to be updated.
                               -->
                               "[resourceId('Microsoft.Network/loadBalancers', '${loadBalancer.name}')]"<#sep>,</#sep>
                           </#list>
                       </#if>
                   ],
                   "properties": {
                       <#if acceleratedNetworkEnabled[instance.flavor]> "enableAcceleratedNetworking": "true", </#if>
                       <#if securityGroups[instance.groupName]?? && securityGroups[instance.groupName]?has_content>
                          "networkSecurityGroup":{
                              "id": "${securityGroups[instance.groupName]}"
                           },
                       <#else>
                           "networkSecurityGroup":{
                               "id": "[resourceId('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                          },
                       </#if>
                       "ipConfigurations": [
                           {
                               "name": "ipconfig1",
                               "properties": {
                                   "privateIPAllocationMethod": "Dynamic",
                                   <#if !noPublicIp>
                                   "publicIPAddress": {
                                       "id": "[resourceId('Microsoft.Network/publicIPAddresses',concat(parameters('publicIPNamePrefix'), '${instance.instanceId}'))]"
                                   },
                                   </#if>
                                   <#if existingVPC>
                                   "subnet": {
                                       "id": "[concat(variables('vnetID'),'/subnets/', '${instance.subnetId}')]"
                                   }
                                   <#else>
                                   "subnet": {
                                       "id": "[concat(variables('vnetID'),'/subnets/',parameters('subnet1Name'))]"
                                   }
                                   </#if>
                                   <#if loadBalancerMapping[instance.groupName]?? && (loadBalancerMapping[instance.groupName]?size > 0)>
                                   ,"loadBalancerBackendAddressPools": [
                                           <#--
                                               This is adding the NIC to all load balancer backend address pools.
                                               When we add more load balancers, we'll have to associate the NIC with
                                               only a single LB pool
                                           -->
                                       <#list loadBalancerMapping[instance.groupName] as loadBalancer>
                                       { "id": "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '${loadBalancer.name}', '${instance.groupName}-pool')]" }
                                       <#sep>, </#sep>
                                       </#list>
                                   ]
                                   </#if>
                               }
                           }
                       ]
                   }
                 }<#if (instance_index + 1) != groups[instanceGroup]?size>,</#if>
             </#list>
             <#if (instanceGroup_index + 1) != groups?size>,</#if>
             </#list>
             <#if groups?keys?has_content && loadBalancers?has_content>,</#if>
            <#list loadBalancers as loadBalancer>
                {
                  "apiVersion": "2023-06-01",
                  "type": "Microsoft.Network/loadBalancers",
                  "dependsOn": [
                    <#if loadBalancer.type == "PUBLIC" || loadBalancer.type == "OUTBOUND">
                    "[resourceId('Microsoft.Network/publicIPAddresses', '${loadBalancer.name}-publicIp')]"
                    </#if>
                  ],
                  "location": "[parameters('region')]",
                  "name": "${loadBalancer.name}",
                  "tags": {
                    <#if userDefinedTags?? && userDefinedTags?has_content>
                        <#list userDefinedTags?keys as key>
                            "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                        </#list>
                    </#if>
                  },
                  "properties": {
                    "backendAddressPools": [
                       <#list loadBalancer.instanceGroupNames as groupName>
                       {
                         "name": "${groupName}-pool",
                         "properties": {
                         }
                       }<#sep>,</#sep>
                       </#list>
                    ],
                    "frontendIPConfigurations": [
                      <#if loadBalancer.type == "PUBLIC" || loadBalancer.type == "OUTBOUND">
                      {
                        "name": "${loadBalancer.name}-frontend",
                        "properties": {
                          "publicIPAddress": {
                            "id": "[resourceId('Microsoft.Network/publicIPAddresses', '${loadBalancer.name}-publicIp')]"
                          }
                        }
                      }
                      <#else>
                      {
                        "name": "${loadBalancer.name}-frontend",
                        "properties": {
                          "privateIPAddressVersion": "IPv4",
                          "privateIPAllocationMethod": "Dynamic",
                          <#if existingVPC>
                          "subnet": {
                            "id": "[concat(variables('vnetID'),'/subnets/', '${existingSubnetName}')]"
                          }
                          <#else>
                          "subnet": {
                            "id": "[concat(variables('vnetID'),'/subnets/',parameters('subnet1Name'))]"
                          }
                          </#if>
                        }<@zoneredundant/>
                      }<#if gatewayPrivateLbNeeded!false>,
                      {
                        "name": "${loadBalancer.name}-frontend-gateway",
                        "properties": {
                          "privateIPAddressVersion": "IPv4",
                          "privateIPAllocationMethod": "Dynamic",
                          "subnet": {
                            "id": "[concat(variables('vnetID'), '/subnets/', '${endpointGwSubnet}')]"
                          }
                        }<@zoneredundant/>
                      }
                       </#if>
                      </#if>
                    ],
                    "inboundNatPools": [],
                    "inboundNatRules": [],
                    "loadBalancingRules": [
                        <#list loadBalancer.rules as rule>
                            {
                                "name": "${rule.name}",
                                "properties": {
                                    "backendAddressPool": {
                                        "id": "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '${loadBalancer.name}', '${rule.groupName}-pool')]"
                                    },
                                    "backendPort": ${rule.backendPort},
                                    "enableFloatingIP": <#if loadBalancer.type == "PRIVATE" && gatewayPrivateLbNeeded!false>true<#else>false</#if>,
                                    "enableTcpReset": false,
                                    "frontendIPConfiguration": {
                                        "id": "[concat(resourceId('Microsoft.Network/loadBalancers', '${loadBalancer.name}'), '/frontendIPConfigurations/${loadBalancer.name}-frontend')]"
                                    },
                                    "frontendPort": ${rule.frontendPort},
                                    "idleTimeoutInMinutes": 4,
                                    "loadDistribution": "Default",
                                    "probe": {
                                        "id": "[resourceId('Microsoft.Network/loadBalancers/probes', '${loadBalancer.name}', '${rule.probe.name}')]"
                                    },
                                    "protocol": "Tcp"
                                }
                            }<#sep>,</#sep>
                        </#list>
                        <#if loadBalancer.type == "PRIVATE" && gatewayPrivateLbNeeded!false>,
                            <#list loadBalancer.rules as rule>
                            {
                                "name": "${rule.name}-gateway",
                                "properties": {
                                "backendAddressPool": {
                                    "id": "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '${loadBalancer.name}', '${rule.groupName}-pool')]"
                                },
                                "backendPort": ${rule.backendPort},
                                "enableFloatingIP": true,
                                "enableTcpReset": false,
                                "frontendIPConfiguration": {
                                    "id": "[concat(resourceId('Microsoft.Network/loadBalancers', '${loadBalancer.name}'), '/frontendIPConfigurations/${loadBalancer.name}-frontend-gateway')]"
                                },
                                "frontendPort": ${rule.frontendPort},
                                "idleTimeoutInMinutes": 4,
                                "loadDistribution": "Default",
                                "probe": {
                                    "id": "[resourceId('Microsoft.Network/loadBalancers/probes', '${loadBalancer.name}', '${rule.probe.name}')]"
                                },
                                "protocol": "Tcp"
                                }
                            }<#sep>,</#sep>
                            </#list>
                        </#if>
                    ],
                    "outboundRules": [
                        <#list loadBalancer.outboundRules as outboundRule>
                            {
                                "name": "${outboundRule.name}",
                                "properties": {
                                    "allocatedOutboundPorts": 1000,
                                    "backendAddressPool": {
                                        "id": "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '${loadBalancer.name}', '${outboundRule.groupName}-pool')]"
                                    },
                                    "enableTcpReset": false,
                                    "frontendIPConfigurations": [{
                                        "id": "[concat(resourceId('Microsoft.Network/loadBalancers', '${loadBalancer.name}'), '/frontendIPConfigurations/${loadBalancer.name}-frontend')]"
                                    }],
                                    "idleTimeoutInMinutes": 4,
                                    "protocol": "Tcp"
                                }
                            }<#sep>,</#sep>
                        </#list>
                    ],
                    "probes": [
                      <#list loadBalancer.probes as probe>
                      {
                        "name": "${probe.name}",
                        "properties": {
                          "intervalInSeconds": 5,
                          "numberOfProbes": 2,
                          "port": ${probe.port},
                          "protocol": "Tcp"
                        }
                      }<#if (probe_index + 1) != loadBalancer.probes?size>,</#if>
                      </#list>
                    ]
                  },
                  "sku": {
                      "name": <#if multiAz>"Standard"<#else>"${loadBalancer.sku.templateName}"</#if>
                  }
                }
                <#if loadBalancer.type == "PUBLIC" || loadBalancer.type == "OUTBOUND">
                ,{
                    "type": "Microsoft.Network/publicIPAddresses",
                    "apiVersion": "2023-06-01",
                    "name": "${loadBalancer.name}-publicIp",
                    "location": "[parameters('region')]",
                    "tags": {
                        <#if userDefinedTags?? && userDefinedTags?has_content>
                            <#list userDefinedTags?keys as key>
                            "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                            </#list>
                        </#if>
                    },
                    "sku": {
                        "name": <#if multiAz>"Standard"<#else>"${loadBalancer.sku.templateName}"</#if>
                    },
                    "properties": {
                        "publicIPAddressVersion": "IPv4",
                        "publicIPAllocationMethod": "Static"
                    }<@zoneredundant/>
                }
                </#if>
                <#if (loadBalancer_index + 1) != loadBalancers?size>,</#if>
            </#list>
     	]
}
