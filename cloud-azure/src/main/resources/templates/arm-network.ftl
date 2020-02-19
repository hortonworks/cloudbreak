<#setting number_format="computer">
{
    "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
        <#list subnetDetails as subnet>
        "subnet${subnet.index}Prefix": {
           "type": "string",
           <#if subnet.publicSubnetCidr?has_content>
           "defaultValue": "${subnet.publicSubnetCidr}"
           </#if>
           <#if subnet.privateSubnetCidr?has_content>
           "defaultValue": "${subnet.privateSubnetCidr}"
           </#if>

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
      <#list subnetDetails as subnet>
      "subnetID${subnet.index}": "[concat(resourceId('Microsoft.Network/virtualNetworks', parameters('virtualNetworkName')), '/subnets/subnet${subnet.index}')]",
      </#list>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks', parameters('virtualNetworkName'))]"
  	},
    "resources": [
            {
                 "apiVersion": "2019-04-01",
                 "type": "Microsoft.Network/virtualNetworks",
                 "name": "[parameters('virtualNetworkName')]",
                 "location": "[parameters('region')]",
                 "properties": {
                     "addressSpace": {
                         "addressPrefixes": [
                             "[parameters('virtualNetworkNamePrefix')]"
                         ]
                     },
                     "subnets": [
                         <#list subnetDetails as subnet>
                         {
                             "name": "subnet${subnet.index}",
                             "properties": {
                                 "addressPrefix": "[parameters('subnet${subnet.index}Prefix')]",
                                 "serviceEndpoints": [
                                     {
                                         "service": "Microsoft.Sql"
                                     },
                                    {
                                        "service": "Microsoft.Storage"
                                    }
                                 ]
                             }
                         }<#if subnet_has_next>,</#if>
                         </#list>
                     ]
                 }
            }
     ],
    "outputs": {
         <#list subnetDetails as subnet>
         "subnetId${subnet.index}": {
           "type": "string",
           "value": "[variables('subnetID${subnet.index}')]"
         },
         </#list>
         "networkId": {
           "type": "string",
           "value": "[variables('vnetID')]"
         }
    }
}