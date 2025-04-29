<#setting number_format="computer">
<#macro zoneredundant>
<#if multiAz>,
"zones": [
   "1",
   "2",
   "3"
 ]
</#if>
</#macro>
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
        "publicIPNamePrefix" :{
            "type": "string",
            "defaultValue" : "${stackname}"
        },
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
      "vnetID": "[resourceId(parameters('resourceGroupName'),'Microsoft.Network/virtualNetworks',parameters('existingVNETName'))]",
      <#else>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks',parameters('virtualNetworkNamePrefix'))]",
      </#if>
      <#list igs as group>
        "${group.compressedName}secGroupName": "${group.compressedName}${stackname}sg"
      </#list>
    },
    "resources": [
    <#list groups?keys as instanceGroup>
    <#list groups[instanceGroup] as instance>
        {
           "apiVersion": "2023-06-01",
           "type": "Microsoft.Network/networkInterfaces",
           "name": "[concat(parameters('nicNamePrefix'), '${instance.instanceId}')]",
           "location": "[parameters('region')]",
           "tags": {
             <#if userDefinedTags?? && userDefinedTags?has_content>
                 <#list userDefinedTags?keys as key>
                    "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                    </#list>
             </#if>
            },
           "dependsOn": [
               <#if loadBalancerMapping[instance.groupName]?? && (loadBalancerMapping[instance.groupName]?size > 0)>
                   <#list loadBalancerMapping[instance.groupName] as loadBalancer>
                       "[resourceId('Microsoft.Network/loadBalancers', '${loadBalancer.name}')]"<#sep>,</#sep>
                   </#list>
               </#if>
           ],
           "properties": {
            <#if acceleratedNetworkEnabled[instance.flavor]>
                "enableAcceleratedNetworking": "true",
            </#if>
            <#if securityGroups[instance.groupName]?? && securityGroups[instance.groupName]?has_content>
               "networkSecurityGroup":{
                   "id": "${securityGroups[instance.groupName]}"
                },
            <#else>
               "networkSecurityGroup":{
                    "id": "[resourceId('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
               },
            </#if>
               "ipConfigurations": [
                   {
                       "name": "ipconfig1",
                       "properties": {
                           "privateIPAllocationMethod": "Dynamic",
                       <#if !noPublicIp>
                           "publicIPAddress": {
                               "id": "[resourceId('Microsoft.Network/publicIPAddresses',concat(parameters('publicIPNamePrefix'), '${instance.instanceId}'))]"
                           },
                        </#if>
                           "subnet": {
                             "id": "[concat(variables('vnetID'),'/subnets/', '${instance.subnetId}')]"
                           }
                        <#if loadBalancerMapping[instance.groupName]?? && (loadBalancerMapping[instance.groupName]?size > 0)>
                           ,"loadBalancerBackendAddressPools": [
                           <#list loadBalancerMapping[instance.groupName] as loadBalancer>
                               { "id": "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '${loadBalancer.name}', '${loadBalancer.name}-pool')]" }
                               <#sep>, </#sep>
                           </#list>
                           ]
                        </#if>
                       }
                   }
               ]
           }
         },
     </#list>
     <#if (instanceGroup_index + 1) != groups?size>,</#if>
     </#list>
     <#if groups?keys?has_content && loadBalancers?has_content>
     <#assign loadBalancer = loadBalancers[0]>
         {
           "apiVersion": "2023-06-01",
           "type": "Microsoft.Network/loadBalancers",
           "dependsOn": [],
           "location": "[parameters('region')]",
           "name": "${loadBalancer.name}",
           "tags": {
             <#if userDefinedTags?? && userDefinedTags?has_content>
                 <#list userDefinedTags?keys as key>
                     "${key}": "${userDefinedTags[key]}"<#if key_has_next>,</#if>
                 </#list>
             </#if>
           },
           "properties": {
             "backendAddressPools": [
                {
                  "name": "${loadBalancer.name}-pool",
                  "properties": {
                  }
                }
             ],
             "frontendIPConfigurations": [
               {
                 "name": "${loadBalancer.name}-frontend",
                 "properties": {
                   "privateIPAddressVersion": "IPv4",
                   "privateIPAllocationMethod": "Dynamic",
                   <#if existingVPC>
                   "subnet": {
                     "id": "[concat(variables('vnetID'),'/subnets/', '${groups[groups?keys[0]][0].subnetId}')]"
                   }
                   <#else>
                   "subnet": {
                     "id": "[concat(variables('vnetID'),'/subnets/',parameters('subnet1Name'))]"
                   }
                   </#if>
                 }<@zoneredundant/>
               }
             ],
             "inboundNatPools": [],
             "inboundNatRules": [],
             "loadBalancingRules": [
                 <#list loadBalancer.rules as rule>
                     {
                         "name": "${rule.name}",
                         "properties": {
                             "backendAddressPool": {
                                 "id": "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '${loadBalancer.name}', '${loadBalancer.name}-pool')]"
                             },
                             "backendPort": ${rule.backendPort},
                             "enableFloatingIP": <#if loadBalancer.type == "PRIVATE" && gatewayPrivateLbNeeded!false>true<#else>false</#if>,
                             "enableTcpReset": false,
                             "frontendIPConfiguration": {
                                 "id": "[concat(resourceId('Microsoft.Network/loadBalancers', '${loadBalancer.name}'), '/frontendIPConfigurations/${loadBalancer.name}-frontend')]"
                             },
                             "frontendPort": ${rule.frontendPort},
                             "idleTimeoutInMinutes": 4,
                             "loadDistribution": "Default",
                             "probe": {
                                 "id": "[resourceId('Microsoft.Network/loadBalancers/probes', '${loadBalancer.name}', '${rule.probe.name}')]"
                             },
                             "protocol": "${rule.protocol}"
                         }
                     }<#sep>,</#sep>
                 </#list>
             ],
             "outboundRules": [],
             "probes": [
               <#list loadBalancer.probes as probe>
               {
                 "name": "${probe.name}",
                 "properties": {
                   "intervalInSeconds": "${probe.interval}",
                   "probeThreshold": "${probe.threshold}",
                   "port": ${probe.port},
                   "requestPath": "${probe.path}",
                   "protocol": "${probe.protocol}"
                 }
               }<#if (probe_index + 1) != loadBalancer.probes?size>,</#if>
               </#list>
             ]
           },
           "sku": {
               "name": "Standard"
           }
         }
     </#if>
    ]
}