{
    "$schema": "https://schema.management.azure.com/schemas/2019-08-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters": {
        "storageAccountName": {
            "defaultValue": "${storageAccountName}",
            "type": "String"
        },
        "location": {
            "defaultValue": "${location}",
            "type": "String"
        },
        "sku_name": {
            "defaultValue": "${skuName}",
            "type": "String"
        },
        "encrypted": {
            "type":"bool",
            "defaultValue": ${(encrypted!true)?c}
        }
    },
    "variables": {},
    "resources": [
        {
            "type": "Microsoft.Storage/storageAccounts",
            "apiVersion": "2019-06-01",
            "name": "[parameters('storageAccountName')]",
            "location": "[parameters('location')]",
            "tags": {
                <#if userDefinedTags?? && userDefinedTags?has_content>
                    <#list userDefinedTags?keys as key>
                      "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                      </#list>
                </#if>
            },
            "sku": {
                "name": "[parameters('sku_name')]"
            },
            "kind": "StorageV2",
            "properties": {
                "networkAcls": {
                    "bypass": "AzureServices",
                    "virtualNetworkRules": [],
                    "ipRules": [],
                    "defaultAction": "Allow"
                },
                "allowBlobPublicAccess": false,
                "supportsHttpsTrafficOnly": true,
                "encryption": {
                    "services": {
                        "file": {
                            "keyType": "Account",
                            "enabled": "[parameters('encrypted')]"
                        },
                        "blob": {
                            "keyType": "Account",
                            "enabled": "[parameters('encrypted')]"
                        }
                    },
                    "keySource": "Microsoft.Storage"
                },
                "accessTier": "Hot"
            }
        },
        {
            "type": "Microsoft.Storage/storageAccounts/blobServices",
            "apiVersion": "2019-06-01",
            "name": "[concat(parameters('storageAccountName'), '/default')]",
            "dependsOn": [
                "[resourceId('Microsoft.Storage/storageAccounts', parameters('storageAccountName'))]"
            ],
            "sku": {
                "name": "Standard_LRS",
                "tier": "Standard"
            },
            "properties": {
                "cors": {
                    "corsRules": []
                },
                "deleteRetentionPolicy": {
                    "enabled": false
                }
            }
        },
        {
            "type": "Microsoft.Storage/storageAccounts/fileServices",
            "apiVersion": "2019-06-01",
            "name": "[concat(parameters('storageAccountName'), '/default')]",
            "dependsOn": [
                "[resourceId('Microsoft.Storage/storageAccounts', parameters('storageAccountName'))]"
            ],
            "sku": {
                "name": "Standard_LRS",
                "tier": "Standard"
            },
            "properties": {
                "cors": {
                    "corsRules": []
                }
            }
        }
    ]
}