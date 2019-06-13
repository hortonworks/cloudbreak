<#setting number_format="computer">
{
    "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
        <#list subnetPrefixList as subnetPrefix>
        "subnet${subnetPrefix?index}Prefix": {
           "type": "string",
           "defaultValue": "${subnetPrefix}"
        },
        </#list>
        "virtualNetworkNamePrefix" : {
            "type": "string",
            "defaultValue" : "${networkPrefix}"
        },
        "virtualNetworkName" : {
            "type": "string",
            "defaultValue" : "${virtualNetworkName}"
        },
        "region" : {
          "type" : "string",
          "defaultValue" : "${region}"
        },
        "resourceGroupName" : {
          "type" : "string",
          "defaultValue" : "${resourceGroupName}"
        }
    },
  	"variables" : {
      <#list subnetPrefixList as subnetPrefix>
      "subnetID${subnetPrefix?index}": "[concat(resourceId('Microsoft.Network/virtualNetworks', parameters('virtualNetworkName')), '/subnets/subnet${subnetPrefix?index}')]",
      </#list>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks', parameters('virtualNetworkName'))]"
  	},
    "resources": [
            {
                 "apiVersion": "2015-05-01-preview",
                 "type": "Microsoft.Network/virtualNetworks",
                 "tags": {
                    "cb-resource-type": "${network_resource}"
                },
                 "name": "[parameters('virtualNetworkName')]",
                 "location": "[parameters('region')]",
                 "properties": {
                     "addressSpace": {
                         "addressPrefixes": [
                             "[parameters('virtualNetworkNamePrefix')]"
                         ]
                     },
                     "subnets": [
                         <#list subnetPrefixList as subnetPrefix>
                         {
                             "name": "subnet${subnetPrefix?index}",
                             "properties": {
                                 "addressPrefix": "[parameters('subnet${subnetPrefix?index}Prefix')]"
                             }
                         }<#if subnetPrefix_has_next>,</#if>
                         </#list>
                     ]
                 }
            }
     ],
    "outputs": {
         <#list subnetPrefixList as subnetPrefix>
         "subnetId${subnetPrefix?index}": {
           "type": "string",
           "value": "[variables('subnetID${subnetPrefix?index}')]"
         },
         "subnetCidr${subnetPrefix?index}": {
           "type": "string",
           "value": "${subnetPrefix}"
         },
         </#list>
         "networkId": {
           "type": "string",
           "value": "[variables('vnetID')]"
         }
    }
}