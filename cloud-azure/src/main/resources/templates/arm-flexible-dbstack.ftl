{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "dbServerName": {
      "type": "string",
      "defaultValue" : "${dbServerName}",
      "metadata": {
        "description": "Name of the database server resource."
      }
    },
    "dbVersion": {
      "type": "string",
      "defaultValue": "${dbVersion}",
      "metadata": {
        "description": "PostgreSQL version."
      }
    },
    "skuName": {
      "type": "string",
      "defaultValue": "${skuName!"Standard_E4ds_v4"}",
      "allowedValues": [
        "Standard_D2s_v3",
        "Standard_D4s_v3",
        "Standard_D2ds_v4",
        "Standard_D4ds_v4",
        "Standard_D2ds_v5",
        "Standard_D4ds_v5",
        "Standard_E2s_v3",
        "Standard_E4s_v3",
        "Standard_E2ds_v4",
        "Standard_E4ds_v4",
        "Standard_E2ds_v5",
        "Standard_E4ds_v5"
      ],
      "metadata": {
        "description": "Azure database for PostgreSQL SKU name."
      }
    },
    "skuTier": {
      "type": "string",
      "defaultValue" : "${skuTier!"MemoryOptimized"}",
      "allowedValues": [
        "Burstable",
        "GeneralPurpose",
        "MemoryOptimized"
      ],
      "metadata": {
        "description": "The tier of the particular SKU, e.g. Basic. - Basic, GeneralPurpose, MemoryOptimized"
      }
    },
    "administratorLogin": {
      "type": "string",
      "defaultValue" : "${adminLoginName}",
      "minLength": 1,
      "metadata": {
        "description": "The administrator login name for the database server."
      }
    },
    "administratorLoginPassword": {
      "type": "securestring",
      "defaultValue" : "${adminPassword}",
      "minLength": 8,
      "maxLength": 128,
      "metadata": {
        "description": "The administrator password for the database server."
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
      "defaultValue": ${(geoRedundantBackup!false)?c},
      "metadata": {
        "description": "Enable Geo-redundant or not for server backup."
      }
    },
    "location": {
      "type": "string",
      "defaultValue": "${location}",
      "metadata": {
        "description": "The location in which the database should be deployed."
      }
    },
    "skuSizeGB": {
      "type": "int",
      "defaultValue": ${(skuSizeGB!128)?c}
    },
    "haMode": {
      "type": "string",
      "defaultValue": "${highAvailability!"Disabled"}",
      "allowedValues": [
        "Disabled",
        "SameZone",
        "ZoneRedundant"
      ],
      "metadata": {
        "description": "The high availability mode of the database."
      }
    },
    <#if useAvailabilityZone>
    "availabilityZone": {
      "type": "string",
      "defaultValue": "${availabilityZone}"
    },
    </#if>
    <#if useStandbyAvailabilityZone>
    "standbyAvailabilityZone": {
        "type": "string",
        "defaultValue": "${standbyAvailabilityZone}"
    },
    </#if>
    "version": {
      "type": "string",
      "defaultValue": "${dbVersion}"
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
    "virtualNetworkExternalId": {
      "type": "string",
      "defaultValue": ""
    },
    "subnetName": {
      "type": "string",
      "defaultValue": ""
    },
    "privateEndpointName": {
      "type": "String",
      "defaultValue": "${privateEndpointName}"
    },
    "existingDatabasePrivateDnsZoneId": {
      "type": "string",
      "defaultValue": "${existingDatabasePrivateDnsZoneId!""}"
    },
    "flexibleServerDelegatedSubnetId": {
      "type": "string",
      "defaultValue": "${flexibleServerDelegatedSubnetId!""}"
    }<#if dataEncryption == true>,
    "encryptionKeyName": {
      "type": "string",
      "defaultValue" : "${encryptionKeyName}"
    },
    "encryptionUserManagedIdentity": {
      "type": "string",
      "defaultValue" : "${encryptionUserManagedIdentity}"
    }</#if>
  },
  "variables": {
    "geoRedundantBackupString":"[if(parameters('geoRedundantBackup'), 'Enabled', 'Disabled')]"
  },
  "resources": [
    {
      "type": "Microsoft.DBforPostgreSQL/flexibleServers",
      "apiVersion": "2023-06-01-preview",
      "name": "[parameters('dbServerName')]",
      "location": "[parameters('location')]",
      "sku": {
        "name": "[parameters('skuName')]",
        "tier": "[parameters('skuTier')]"
      },
      <#if dataEncryption == true>
      "identity": {
        "type": "UserAssigned",
        "userAssignedIdentities": {
            "[parameters('encryptionUserManagedIdentity')]": {}
        }
      },</#if>
      "tags": "[parameters('serverTags')]",
      "properties": {
        "version": "[parameters('dbVersion')]",
        "administratorLogin": "[parameters('administratorLogin')]",
        "administratorLoginPassword": "[parameters('administratorLoginPassword')]",
        <#if dataEncryption == true>
        "dataEncryption": {
            "primaryEncryptionKeyStatus": "Valid",
            "type": "AzureKeyVault",
            "primaryKeyURI": "[parameters('encryptionKeyName')]",
            "primaryUserAssignedIdentityId": "[parameters('encryptionUserManagedIdentity')]"
        },</#if>
        "network": {
          <#if usePrivateEndpoints>
          "publicNetworkAccess": "Disabled"
          </#if>
          <#if useDelegatedSubnet>
          "publicNetworkAccess": "Disabled",
          "delegatedSubnetResourceId": "[if(empty(parameters('flexibleServerDelegatedSubnetId')), json('null'), parameters('flexibleServerDelegatedSubnetId'))]",
          "privateDnsZoneArmResourceId": "[if(empty(parameters('existingDatabasePrivateDnsZoneId')), json('null'), parameters('existingDatabasePrivateDnsZoneId'))]"
          </#if>
        },
        "highAvailability": {
          "mode": "[parameters('haMode')]"
          <#if useStandbyAvailabilityZone>,
          "standbyAvailabilityZone": "[parameters('standbyAvailabilityZone')]"
          </#if>
        },
        "storage": {
          "storageSizeGB": "[parameters('skuSizeGB')]"
        },
        <#if useAvailabilityZone>
        "availabilityZone": "[parameters('availabilityZone')]",
         </#if>
        "backup": {
          "backupRetentionDays": "[parameters('backupRetentionDays')]",
          "geoRedundantBackup": "[variables('geoRedundantBackupString')]"
        }
      }
    }
    <#if !useSslEnforcement>
    ,{
      "type": "Microsoft.DBforPostgreSQL/flexibleServers/configurations",
      "apiVersion": "2022-12-01",
      "name": "[concat(parameters('dbServerName'), '/require_secure_transport')]",
      "dependsOn": [
        "[resourceId('Microsoft.DBforPostgreSQL/flexibleServers', parameters('dbServerName'))]"
      ],
      "properties": {
        "value": "off",
        "source": "user-override"
      }
    }
    </#if>
    <#if usePrivateEndpoints>
    ,{
        "type": "Microsoft.Network/privateEndpoints",
        "apiVersion": "2023-05-01",
        "name": "${privateEndpointName}",
        "location": "[parameters('location')]",
        "dependsOn": [
            "[resourceId('Microsoft.DBforPostgreSQL/flexibleServers', parameters('dbServerName'))]"
        ],
        "tags": "[parameters('serverTags')]",
        "properties": {
            "privateLinkServiceConnections": [
                {
                    "name": "${privateEndpointName}",
                    "properties": {
                        "privateLinkServiceId": "[resourceId('Microsoft.DBforPostgreSQL/flexibleServers', parameters('dbServerName'))]",
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
        "apiVersion": "2023-05-01",
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
    <#elseif !useDelegatedSubnet && !usePrivateEndpoints>
    ,{
      "name": "[concat(parameters('dbServerName'), '/publicaccess')]",
      "type": "Microsoft.DBforPostgreSQL/flexibleServers/firewallRules",
      "apiVersion": "2022-12-01",
      "dependsOn": [
        "[resourceId('Microsoft.DBforPostgreSQL/flexibleServers', parameters('dbServerName'))]"
      ],
      "properties": {
        "startIpAddress": "0.0.0.0",
        "endIpAddress": "255.255.255.255"
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