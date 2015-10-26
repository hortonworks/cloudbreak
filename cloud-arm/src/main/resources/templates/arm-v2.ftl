{
    "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
        "userImageStorageAccountName": {
            "type": "string",
            "defaultValue" : "${storage_account_name}"
        },
        "userAttachedDiskStorageAccountName": {
            "type": "string",
            "defaultValue" : "${attached_disk_storage_account_name}"
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
          "defaultValue" : "${admin_user_name}"
        },
        "virtualNetworkNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
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
            "defaultValue" : "${ssh_key}"
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
        "subnet1Prefix": {
           "type": "string",
           "defaultValue": "${subnet1Prefix}"
        },
        "sshIPConfigName": {
            "type": "string",
            "defaultValue": "${stackname}ipcn"
        },
        <#list groups as instanceGroup>
        <#list instanceGroup.instances as instance>
        <#if instanceGroup.type == "GATEWAY">
        "gatewaystaticipname": {
            "type": "string",
            "defaultValue": "${stackname}${instanceGroup.name?replace('_', '')}${instance.privateId}"
        },
        </#if>
        </#list>
        </#list>
        "addressPrefix": {
          "type": "string",
          "defaultValue": "${addressPrefix}"
        }
    },
  	"variables" : {
      "userImageName" : "[concat('https://',parameters('userImageStorageAccountName'),'.blob.core.windows.net/',parameters('userImageStorageContainerName'),'/',parameters('userImageVhdName'))]",
      "osDiskVhdName" : "[concat('https://',parameters('userImageStorageAccountName'),'.blob.core.windows.net/',parameters('userDataStorageContainerName'),'/',parameters('vmNamePrefix'),'osDisk')]",
      "dataDiskVhdName" : "[concat('https://',parameters('userAttachedDiskStorageAccountName'),'.blob.core.windows.net/',parameters('userDataStorageContainerName'),'/',parameters('vmNamePrefix'),'datadisk')]",
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks',parameters('virtualNetworkNamePrefix'))]",
      "subnet1Ref": "[concat(variables('vnetID'),'/subnets/',parameters('subnet1Name'))]",
      "staticIpRef": "[resourceId('Microsoft.Network/publicIPAddresses', parameters('gatewaystaticipname'))]",
      "ilbBackendAddressPoolName": "${stackname}bapn",
      "lbID": "[resourceId('Microsoft.Network/loadBalancers', parameters('loadBalancerName'))]",
      "sshIPConfig": "[concat(variables('lbID'),'/frontendIPConfigurations/', parameters('sshIPConfigName'))]",
      "ilbBackendAddressPoolID": "[concat(variables('lbID'),'/backendAddressPools/', variables('ilbBackendAddressPoolName'))]",
      "sshKeyPath" : "[concat('/home/',parameters('adminUsername'),'/.ssh/authorized_keys')]"
  	},
    "resources": [
        {
              "apiVersion": "2015-05-01-preview",
              "type": "Microsoft.Network/virtualNetworks",
              "name": "[parameters('virtualNetworkNamePrefix')]",
              "location": "[parameters('region')]",
              "properties": {
                  "addressSpace": {
                      "addressPrefixes": [
                          "[parameters('addressPrefix')]"
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
          <#list groups as instanceGroup>
          <#list instanceGroup.instances as instance>
              <#if instanceGroup.type == "GATEWAY">
              {
                    "apiVersion": "2015-05-01-preview",
                    "type": "Microsoft.Network/publicIPAddresses",
                    "name": "[concat(parameters('publicIPNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]",
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
                      "[concat('Microsoft.Network/publicIPAddresses/', parameters('publicIPNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]"
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
                      <#list ports as port>
                        {
                          "name": "endpoint${port_index}inr",
                          "properties": {
                            "frontendIPConfiguration": {
                              "id": "[variables('sshIPConfig')]"
                            },
                            "protocol": "${port_protocol}",
                            "frontendPort": "${port}",
                            "backendPort": "${port}",
                            "enableFloatingIP": false
                          }
                        }<#if (port_index + 1) != ports?size>,</#if>
                        </#list>
                      ]
                    }
                  },
              </#if>
              <#if instanceGroup.type == "CORE">
              {
                "apiVersion": "2015-05-01-preview",
                "type": "Microsoft.Network/publicIPAddresses",
                "name": "[concat(parameters('publicIPNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]",
                "location": "[parameters('region')]",
                "properties": {
                    "publicIPAllocationMethod": "Dynamic"
                }
              },
              </#if>

              {
                "apiVersion": "2015-05-01-preview",
                "type": "Microsoft.Network/networkInterfaces",
                "name": "[concat(parameters('nicNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]",
                "location": "[parameters('region')]",
                "dependsOn": [
                    <#if instanceGroup.type == "CORE">
                    "[concat('Microsoft.Network/publicIPAddresses/', parameters('publicIPNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]",
                    </#if>
                    <#if instanceGroup.type == "GATEWAY">
                    "[concat('Microsoft.Network/loadBalancers/', parameters('loadBalancerName'))]",
                    </#if>
                    "[concat('Microsoft.Network/virtualNetworks/', parameters('virtualNetworkNamePrefix'))]"
                ],
                "properties": {
                    "ipConfigurations": [
                        {
                            "name": "ipconfig1",
                            "properties": {
                                "privateIPAllocationMethod": "Dynamic",
                                <#if instanceGroup.type == "CORE">
                                "publicIPAddress": {
                                    "id": "[resourceId('Microsoft.Network/publicIPAddresses',concat(parameters('publicIPNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}'))]"
                                },
                                </#if>
                                "subnet": {
                                    "id": "[variables('subnet1Ref')]"
                                }
                                <#if instanceGroup.type == "GATEWAY">
                                ,"loadBalancerBackendAddressPools": [
                                    {
                                        "id": "[variables('ilbBackendAddressPoolID')]"
                                    }
                                ],
                                "loadBalancerInboundNatRules": [
                                <#list ports as port>
                                    {
                                        "id": "[concat(variables('lbID'),'/inboundNatRules/', 'endpoint${port_index}inr')]"
                                    }<#if (port_index + 1) != ports?size>,</#if>
                                </#list>
                                ]
                                </#if>

                            }
                        }
                    ]
                }
              },
              {
                "apiVersion": "2015-05-01-preview",
                "type": "Microsoft.Compute/virtualMachines",
                "name": "[concat(parameters('vmNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]",
                "location": "[parameters('region')]",
                "dependsOn": [
                    "[concat('Microsoft.Network/networkInterfaces/', parameters('nicNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]"
                ],
                "properties": {
                    "hardwareProfile": {
                        "vmSize": "${instanceGroup.instances[0].flavor}"
                    },
                    "osProfile": {
                        "computername": "[concat('vm', '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]",
                        "adminUsername": "[parameters('adminUsername')]",
                        <#if disablePasswordAuthentication == false>
                        "adminPassword": "${adminPassword}",
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
                        <#if instanceGroup.type == "CORE">
                        "customData": "${corecustomData}"
                        </#if>
                        <#if instanceGroup.type == "GATEWAY">
                        "customData": "${gatewaycustomData}"
                        </#if>
                    },
                    "storageProfile": {
                        "osDisk" : {
                            "name" : "[concat(parameters('vmNamePrefix'),'-osDisk', '${instanceGroup.name?replace('_', '')}', '${instance.privateId}')]",
                            "osType" : "linux",
                            "image" : {
                                "uri" : "[variables('userImageName')]"
                            },
                            "vhd" : {
                                "uri" : "[concat(variables('osDiskVhdName'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}','.vhd')]"
                            },
                            "createOption": "FromImage"
                        },
                        "dataDisks": [
                        <#list instance.volumes as volume>
                            {
                                "name": "[concat('datadisk', '${instanceGroup.name?replace('_', '')}', '${volume_index}', '${instance.privateId}')]",
                                "diskSizeGB": ${volume.size},
                                "lun":  ${volume_index},
                                "vhd": {
                                    "Uri": "[concat(variables('dataDiskVhdName'), 'datadisk', '${instanceGroup.name?replace('_', '')}', '${volume_index}', '${instance.privateId}', '.vhd')]"
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
                                "id": "[resourceId('Microsoft.Network/networkInterfaces',concat(parameters('nicNamePrefix'), '${instanceGroup.name?replace('_', '')}', '${instance.privateId}'))]"
                            }
                        ],
                        "inputEndpoints": [
                            <#list ports as port>
                            {
                                "enableDirectServerReturn": "False",
                                "endpointName": "endpoint${port_index}",
                                "privatePort": ${port},
                                "publicPort": ${port},
                                "protocol": "${port_protocol}"
                            }<#if (port_index + 1) != ports?size>,</#if>
                            </#list>
                        ]
                    }
                }
              }<#if (instance_index + 1) != instanceGroup.instances?size>,</#if>
          </#list>
          <#if (instanceGroup_index + 1) != groups?size>,</#if>
          </#list>

  	]
}