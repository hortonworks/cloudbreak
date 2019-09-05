{
    "$schema": "http://schema.management.azure.com/schemas/2014-04-01-preview/deploymentTemplate.json#",
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
            "defaultValue" : "${skuTier}",
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
            "defaultValue" : "${skuFamily}",
            "metadata": {
                "description": "The family of hardware."
            }
        },
        "skuCapacity": {
            "type": "int",
            "defaultValue" : ${skuCapacity},
            "metadata": {
                "description": "The scale up/out capacity, representing server's compute units."
            }
        },
        "skuSizeMB": {
            "type": "int",
            "defaultValue" : ${skuSizeMB},
            "metadata": {
                "description": "The size code, to be interpreted by resource as appropriate."
            }
        },
        "skuName": {
            "type": "string",
            "defaultValue": "${skuName!GP_Gen5_2}",
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
                "description": "Server version."
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
        "adminPassword": {
            "type": "securestring",
            "defaultValue" : "${adminPassword}",
            "minLength": 8,
            "maxLength": 128,
            "metadata": {
                "description": "The administrator password for the database server."
            }
        },
        "sslEnforcement": {
            "type": "bool",
            "defaultValue": ${(sslEnforcement!true)?c},
            "metadata": {
                "description": "Enable ssl enforcement or not when connect to server."
            }
        },
        "backupRetentionDays": {
            "type": "int",
            "defaultValue": ${backupRetentionDays!7},
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
        "storageAutoGrow": {
            "type": "bool",
            "defaultValue": ${(storageAutoGrow!false)?c},
            "metadata": {
                "description": "Enable Storage Auto Grow."
            }
        },
        "location": {
            "type": "string",
            "defaultValue": "[resourceGroup().location]",
            "metadata": {
                "description": "The location in which the database should be deployed."
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
                      "${key}": "${serverTags[key]}",
                      </#list>
                </#if>
                    "cb-resource-type": "database"
            },
            "metadata": {
                "description": "User defined tags to be attached to the resource."
            }
        }
    },
    "variables": {
        "subnetList": "[split(parameters('subnets'),',')]",
        "apiVersionMSSql":"2015-05-01-preview",
        "apiVersion":"2017-12-01",
        "sslEnforcementString":"[if(parameters('sslEnforcement'), 'Enabled', 'Disabled')]",
        "geoRedundantBackupString":"[if(parameters('geoRedundantBackup'), 'Enabled', 'Disabled')]",
        "storageAutoGrowString":"[if(parameters('storageAutoGrow'), 'Enabled', 'Disabled')]"

    },
    "resources": [
        {
            "name": "[parameters('dbServerName')]",
            "type": "Microsoft.DBforPostgreSQL/servers",
            "apiVersion": "[variables('apiVersion')]",
            "sku": {
                "name": "[parameters('skuName')]",
                "tier": "[parameters('skuTier')]",
                "capacity": "[parameters('skuCapacity')]",
                "size": "[parameters('skuSizeMB')]",
                "family": "[parameters('skuFamily')]"
            },
            "tags": "[parameters('serverTags')]",
            "properties": {
                "version": "[parameters('dbVersion')]",
                "administratorLogin": "[parameters('adminLoginName')]",
                "administratorLoginPassword": "[parameters('adminPassword')]",
                "sslEnforcement": "[variables('sslEnforcementString')]",
                "storageProfile": {
                    "backupRetentionDays": "[parameters('backupRetentionDays')]",
                    "geoRedundantBackup": "[variables('geoRedundantBackupString')]",
                    "storageMB": "[parameters('skuSizeMB')]",
                    "storageAutoGrow": "[variables('storageAutoGrowString')]"
                },
                "createMode": "Default"
            },
            "location": "[parameters('location')]",
            "resources": []
        },
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
                "count": "[length(variables('subnetList'))]"
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
