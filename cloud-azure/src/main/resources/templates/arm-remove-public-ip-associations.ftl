<#setting number_format="computer">
{
    "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
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
        "nicNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "region" : {
          "type" : "string",
          "defaultValue" : "${region}"
        },
        "subnet1Name": {
            "type": "string",
            "defaultValue": "${stackname}subnet"
        }
    },
  	"variables" : {
      <#if existingVPC>
      "vnetID": "[resourceId(parameters('resourceGroupName'),'Microsoft.Network/virtualNetworks',parameters('existingVNETName'))]"
      <#else>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks',parameters('virtualNetworkNamePrefix'))]"
      </#if>
  	},
    "resources": [
             <#list groups?keys as instanceGroup>
             <#list groups[instanceGroup] as instance>
                 {
                   "apiVersion": "2023-06-01",
                   "type": "Microsoft.Network/networkInterfaces",
                   "name": "[concat(parameters('nicNamePrefix'), '${instance.instanceId}')]",
                   "location": "[parameters('region')]",
                   "properties": {
                       "ipConfigurations": [
                           {
                               "name": "ipconfig1",
                               "properties": {
                                   "privateIPAllocationMethod": "Dynamic",
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
                 }<#if (instance_index + 1) != groups[instanceGroup]?size>,</#if>
             </#list>
             <#if (instanceGroup_index + 1) != groups?size>,</#if>
             </#list>
     	]
}
