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
                <#if group.availabilitySetName?? && group.availabilitySetName?has_content>
            {
                "type": "Microsoft.Compute/availabilitySets",
                "name": "[variables('${group.compressedName}AsName')]",
                "apiVersion": "2017-03-30",
                "location": "[resourceGroup().location]",
                <#if group.managedDisk == true>
                "sku": {
                    "name": "Aligned"
                },
                </#if>
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
                 <#if !noFirewallRules>
                 "dependsOn": [
                    <#list igs as group>
                        <#if ! securityGroups[group.name]?? || ! securityGroups[group.name]?has_content>
                        "[concat('Microsoft.Network/networkSecurityGroups/', variables('${group.compressedName}secGroupName'))]"<#if (group_index + 1) != igs?size -securityGroups?size>,</#if>
                        </#if>
                    </#list>
                 ],
                 </#if>
                 "tags": {
                <#if userDefinedTags?? && userDefinedTags?has_content>
                    <#list userDefinedTags?keys as key>
                      "${key}": "${userDefinedTags[key]}",
                      </#list>
                </#if>
                    "cb-resource-type": "${network_resource}"
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
             <#if !noFirewallRules>
             <#list igs as group>
             <#if ! securityGroups[group.name]?? || ! securityGroups[group.name]?has_content>
             {
               "apiVersion": "2015-05-01-preview",
               "type": "Microsoft.Network/networkSecurityGroups",
               "name": "[variables('${group.compressedName}secGroupName')]",
               "location": "[parameters('region')]",
               "tags": {

                 <#if userDefinedTags?? && userDefinedTags?has_content>
                     <#list userDefinedTags?keys as key>
                    "${key}": "${userDefinedTags[key]}",
                    </#list>
                 </#if>
                    "cb-resource-type": "${network_resource}"
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
             </#if>
             <#list groups?keys as instanceGroup>
             <#list groups[instanceGroup] as instance>
                 <#if !noPublicIp>
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/publicIPAddresses",
                   "name": "[concat(parameters('publicIPNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "tags": {
                     <#if userDefinedTags?? && userDefinedTags?has_content>
                         <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}",
                        </#list>
                     </#if>
                        "cb-resource-type": "${ipaddress_resource}"
                     },
                   "properties": {
                       <#if instanceGroup == "GATEWAY">
                       "publicIPAllocationMethod": "Static"
                       <#else>
                       "publicIPAllocationMethod": "Dynamic"
                       </#if>
                   }
                 },
                 </#if>
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/networkInterfaces",
                   "name": "[concat(parameters('nicNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "tags": {
                 <#if userDefinedTags?? && userDefinedTags?has_content>
                     <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}",
                        </#list>
                 </#if>
                        "cb-resource-type": "${network_resource}"
                    },
                   "dependsOn": [
                       <#if !noFirewallRules>
                       <#if ! securityGroups[instance.groupName]?? || ! securityGroups[instance.groupName]?has_content>
                       "[concat('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                       </#if>
                       </#if>
                       <#if !noPublicIp>
                       <#if !noFirewallRules>
                       <#if ! securityGroups[instance.groupName]?? || ! securityGroups[instance.groupName]?has_content>
                       ,
                       </#if>
                       </#if>
                       "[concat('Microsoft.Network/publicIPAddresses/', parameters('publicIPNamePrefix'), '${instance.instanceId}')]"
                       </#if>
                       <#if !existingVPC>
                       <#if !noFirewallRules || !noPublicIp>,</#if>
                       "[concat('Microsoft.Network/virtualNetworks/', parameters('virtualNetworkNamePrefix'))]"
                       </#if>
                   ],
                   "properties": {
                       <#if !noFirewallRules>
                       "networkSecurityGroup":{
                           <#if securityGroups[instance.groupName]?? && securityGroups[instance.groupName]?has_content>
                           "id": "${securityGroups[instance.groupName]}"
                           <#else>
                            "id": "[resourceId('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                           </#if>
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
                               }
                           }
                       ]
                   }
                 },
                 {
                   "apiVersion": "2018-04-01",
                   "type": "Microsoft.Compute/virtualMachines",
                   "name": "[concat(parameters('vmNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "dependsOn": [
                    <#if instance.availabilitySetName?? && instance.availabilitySetName?has_content>
                       "[concat('Microsoft.Compute/availabilitySets/', '${instance.availabilitySetName}')]",
                    </#if>
                       "[concat('Microsoft.Network/networkInterfaces/', parameters('nicNamePrefix'), '${instance.instanceId}')]"
                   ],
                   "tags": {

                    <#if userDefinedTags?? && userDefinedTags?has_content>
                        <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}",
                        </#list>
                    </#if>
                        "cb-resource-type": "${instance_resource}"
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
                               <#if instance.managedDisk == false>
                               "image" : {
                                    "uri" : "[variables('userImageName')]"
                               },
                               "vhd" : {
                                    "uri" : "[concat(variables('osDiskVhdName'), '${instance.instanceId}','.vhd')]"
                               },
                               <#else>
                               "diskSizeGB": "${instance.rootVolumeSize}",
                               "managedDisk": {
                                    "storageAccountType": "${instance.attachedDiskStorageType}"
                               },
                               </#if>
                               "name" : "[concat(parameters('vmNamePrefix'),'-osDisk', '${instance.instanceId}')]",
                               "osType" : "linux",
                               "createOption": "FromImage"
                           },
                           <#if instance.managedDisk == true>
                           "imageReference": {
                               <#if instance.customImageId?? && instance.customImageId?has_content>
                               "id": "${instance.customImageId}"
                               <#else>
                               "id": "${customImageId}"
                               </#if>
                           }
                           </#if>
                       },
                       "networkProfile": {
                           "networkInterfaces": [
                               {
                                   "id": "[resourceId('Microsoft.Network/networkInterfaces',concat(parameters('nicNamePrefix'), '${instance.instanceId}'))]"
                               }
                           ]
                       }
                       <#if instance.managedDisk == false>
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