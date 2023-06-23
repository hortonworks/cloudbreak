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
        "Standard_D2ds_v4",
        "Standard_D4ds_v4",
        "Standard_E2ds_v4",
        "Standard_E4ds_v4"
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
      "defaultValue": ${(geoRedundantBackup!true)?c},
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
    "availabilityZone": {
      "type": "string",
      "defaultValue": "1"
    },
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
    "privateDnsZoneArmResourceId": {
      "type": "string",
      "defaultValue": ""
    }
  },
  "variables": {
    "geoRedundantBackupString":"[if(parameters('geoRedundantBackup'), 'Enabled', 'Disabled')]"
  },
  "resources": [
    {
      "type": "Microsoft.DBforPostgreSQL/flexibleServers",
      "apiVersion": "2022-12-01",
      "name": "[parameters('dbServerName')]",
      "location": "[parameters('location')]",
      "sku": {
        "name": "[parameters('skuName')]",
        "tier": "[parameters('skuTier')]"
      },
      "tags": "[parameters('serverTags')]",
      "properties": {
        "version": "[parameters('dbVersion')]",
        "administratorLogin": "[parameters('administratorLogin')]",
        "administratorLoginPassword": "[parameters('administratorLoginPassword')]",
        "network": {
          "delegatedSubnetResourceId": "[if(empty(parameters('virtualNetworkExternalId')), json('null'), json(format('{0}/subnets/{1}', parameters('virtualNetworkExternalId'), parameters('subnetName'))))]",
          "privateDnsZoneArmResourceId": "[if(empty(parameters('virtualNetworkExternalId')), json('null'), parameters('privateDnsZoneArmResourceId'))]"
        },
        "highAvailability": {
          "mode": "[parameters('haMode')]"
        },
        "storage": {
          "storageSizeGB": "[parameters('skuSizeGB')]"
        },
        "backup": {
          "backupRetentionDays": "[parameters('backupRetentionDays')]",
          "geoRedundantBackup": "[variables('geoRedundantBackupString')]"
        },
        "availabilityZone": "[parameters('availabilityZone')]"
      }
    },
    <#if !useSslEnforcement>
    {
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
    },
    </#if>
    {
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
  ],
  "outputs": {
    "databaseServerFQDN": {
      "type": "string",
      "value": "[reference(parameters('dbServerName')).fullyQualifiedDomainName]"
    }
  }
}
