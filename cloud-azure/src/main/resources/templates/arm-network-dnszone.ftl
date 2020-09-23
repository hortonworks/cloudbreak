<#setting number_format="computer">
{
    "$schema": "https://schema.management.azure.com/schemas/2019-08-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
        "virtualNetworkId": {
            "defaultValue": "${virtualNetworkId}",
            "type": "String"
        },
        "virtualNetworkName": {
            "defaultValue": "${virtualNetworkName}",
            "type": "String"
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
        }
    },
  	"variables" : {
  	},
    "resources": [
        <#list privateEndpointServices as privateEndpointService>
        <#if !deployOnlyNetworkLinks>
        {
            "type": "Microsoft.Network/privateDnsZones",
            "apiVersion": "2018-09-01",
            "name": "${privateEndpointService.dnsZoneName}",
            "location": "global",
            "tags": "[parameters('serverTags')]"
        },
        </#if>
        {
            "type": "Microsoft.Network/privateDnsZones/virtualNetworkLinks",
            "apiVersion": "2018-09-01",
            "name": "[concat('${privateEndpointService.dnsZoneName}', '/', parameters('virtualNetworkName'))]",
            "location": "global",
            "tags": "[parameters('serverTags')]",
            <#if !deployOnlyNetworkLinks>
            "dependsOn": [
                "[resourceId('Microsoft.Network/privateDnsZones', '${privateEndpointService.dnsZoneName}')]"
            ],
            </#if>
            "properties": {
                "registrationEnabled": false,
                "virtualNetwork": {
                    "id": "[parameters('virtualNetworkId')]"
                }
            }
        }<#if privateEndpointService_has_next>,</#if>
        </#list>
    ]
}