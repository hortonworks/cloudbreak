<#setting number_format="computer">
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
        <#list groups?keys as instanceGroup>
        <#list groups[instanceGroup] as instance>
        <#if instanceGroup == "GATEWAY">
        "gatewaystaticipname": {
            "type": "string",
            "defaultValue": "${stackname}${instance.instanceId}"
        },
        </#if>
        </#list>
        </#list>
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
      "subnet1Ref": "[concat(variables('vnetID'),'/subnets/',parameters('existingSubnetName'))]",
      <#else>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks',parameters('virtualNetworkNamePrefix'))]",
      "subnet1Ref": "[concat(variables('vnetID'),'/subnets/',parameters('subnet1Name'))]",
      </#if>
      "staticIpRef": "[resourceId('Microsoft.Network/publicIPAddresses', parameters('gatewaystaticipname'))]",
      "ilbBackendAddressPoolName": "${stackname}bapn",
      <#list igs as group>
      "${group?replace('_', '')}secGroupName": "${group?replace('_', '')}${stackname}sg",
      </#list>
      "lbID": "[resourceId('Microsoft.Network/loadBalancers', parameters('loadBalancerName'))]",
      "sshIPConfig": "[concat(variables('lbID'),'/frontendIPConfigurations/', parameters('sshIPConfigName'))]",
      "ilbBackendAddressPoolID": "[concat(variables('lbID'),'/backendAddressPools/', variables('ilbBackendAddressPoolName'))]",
      "sshKeyPath" : "[concat('/home/',parameters('adminUsername'),'/.ssh/authorized_keys')]"
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
                   ]

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
                         "[concat('Microsoft.Network/publicIPAddresses/', parameters('publicIPNamePrefix'), '${instance.instanceId}')]"
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
                             "name": "[variables('ilbBackendAddressPoolName')]"
                           }
                         ],
                         "inboundNatRules": [
                         <#list securities[instance.groupName] as port>
                           {
                             "name": "endpoint${port_index}inr",
                             "properties": {
                               "frontendIPConfiguration": {
                                 "id": "[variables('sshIPConfig')]"
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
                       "[concat('Microsoft.Network/publicIPAddresses/', parameters('publicIPNamePrefix'), '${instance.instanceId}')]"
                       </#if>
                       <#if instanceGroup == "GATEWAY">
                       "[concat('Microsoft.Network/loadBalancers/', parameters('loadBalancerName'))]"
                       </#if>
                       <#if !existingVPC>
                       ,"[concat('Microsoft.Network/virtualNetworks/', parameters('virtualNetworkNamePrefix'))]"
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
                                       "id": "[variables('subnet1Ref')]"
                                   }
                                   <#if instanceGroup == "GATEWAY">
                                   ,"loadBalancerBackendAddressPools": [
                                       {
                                           "id": "[variables('ilbBackendAddressPoolID')]"
                                       }
                                   ],
                                   "loadBalancerInboundNatRules": [
                                   <#list securities[instance.groupName] as port>
                                       {
                                           "id": "[concat(variables('lbID'),'/inboundNatRules/', 'endpoint${port_index}inr')]"
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
                       "[concat('Microsoft.Network/networkInterfaces/', parameters('nicNamePrefix'), '${instance.instanceId}')]"
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
                                           "path": "[variables('sshKeyPath')]",
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
                               "name" : "[concat(parameters('vmNamePrefix'),'-osDisk', '${instance.instanceId}')]",
                               "osType" : "linux",
                               "image" : {
                                   "uri" : "[variables('userImageName')]"
                               },
                               "vhd" : {
                                   "uri" : "[concat(variables('osDiskVhdName'), '${instance.instanceId}','.vhd')]"
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
                                       "Uri": "[concat('${instance.attachedDiskStorageUrl}',parameters('userDataStorageContainerName'),'/',parameters('vmNamePrefix'),'datadisk','${instance.instanceId}', '${volume_index}', '.vhd')]"
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