{
    "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
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
        "existingSubnetName" : {
          "type": "string",
          "defaultValue" : "${existingSubnetName}"
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
        "loadBalancerName" : {
            "type" : "string",
            "defaultValue" : "${stackname}lb"
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
        },
        <#list groups?keys as instanceGroup>
        <#list groups[instanceGroup] as instance>
        <#if instanceGroup == "GATEWAY">
        "gatewaystaticipname": {
            "type": "string",
            "defaultValue": "${stackname}${instance.instanceId}"
        }
        </#if>
        </#list>
        </#list>
    },
  	"variables" : {
      "staticIpRef": "[resourceId('Microsoft.Network/publicIPAddresses', parameters('gatewaystaticipname'))]",
      <#list igs as group>
      "${group?replace('_', '')}secGroupName": "${group?replace('_', '')}${stackname}sg",
      </#list>
  	},
    "resources": [
           <#if !existingVPC>
           {
                 "apiVersion": "2015-05-01-preview",
                 "type": "Microsoft.Network/virtualNetworks",
                 "dependsOn": [
                    <#list igs as group>
                        "[concat('Microsoft.Network/networkSecurityGroups/', variables('${group?replace('_', '')}secGroupName'))]"<#if (group_index + 1) != igs?size>,</#if>
                    </#list>
                 ],
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
             {
               "apiVersion": "2015-05-01-preview",
               "type": "Microsoft.Network/networkSecurityGroups",
               "name": "[variables('${group?replace('_', '')}secGroupName')]",
               "location": "[parameters('region')]",
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
                   },
                   <#list securities[group] as port>
                   {
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
                   }<#if (port_index + 1) != securities[group]?size>,</#if>
                   </#list>
                   ],

               }
             },
             </#list>
             <#list groups?keys as instanceGroup>
             <#list groups[instanceGroup] as instance>
                 <#if instanceGroup == "GATEWAY">
                 {
                       "apiVersion": "2015-05-01-preview",
                       "type": "Microsoft.Network/publicIPAddresses",
                       "name": "[concat(parameters('publicIPNamePrefix'), '${instance.instanceId}')]",
                       "location": "[parameters('region')]",
                       "properties": {
                           "publicIPAllocationMethod": "Static"
                       }
                 },
                 {
                       "apiVersion": "2015-05-01-preview",
                       "type": "Microsoft.Network/loadBalancers",
                       "name": "[parameters('loadBalancerName')]",
                       "location": "[parameters('region')]",
                       "dependsOn": [
                         "[concat('${publicIpAddressId}', '${instance.instanceId}')]"
                       ],
                       "properties": {
                         "frontendIPConfigurations": [
                           {
                             "name": "[parameters('sshIPConfigName')]",
                             "properties": {
                               "publicIPAddress": {
                                 "id": "[variables('staticIpRef')]"
                               }
                             }
                           }
                         ],
                         "backendAddressPools": [
                           {
                             "name": "${stackname}bapn"
                           }
                         ],
                         "inboundNatRules": [
                         <#list securities[instance.groupName] as port>
                           {
                             "name": "endpoint${port_index}inr",
                             "properties": {
                               "frontendIPConfiguration": {
                                 "id": "[concat(resourceId('${lbID}'), '${ipConfigurationsAddress}')]"
                               },
                               "protocol": "${port.protocol}",
                               "frontendPort": "${port.port}",
                               "backendPort": "${port.port}",
                               "enableFloatingIP": false
                             }
                           }<#if (port_index + 1) != securities[instance.groupName]?size>,</#if>
                           </#list>
                         ]
                       }
                     },
                 </#if>
                 <#if instanceGroup == "CORE">
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/publicIPAddresses",
                   "name": "[concat(parameters('publicIPNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "properties": {
                       "publicIPAllocationMethod": "Dynamic"
                   }
                 },
                 </#if>

                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/networkInterfaces",
                   "name": "[concat(parameters('nicNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "dependsOn": [
                       <#if instanceGroup == "CORE">
                       "[concat('${publicIpAddressId}', '${instance.instanceId}')]"
                       </#if>
                       <#if instanceGroup == "GATEWAY">
                       "${loadBalancerAddress}"
                       </#if>
                       <#if !existingVPC>
                       ,"${virtualNetworkAddress}"
                       </#if>
                       ,"[concat('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                   ],

                   "properties": {
                       "networkSecurityGroup":{
                            "id": "[resourceId('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                       },
                       "ipConfigurations": [
                           {
                               "name": "ipconfig1",
                               "properties": {
                                   "privateIPAllocationMethod": "Dynamic",
                                   <#if instanceGroup == "CORE">
                                   "publicIPAddress": {
                                       "id": "[resourceId('Microsoft.Network/publicIPAddresses',concat(parameters('publicIPNamePrefix'), '${instance.instanceId}'))]"
                                   },
                                   </#if>
                                   "subnet": {
                                       "id": "[concat(resourceId('${vnetID}'), '${subnet1Address}')]"
                                   }
                                   <#if instanceGroup == "GATEWAY">
                                   ,"loadBalancerBackendAddressPools": [
                                       {
                                           "id": "[concat(resourceId('${lbID}'), ${ilbBackendAddress})]"
                                       }
                                   ],
                                   "loadBalancerInboundNatRules": [
                                   <#list securities[instance.groupName] as port>
                                       {
                                           "id": "[concat(resourceId('${lbID}'), '/inboundNatRules/', 'endpoint${port_index}inr')]"
                                       }<#if (port_index + 1) != securities[instance.groupName]?size>,</#if>
                                   </#list>
                                   ]
                                   </#if>

                               }
                           }
                       ]
                   }
                 },
                 {
                   "apiVersion": "2015-06-15",
                   "type": "Microsoft.Compute/virtualMachines",
                   "name": "[concat(parameters('vmNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "dependsOn": [
                       "[concat('${networkInterfaceAddress}', '${instance.instanceId}')]"
                   ],
                   "properties": {
                       "hardwareProfile": {
                           "vmSize": "${instance.flavor}"
                       },
                       "osProfile": {
                           "computername": "[concat('vm', '${instance.instanceId}')]",
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
                                           "path": "${sshKeyPath}",
                                           "keyData": "[parameters('sshKeyData')]"
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
                               "name" : "[concat('${osDiskName}', '${instance.instanceId}')]",
                               "osType" : "linux",
                               "image" : {
                                   "uri" : "${userImageName}"
                               },
                               "vhd" : {
                                   "uri" : "[concat('${osDiskVhdName}', '${instance.instanceId}','.vhd')]"
                               },
                               "createOption": "FromImage"
                           },
                           "dataDisks": [
                           <#list instance.volumes as volume>
                               {
                                   "name": "[concat('datadisk', '${instance.instanceId}', '${volume_index}')]",
                                   "diskSizeGB": ${volume.size},
                                   "lun":  ${volume_index},
                                   "vhd": {
                                       "Uri": "[concat('${instance.attachedDiskStorageUrl}','${dataDiskAddress}','${instance.instanceId}', '${volume_index}', '.vhd')]"
                                   },
                                   "caching": "None",
                                   "createOption": "Empty"
                               } <#if (volume_index + 1) != instance.volumes?size>,</#if>
                           </#list>
                           ]
                       },
                       "networkProfile": {
                           "networkInterfaces": [
                               {
                                   "id": "[resourceId('Microsoft.Network/networkInterfaces',concat(parameters('nicNamePrefix'), '${instance.instanceId}'))]"
                               }
                           ]
                       }
                       <#if instance.bootDiagnosticsEnabled>
                       ,"diagnosticsProfile": {
                         "bootDiagnostics": {
                           "enabled": true,
                           "storageUri": "${instance.attachedDiskStorageUrl}"
                         }
                       }
                       </#if>
                   }
                 }<#if (instance_index + 1) != groups[instanceGroup]?size>,</#if>
             </#list>
             <#if (instanceGroup_index + 1) != groups?size>,</#if>
             </#list>

     	]
}