<#setting number_format="computer">
{
    "$schema": "https://schema.management.azure.com/schemas/2019-08-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
        "userImageStorageAccountName": {
            "type": "string",
            "defaultValue" : "${storage_account_name}"
        },
        "userImageStorageContainerName" : {
            "type" : "string",
            "defaultValue" : "${image_storage_container_name}"
        },
        "userDataStorageContainerName" : {
            "type" : "string",
            "defaultValue" : "${storage_container_name}"
        },
        "userImageVhdName" : {
            "type" : "string",
            "defaultValue" : "${storage_vhd_name}"
        },
        "adminUsername" : {
          "type" : "string",
          "defaultValue" : "${credential.loginUserName}"
        },
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
        "vmNamePrefix" :{
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "publicIPNamePrefix" :{
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "nicNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "sshKeyData" : {
            "type" : "string",
            "defaultValue" : "${credential.publicKey}"
        },
        "region" : {
          "type" : "string",
          "defaultValue" : "${region}"
        },
        "subnet1Name": {
            "type": "string",
            "defaultValue": "${stackname}subnet"
        },
        <#if !existingVPC>
        "subnet1Prefix": {
           "type": "string",
           "defaultValue": "${subnet1Prefix}"
        },
        </#if>
        "sshIPConfigName": {
            "type": "string",
            "defaultValue": "${stackname}ipcn"
        }
    },
  	"variables" : {
      "userImageName" : "[concat('https://',parameters('userImageStorageAccountName'),'.blob.core.windows.net/',parameters('userImageStorageContainerName'),'/',parameters('userImageVhdName'))]",
      "osDiskVhdName" : "[concat('https://',parameters('userImageStorageAccountName'),'.blob.core.windows.net/',parameters('userDataStorageContainerName'),'/',parameters('vmNamePrefix'),'osDisk')]",
      <#if existingVPC>
      "vnetID": "[resourceId(parameters('resourceGroupName'),'Microsoft.Network/virtualNetworks',parameters('existingVNETName'))]",
      <#else>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks',parameters('virtualNetworkNamePrefix'))]",
      </#if>
      <#list igs as group>
      "${group.compressedName}secGroupName": "${group.compressedName}${stackname}sg",
          <#if group.availabilitySetName?? && group.availabilitySetName?has_content>
          "${group.compressedName}AsName": "${group.availabilitySetName}",
          "${group.compressedName}AsFaultDomainCount": ${group.platformFaultDomainCount},
          "${group.compressedName}AsUpdateDomainCount": ${group.platformUpdateDomainCount},
          </#if>
      </#list>
      "sshKeyPath" : "[concat('/home/',parameters('adminUsername'),'/.ssh/authorized_keys')]"
  	},
    "resources": [
            <#list igs as group>
                <#if !isUpscale && group.availabilitySetName?? && group.availabilitySetName?has_content>
            {
                "type": "Microsoft.Compute/availabilitySets",
                "name": "[variables('${group.compressedName}AsName')]",
                "apiVersion": "2018-04-01",
                "location": "[parameters('region')]",
                "sku": {
                    "name": "Aligned"
                },
                "properties": {
                    "platformFaultDomainCount": "[variables('${group.compressedName}AsFaultDomainCount')]",
                    "platformUpdateDomainCount": "[variables('${group.compressedName}AsUpdateDomainCount')]"
                }
            },
                </#if>
            </#list>
            <#if !existingVPC>
            {
                 "apiVersion": "2015-05-01-preview",
                 "type": "Microsoft.Network/virtualNetworks",
                 "dependsOn": [
                    <#list igs as group>
                        <#if !isUpscale && (! securityGroups[group.name]?? || ! securityGroups[group.name]?has_content)>
                        "[concat('Microsoft.Network/networkSecurityGroups/', variables('${group.compressedName}secGroupName'))]"<#if (group_index + 1) != igs?size -securityGroups?size>,</#if>
                        </#if>
                    </#list>
                 ],
                 "tags": {
                <#if userDefinedTags?? && userDefinedTags?has_content>
                    <#list userDefinedTags?keys as key>
                      "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                      </#list>
                </#if>
                },
                 "name": "[parameters('virtualNetworkNamePrefix')]",
                 "location": "[parameters('region')]",
                 "properties": {
                     "addressSpace": {
                         "addressPrefixes": [
                             "[parameters('subnet1Prefix')]"
                         ]
                     },
                     "subnets": [
                         {
                             "name": "[parameters('subnet1Name')]",
                             "properties": {
                                 "addressPrefix": "[parameters('subnet1Prefix')]"
                             }
                         }
                     ]
                 }
             },
             </#if>
             <#list igs as group>
             <#if !isUpscale && (! securityGroups[group.name]?? || ! securityGroups[group.name]?has_content)>
             {
               "apiVersion": "2015-05-01-preview",
               "type": "Microsoft.Network/networkSecurityGroups",
               "name": "[variables('${group.compressedName}secGroupName')]",
               "location": "[parameters('region')]",
               "tags": {

                 <#if userDefinedTags?? && userDefinedTags?has_content>
                     <#list userDefinedTags?keys as key>
                    "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                    </#list>
                 </#if>
               },
               "properties": {
               "securityRules": [
                   {
                       "name": "endpoint1outr",
                       "properties": {
                           "protocol": "*",
                           "sourcePortRange": "*",
                           "destinationPortRange": "*",
                           "sourceAddressPrefix": "*",
                           "destinationAddressPrefix": "*",
                           "access": "Allow",
                           "priority": 101,
                           "direction": "Outbound"
                       }
                   }
                   <#list securities[group.name] as port>
                   ,{
                       "name": "endpoint${port_index}inr",
                       "properties": {
                           "protocol": "${port.capitalProtocol}",
                           "sourcePortRange": "*",
                           "destinationPortRange": "${port.port}",
                           "sourceAddressPrefix": "${port.cidr}",
                           "destinationAddressPrefix": "*",
                           "access": "Allow",
                           "priority": ${port_index + 102},
                           "direction": "Inbound"
                       }
                   }
                   </#list>
                   ]
               }
             },
             </#if>
             </#list>
             <#list groups?keys as instanceGroup>
             <#list groups[instanceGroup] as instance>
                 <#assign createAndNoSecGroup = !isUpscale && (! securityGroups[instance.groupName]?? || ! securityGroups[instance.groupName]?has_content)>
                 <#if !noPublicIp>
                 {
                   "apiVersion": "2017-08-01",
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
                   <#if loadBalancerMapping[instance.groupName]?? && (loadBalancers?filter(loadBalancer -> loadBalancer.sku == "STANDARD")?size > 0)>
                     "sku": {
                         "name": "Standard",
                         "tier": "Regional"
                     },
                   </#if>
                   "properties": {
                       <#if instanceGroup == "GATEWAY" || (instance.availabilitySetName?? && instance.availabilitySetName?has_content)>
                       "publicIPAllocationMethod": "Static"
                       <#else>
                       "publicIPAllocationMethod": "Dynamic"
                       </#if>
                   }
                 },
                 </#if>
                 {
                   "apiVersion": "2016-09-01",
                   "type": "Microsoft.Network/networkInterfaces",
                   "name": "[concat(parameters('nicNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "tags": {
                     <#if userDefinedTags?? && userDefinedTags?has_content>
                         <#list userDefinedTags?keys as key>
                            "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                            </#list>
                     </#if>
                    },
                   "dependsOn": [
                       <#if createAndNoSecGroup>
                           "[concat('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                       </#if>

                       <#if !noPublicIp>
                           <#if createAndNoSecGroup>
                               ,
                           </#if>

                           "[concat('Microsoft.Network/publicIPAddresses/', parameters('publicIPNamePrefix'), '${instance.instanceId}')]"
                       </#if>

                       <#if !existingVPC>
                           <#if !noPublicIp || createAndNoSecGroup>
                               ,
                          </#if>
                           "[concat('Microsoft.Network/virtualNetworks/', parameters('virtualNetworkNamePrefix'))]"
                       </#if>

                       <#if loadBalancerMapping[instance.groupName]?? && (loadBalancerMapping[instance.groupName]?size > 0)>
                           <#if createAndNoSecGroup || !noPublicIp || !existingVPC>,</#if>
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
                 },
                 {
                   "apiVersion": "2019-07-01",
                   "type": "Microsoft.Compute/virtualMachines",
                   "name": "[concat(parameters('vmNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   <#if usePartnerCenter>
                   "plan": {
                        "name": "${marketplaceImageDetails.planId}",
                        "product": "${marketplaceImageDetails.offerId}",
                        "publisher": "${marketplaceImageDetails.publisherId}"
                   },
                   </#if>
                    <#if instance.managedIdentity?? && instance.managedIdentity?has_content>
                    "identity": {
                        "type": "userAssigned",
                        "userAssignedIdentities": {
                            "${instance.managedIdentity}": {
                            }
                        }
                     },
                    </#if>
                   "dependsOn": [
                    <#if !isUpscale && instance.availabilitySetName?? && instance.availabilitySetName?has_content>
                       "[concat('Microsoft.Compute/availabilitySets/', '${instance.availabilitySetName}')]",
                    </#if>
                       "[concat('Microsoft.Network/networkInterfaces/', parameters('nicNamePrefix'), '${instance.instanceId}')]"
                   ],
                   "tags": {

                    <#if userDefinedTags?? && userDefinedTags?has_content>
                        <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                        </#list>
                    </#if>
                    },
                   "properties": {
                        <#if instance.availabilitySetName?? && instance.availabilitySetName?has_content>
                        "availabilitySet": {
                            "id": "[resourceId('Microsoft.Compute/availabilitySets', '${instance.availabilitySetName}')]"
                        },
                        </#if>
                       "hardwareProfile": {
                           "vmSize": "${instance.flavor}"
                       },
                       "osProfile": {
                           "computername": "${instance.instanceName}",
                           "adminUsername": "[parameters('adminUsername')]",
                           <#if disablePasswordAuthentication == false>
                           "adminPassword": "${credential.password}",
                           </#if>
                           "linuxConfiguration": {
                               "disablePasswordAuthentication": "${disablePasswordAuthentication?c}",
                               "ssh": {
                                   "publicKeys": [
                                    <#if disablePasswordAuthentication == true>
                                       {
                                           "path": "[variables('sshKeyPath')]",
                                           "keyData": "[trim(parameters('sshKeyData'))]"
                                       }
                                    </#if>
                                   ]
                               }
                           },
                           <#if instanceGroup == "CORE">
                           "customData": "${corecustomData}"
                           </#if>
                           <#if instanceGroup == "GATEWAY">
                           "customData": "${gatewaycustomData}"
                           </#if>
                       },
                       "storageProfile": {
                           "osDisk" : {
                               "diskSizeGB": "${instance.rootVolumeSize}",
                               "managedDisk": {
                                    <#if instance.managedDiskEncryptionWithCustomKeyEnabled>
                                    "diskEncryptionSet": {
                                        "id": "${instance.diskEncryptionSetId}"
                                    },
                                    </#if>
                                    "storageAccountType": "${instance.attachedDiskStorageType}"
                               },
                               "name" : "[concat(parameters('vmNamePrefix'),'-osDisk', '${instance.instanceId}')]",
                               "osType" : "linux",
                               "createOption": "FromImage"
                           },
                           "imageReference": {
                               <#if usePartnerCenter>
                                    "publisher": "${marketplaceImageDetails.publisherId}",
                                    "offer": "${marketplaceImageDetails.offerId}",
                                    "sku": "${marketplaceImageDetails.planId}",
                                    "version": "${marketplaceImageDetails.version}"
                               <#else>
                                   <#if instance.customImageId?? && instance.customImageId?has_content>
                                   "id": "${instance.customImageId}"
                                   <#else>
                                   "id": "${customImageId}"
                                   </#if>
                               </#if>
                           }
                       },
                       "networkProfile": {
                           "networkInterfaces": [
                               {
                                   "id": "[resourceId('Microsoft.Network/networkInterfaces',concat(parameters('nicNamePrefix'), '${instance.instanceId}'))]"
                               }
                           ]
                       }
                   }
                 }<#if (instance_index + 1) != groups[instanceGroup]?size>,</#if>
             </#list>
             <#if (instanceGroup_index + 1) != groups?size>,</#if>
             </#list>
            <#list loadBalancers as loadBalancer>
                ,{
                  "apiVersion": "2020-05-01",
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
                        }
                      }
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
                                    "enableFloatingIP": false,
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
                            }<#if (rule_index + 1) != loadBalancer.rules?size>,</#if>
                        </#list>
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
                            }<#if (outboundRule_index + 1) != loadBalancer.outboundRules?size>,</#if>
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
                      "name": "${loadBalancer.sku.templateName}"
                  }
                }
                <#if loadBalancer.type == "PUBLIC" || loadBalancer.type == "OUTBOUND">
                ,{
                    "type": "Microsoft.Network/publicIPAddresses",
                    "apiVersion": "2020-06-01",
                    "name": "${loadBalancer.name}-publicIp",
                    "location": "[parameters('region')]",
                    "sku": {
                        "name": "${loadBalancer.sku.templateName}"
                    },
                    "properties": {
                        "publicIPAddressVersion": "IPv4",
                        "publicIPAllocationMethod": "Static"
                    }
                }
                </#if>
            </#list>
     	]
}
