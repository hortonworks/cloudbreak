{
    "$schema": "https://schema.management.azure.com/schemas/2019-08-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters": {
        "dbServerName": {
            "type": "string",
            "defaultValue" : "${dbServerName}",
            "metadata": {
                "description": "Name of the database server resource."
            }
        },
        "skuTier": {
            "type": "string",
            "defaultValue" : "${skuTier!"MemoryOptimized"}",
            "allowedValues": [
                "Basic",
                "GeneralPurpose",
                "MemoryOptimized"
            ],
            "metadata": {
                "description": "The tier of the particular SKU, e.g. Basic. - Basic, GeneralPurpose, MemoryOptimized"
            }
        },
        "skuFamily": {
            "type": "string",
            "defaultValue" : "${skuFamily!"Gen5"}",
            "metadata": {
                "description": "The family of hardware."
            }
        },
        <#if skuCapacity??>
        "skuCapacity": {
            "type": "int",
            "defaultValue" : ${skuCapacity?c},
            "metadata": {
                "description": "The scale up/out capacity, representing server's compute units."
            }
        },
        </#if>
        "skuSizeMB": {
            "type": "int",
            "defaultValue" : ${(skuSizeMB!10240)?c},
            "metadata": {
                "description": "The size code, to be interpreted by resource as appropriate."
            }
        },
        "skuName": {
            "type": "string",
            "defaultValue": "${skuName!"MO_Gen5_4"}",
            "allowedValues": [
                "GP_Gen5_2",
                "GP_Gen5_4",
                "GP_Gen5_8",
                "GP_Gen5_16",
                "GP_Gen5_32",
                "MO_Gen5_2",
                "MO_Gen5_4",
                "MO_Gen5_8",
                "MO_Gen5_16",
                "MO_Gen5_32"
            ],
            "metadata": {
                "description": "Azure database for PostgreSQL SKU name."
            }
        },
        "dbVersion": {
            "type": "string",
            "defaultValue": "${dbVersion}",
            "metadata": {
                "description": "PostgreSQL version."
            }
        },
        "adminLoginName": {
            "type": "securestring",
            "defaultValue" : "${adminLoginName}",
            "minLength": 1,
            "metadata": {
                "description": "The administrator login name for the database server."
            }
        },
        "useSslEnforcement": {
            "type": "bool",
            "defaultValue": ${(useSslEnforcement!false)?c},
            "metadata": {
                "description": "Enable SSL enforcement or not when connecting to the server."
            }
        },
        "backupRetentionDays": {
            "type": "int",
            "defaultValue": ${(backupRetentionDays!7)?c},
            "minValue": 7,
            "maxValue": 35,
            "metadata": {
                "description": "Backup retention days for the server."
            }
        },
        "geoRedundantBackup": {
            "type": "bool",
            "defaultValue": ${(geoRedundantBackup!true)?c},
            "metadata": {
                "description": "Enable Geo-redundant or not for server backup."
            }
        },
        "storageAutoGrow": {
            "type": "bool",
            "defaultValue": ${(storageAutoGrow!false)?c},
            "metadata": {
                "description": "Enable Storage Auto Grow."
            }
        },
        "location": {
            "type": "string",
            "defaultValue": "${location}",
            "metadata": {
                "description": "The location in which the database should be deployed."
            }
        },
        "batchSize": {
            "type": "int",
            "defaultValue": ${batchSize},
            "metadata": {
                "description": "batchsize of resource creation."
            }
        },
        "adminPassword": {
            "type": "securestring",
            "defaultValue" : "${adminPassword}",
            "minLength": 8,
            "maxLength": 128,
            "metadata": {
                "description": "The administrator password for the database server."
            }
        },
        "subnets": {
            "type": "string",
            "defaultValue": "${subnets}",
            "metadata": {
                "description": "The subnets to connect through virtual network rules. It is expected that each subnet has a service endpoint defined."
            }
        },
        "serverTags": {
            "type":"object",
            "defaultValue": {
                <#if serverTags?? && serverTags?has_content>
                    <#list serverTags?keys as key>
                      "${key}": "${serverTags[key]}"<#if key_has_next>,</#if>
                      </#list>
                </#if>
            },
            "metadata": {
                "description": "User defined tags to be attached to the resource."
            }
        },
        "privateEndpointName": {
            "defaultValue": "${privateEndpointName}",
            "type": "String"
        }
        <#if dataEncryption == true>
        , "keyVaultName": {
              "type": "string",
              "defaultValue" : "${keyVaultName}",
              "metadata": {
                "description": "Key vault name where the key to use is stored"
              }
        },
        "keyVaultResourceGroupName": {
              "type": "string",
              "defaultValue" : "${keyVaultResourceGroupName}",
              "metadata": {
                "description": "Key vault resource group name where it is stored"
              }
        },
        "keyName": {
              "type": "string",
              "defaultValue" : "${keyName}",
              "metadata": {
                "description": "Key name in the key vault to use as encryption protector"
              }
        },
        "keyVersion": {
              "type": "string",
              "defaultValue" : "${keyVersion}",
              "metadata": {
                "description": "Version of the key in the key vault to use as encryption protector"
              }
        }
        </#if>
    },
    "variables": {
        "apiVersion":"2017-12-01",
        "sslEnforcementString":"[if(parameters('useSslEnforcement'), 'Enabled', 'Disabled')]",
        "minimalTlsVersionString":"[if(parameters('useSslEnforcement'), 'TLS1_2', 'TLSEnforcementDisabled')]",
        "geoRedundantBackupString":"[if(parameters('geoRedundantBackup'), 'Enabled', 'Disabled')]",
        "storageAutoGrowString":"[if(parameters('storageAutoGrow'), 'Enabled', 'Disabled')]",
        "subnetList": "[split(parameters('subnets'),',')]"
        <#if dataEncryption == true>
        , "serverKeyName": "[concat(parameters('keyVaultName'), '_', parameters('keyName'), '_', parameters('keyVersion'))]"
        </#if>
    },
    "resources": [
            {
                "name": "[parameters('dbServerName')]",
                "type": "Microsoft.DBforPostgreSQL/servers",
                "apiVersion": "[variables('apiVersion')]",
                "identity": {
                    "type": "SystemAssigned"
                },
                "sku": {
                    "name": "[parameters('skuName')]",
                    "tier": "[parameters('skuTier')]",
                    <#if skuCapacity??>
                    "capacity": "[parameters('skuCapacity')]",
                    </#if>
                    "size": "[parameters('skuSizeMB')]",
                    "family": "[parameters('skuFamily')]"
                },
                "tags": "[parameters('serverTags')]",
                "properties": {
                    "version": "[parameters('dbVersion')]",
                    "administratorLogin": "[parameters('adminLoginName')]",
                    "administratorLoginPassword": "[parameters('adminPassword')]",
                    "sslEnforcement": "[variables('sslEnforcementString')]",
                    "minimalTlsVersion": "[variables('minimalTlsVersionString')]",
                    "storageProfile": {
                        "backupRetentionDays": "[parameters('backupRetentionDays')]",
                        "geoRedundantBackup": "[variables('geoRedundantBackupString')]",
                        "storageMB": "[parameters('skuSizeMB')]",
                        "storageAutoGrow": "[variables('storageAutoGrowString')]"
                    },
                    "createMode": "Default"
                    <#if usePrivateEndpoints>
                    ,"publicNetworkAccess": "Disabled"
                    </#if>
                },
                "location": "[parameters('location')]",
                "resources": []
            },
    <#if usePrivateEndpoints>
        {
            "type": "Microsoft.Network/privateEndpoints",
            "apiVersion": "2020-05-01",
            "name": "${privateEndpointName}",
            "location": "[parameters('location')]",
            "dependsOn": [
                "[resourceId('Microsoft.DBforPostgreSQL/servers', parameters('dbServerName'))]"
            ],
            "tags": "[parameters('serverTags')]",
            "properties": {
                "privateLinkServiceConnections": [
                    {
                        "name": "${privateEndpointName}",
                        "properties": {
                            "privateLinkServiceId": "[resourceId('Microsoft.DBforPostgreSQL/servers', parameters('dbServerName'))]",
                            "groupIds": [
                                "postgresqlServer"
                            ],
                            "privateLinkServiceConnectionState": {
                                "status": "Approved",
                                "description": "Auto-approved",
                                "actionsRequired": "None"
                            }
                        }
                    }
                ],
                "manualPrivateLinkServiceConnections": [],
                "subnet": {
                    "id": "${subnetIdForPrivateEndpoint}"
                },
                "customDnsConfigs": []
            }
        },
        {
            "type": "Microsoft.Network/privateEndpoints/privateDnsZoneGroups",
            "apiVersion": "2020-05-01",
            "name": "[concat('${privateEndpointName}', '/default')]",
            "dependsOn": [
                "[resourceId('Microsoft.Network/privateEndpoints', '${privateEndpointName}') ]"
            ],
            "properties": {
                "privateDnsZoneConfigs": [
                    {
                        "name": "dns-${privateEndpointName}",
                        "properties": {
                            <#if existingDatabasePrivateDnsZoneId??>
                            "privateDnsZoneId": "${existingDatabasePrivateDnsZoneId}"
                            <#else>
                            "privateDnsZoneId": "[resourceId('Microsoft.Network/privateDnsZones', 'privatelink.postgres.database.azure.com')]"
                            </#if>
                        }
                    }
                ]
            }
        }
    <#else>
        {
            "name": "[concat(parameters('dbServerName'), '/vnet-rule-for-postgres', copyIndex())]",
            "type": "Microsoft.DBforPostgreSQL/servers/virtualNetworkRules",
            "apiVersion": "[variables('apiVersion')]",
            "dependsOn": [
                "[concat('Microsoft.DBforPostgreSQL/servers/',parameters('dbServerName'))]"
            ],
            "properties": {
                "virtualNetworkSubnetId": "[variables('subnetList')[copyIndex()]]",
                "ignoreMissingVnetServiceEndpoint": false
            },
            "copy": {
                "name": "vnetcopy",
                "count": "[length(variables('subnetList'))]",
                "mode": "serial",
                "batchSize": "[parameters('batchSize')]"
            }
        }
    </#if>
    <#if dataEncryption == true> ,
        {
          "type": "Microsoft.Resources/deployments",
          "apiVersion": "2019-05-01",
          "name": "[concat(parameters('dbServerName'), '-', 'addAccessPolicy')]",
          "resourceGroup": "[parameters('keyVaultResourceGroupName')]",
          "dependsOn": [
            "[resourceId('Microsoft.DBforPostgreSQL/servers', parameters('dbServerName'))]"
          ],
          "properties": {
            "mode": "Incremental",
            "template": {
              "$schema": "http://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
              "contentVersion": "1.0.0.0",
              "resources": [
                {
                  "type": "Microsoft.KeyVault/vaults/accessPolicies",
                  "name": "[concat(parameters('keyVaultName'), '/add')]",
                  "apiVersion": "2018-02-14-preview",
                  "properties": {
                    "accessPolicies": [
                      {
                        "tenantId": "[subscription().tenantId]",
                        "objectId": "[reference(resourceId('Microsoft.DBforPostgreSQL/servers/', parameters('dbServerName')), '2017-12-01', 'Full').identity.principalId]",
                        "permissions": {
                          "keys": [
                            "get",
                            "wrapKey",
                            "unwrapKey"
                          ]
                        }
                      }
                    ]
                  }
                }
              ]
            }
          }
        },
        {
          "name": "[concat(parameters('dbServerName'), '/', variables('serverKeyName'))]",
          "type": "Microsoft.DBforPostgreSQL/servers/keys",
          "apiVersion": "2020-01-01-preview",
          "dependsOn": [
            "[concat(parameters('dbServerName'), '-', 'addAccessPolicy')]",
            "[resourceId('Microsoft.DBforPostgreSQL/servers', parameters('dbServerName'))]"
          ],
          "properties": {
            "serverKeyType": "AzureKeyVault",
            "uri": "[concat(reference(resourceId(parameters('keyVaultResourceGroupName'), 'Microsoft.KeyVault/vaults/', parameters('keyVaultName')), '2018-02-14-preview', 'Full').properties.vaultUri, 'keys/', parameters('keyName'), '/', parameters('keyVersion'))]"
          }
        }
    </#if>
    ],
    "outputs": {
     "databaseServerFQDN": {
         "type": "string",
         "value": "[reference(parameters('dbServerName')).fullyQualifiedDomainName]"
     }
    }
}