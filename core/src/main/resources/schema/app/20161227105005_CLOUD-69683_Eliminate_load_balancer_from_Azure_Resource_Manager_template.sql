-- // CLOUD-69683 Eliminate load balancer from Azure Resource Manager template
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN stackTemplate TEXT;

UPDATE stack SET stackTemplate = '<#setting number_format="computer">
{
    "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "parameters" : {
        "userImageStorageAccountName": {
            "type": "string",
            "defaultValue" : "${storage_account_name}"
        },
        "userImageStorageContainerName" : {
            "type" : "string",
            "defaultValue" : "${image_storage_container_name}"
        },
        "userDataStorageContainerName" : {
            "type" : "string",
            "defaultValue" : "${storage_container_name}"
        },
        "userImageVhdName" : {
            "type" : "string",
            "defaultValue" : "${storage_vhd_name}"
        },
        "adminUsername" : {
          "type" : "string",
          "defaultValue" : "${credential.loginUserName}"
        },
        <#if existingVPC>
        "resourceGroupName" : {
          "type": "string",
          "defaultValue" : "${resourceGroupName}"
        },
        "existingVNETName" : {
          "type": "string",
          "defaultValue" : "${existingVNETName}"
        },
        "existingSubnetName" : {
          "type": "string",
          "defaultValue" : "${existingSubnetName}"
        },
        <#else>
        "virtualNetworkNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        </#if>
        "vmNamePrefix" :{
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "publicIPNamePrefix" :{
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "nicNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "sshKeyData" : {
            "type" : "string",
            "defaultValue" : "${credential.publicKey}"
        },
        "loadBalancerName" : {
            "type" : "string",
            "defaultValue" : "${stackname}lb"
        },
        "region" : {
          "type" : "string",
          "defaultValue" : "${region}"
        },
        "subnet1Name": {
            "type": "string",
            "defaultValue": "${stackname}subnet"
        },
        <#if !existingVPC>
        "subnet1Prefix": {
           "type": "string",
           "defaultValue": "${subnet1Prefix}"
        },
        </#if>
        <#list groups?keys as instanceGroup>
        <#list groups[instanceGroup] as instance>
        <#if instanceGroup == "GATEWAY">
        "gatewaystaticipname": {
            "type": "string",
            "defaultValue": "${stackname}${instance.instanceId}"
        },
        </#if>
        </#list>
        </#list>
        "sshIPConfigName": {
            "type": "string",
            "defaultValue": "${stackname}ipcn"
        }
    },
  	"variables" : {
      "userImageName" : "[concat(''https://'',parameters(''userImageStorageAccountName''),''.blob.core.windows.net/'',parameters(''userImageStorageContainerName''),''/'',parameters(''userImageVhdName''))]",
      "osDiskVhdName" : "[concat(''https://'',parameters(''userImageStorageAccountName''),''.blob.core.windows.net/'',parameters(''userDataStorageContainerName''),''/'',parameters(''vmNamePrefix''),''osDisk'')]",
      <#if existingVPC>
      "vnetID": "[resourceId(parameters(''resourceGroupName''),''Microsoft.Network/virtualNetworks'',parameters(''existingVNETName''))]",
      "subnet1Ref": "[concat(variables(''vnetID''),''/subnets/'',parameters(''existingSubnetName''))]",
      <#else>
      "vnetID": "[resourceId(''Microsoft.Network/virtualNetworks'',parameters(''virtualNetworkNamePrefix''))]",
      "subnet1Ref": "[concat(variables(''vnetID''),''/subnets/'',parameters(''subnet1Name''))]",
      </#if>
      <#if !noPublicIp>
      "staticIpRef": "[resourceId(''Microsoft.Network/publicIPAddresses'', parameters(''gatewaystaticipname''))]",
      "ilbBackendAddressPoolName": "${stackname}bapn",
      "lbID": "[resourceId(''Microsoft.Network/loadBalancers'', parameters(''loadBalancerName''))]",
      "sshIPConfig": "[concat(variables(''lbID''),''/frontendIPConfigurations/'', parameters(''sshIPConfigName''))]",
      "ilbBackendAddressPoolID": "[concat(variables(''lbID''),''/backendAddressPools/'', variables(''ilbBackendAddressPoolName''))]",
      </#if>
      <#list igs as group>
      "${group?replace(''_'', '''')}secGroupName": "${group?replace(''_'', '''')}${stackname}sg",
      </#list>
      "sshKeyPath" : "[concat(''/home/'',parameters(''adminUsername''),''/.ssh/authorized_keys'')]"
  	},
    "resources": [
            <#if !existingVPC>
            {
                 "apiVersion": "2015-05-01-preview",
                 "type": "Microsoft.Network/virtualNetworks",
                 <#if !noFirewallRules>
                 "dependsOn": [
                    <#list igs as group>
                        "[concat(''Microsoft.Network/networkSecurityGroups/'', variables(''${group?replace(''_'', '''')}secGroupName''))]"<#if (group_index + 1) != igs?size>,</#if>
                    </#list>
                 ],
                 </#if>
                 "name": "[parameters(''virtualNetworkNamePrefix'')]",
                 "location": "[parameters(''region'')]",
                 "properties": {
                     "addressSpace": {
                         "addressPrefixes": [
                             "[parameters(''subnet1Prefix'')]"
                         ]
                     },
                     "subnets": [
                         {
                             "name": "[parameters(''subnet1Name'')]",
                             "properties": {
                                 "addressPrefix": "[parameters(''subnet1Prefix'')]"
                             }
                         }
                     ]
                 }
             },
             </#if>
             <#if !noFirewallRules>
             <#list igs as group>
             {
               "apiVersion": "2015-05-01-preview",
               "type": "Microsoft.Network/networkSecurityGroups",
               "name": "[variables(''${group?replace(''_'', '''')}secGroupName'')]",
               "location": "[parameters(''region'')]",
               "properties": {
               "securityRules": [
                   {
                       "name": "endpoint1outr",
                       "properties": {
                           "protocol": "*",
                           "sourcePortRange": "*",
                           "destinationPortRange": "*",
                           "sourceAddressPrefix": "*",
                           "destinationAddressPrefix": "*",
                           "access": "Allow",
                           "priority": 101,
                           "direction": "Outbound"
                       }
                   },
                   <#list securities[group] as port>
                   {
                       "name": "endpoint${port_index}inr",
                       "properties": {
                           "protocol": "${port.capitalProtocol}",
                           "sourcePortRange": "*",
                           "destinationPortRange": "${port.port}",
                           "sourceAddressPrefix": "${port.cidr}",
                           "destinationAddressPrefix": "*",
                           "access": "Allow",
                           "priority": ${port_index + 102},
                           "direction": "Inbound"
                       }
                   }<#if (port_index + 1) != securities[group]?size>,</#if>
                   </#list>
                   ]

               }
             },
             </#list>
             </#if>
             <#list groups?keys as instanceGroup>
             <#list groups[instanceGroup] as instance>
                 <#if instanceGroup == "GATEWAY" && !noPublicIp>
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/publicIPAddresses",
                   "name": "[concat(parameters(''publicIPNamePrefix''), ''${instance.instanceId}'')]",
                   "location": "[parameters(''region'')]",
                   "properties": {
                       "publicIPAllocationMethod": "Static"
                   }
                 },
                 {
                    "apiVersion": "2015-05-01-preview",
                    "type": "Microsoft.Network/loadBalancers",
                    "name": "[parameters(''loadBalancerName'')]",
                    "location": "[parameters(''region'')]",
                    "dependsOn": [
                        "[concat(''Microsoft.Network/publicIPAddresses/'', parameters(''publicIPNamePrefix''), ''${instance.instanceId}'')]"
                    ],
                    "properties": {
                        "frontendIPConfigurations": [
                        {
                            "name": "[parameters(''sshIPConfigName'')]",
                            "properties": {
                                "publicIPAddress": {
                                    "id": "[variables(''staticIpRef'')]"
                                }
                            }
                        }
                        ],
                        "backendAddressPools": [
                        {
                            "name": "[variables(''ilbBackendAddressPoolName'')]"
                        }
                        ]
                        <#if !noFirewallRules>
                        ,"inboundNatRules": [
                        <#list securities[instance.groupName] as port>
                        {
                            "name": "endpoint${port_index}inr",
                            "properties": {
                                "frontendIPConfiguration": {
                                    "id": "[variables(''sshIPConfig'')]"
                                },
                                "protocol": "${port.protocol}",
                                "frontendPort": "${port.port}",
                                "backendPort": "${port.port}",
                                "enableFloatingIP": false
                            }
                        }<#if (port_index + 1) != securities[instance.groupName]?size>,</#if>
                        </#list>
                        ]
                        </#if>
                    }
                 },
                 </#if>
                 <#if instanceGroup == "CORE" && !noPublicIp>
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/publicIPAddresses",
                   "name": "[concat(parameters(''publicIPNamePrefix''), ''${instance.instanceId}'')]",
                   "location": "[parameters(''region'')]",
                   "properties": {
                       "publicIPAllocationMethod": "Static"
                   }
                 },
                 </#if>
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/networkInterfaces",
                   "name": "[concat(parameters(''nicNamePrefix''), ''${instance.instanceId}'')]",
                   "location": "[parameters(''region'')]",
                   "dependsOn": [
                       <#if !noFirewallRules>
                       "[concat(''Microsoft.Network/networkSecurityGroups/'', variables(''${instance.groupName?replace(''_'', '''')}secGroupName''))]"
                       </#if>
                       <#if !noPublicIp>
                       <#if !noFirewallRules>,</#if>
                       <#if instanceGroup == "CORE">
                       "[concat(''Microsoft.Network/publicIPAddresses/'', parameters(''publicIPNamePrefix''), ''${instance.instanceId}'')]"
                       </#if>
                       <#if instanceGroup == "GATEWAY">
                       "[concat(''Microsoft.Network/loadBalancers/'', parameters(''loadBalancerName''))]"
                       </#if>
                       </#if>
                       <#if !existingVPC>
                       <#if !noFirewallRules || !noPublicIp>,</#if>
                       "[concat(''Microsoft.Network/virtualNetworks/'', parameters(''virtualNetworkNamePrefix''))]"
                       </#if>
                   ],
                   "properties": {
                       <#if !noFirewallRules>
                       "networkSecurityGroup":{
                            "id": "[resourceId(''Microsoft.Network/networkSecurityGroups/'', variables(''${instance.groupName?replace(''_'', '''')}secGroupName''))]"
                       },
                       </#if>
                       "ipConfigurations": [
                           {
                               "name": "ipconfig1",
                               "properties": {
                                   "privateIPAllocationMethod": "Dynamic",
                                   <#if instanceGroup == "CORE" && !noPublicIp>
                                   "publicIPAddress": {
                                       "id": "[resourceId(''Microsoft.Network/publicIPAddresses'',concat(parameters(''publicIPNamePrefix''), ''${instance.instanceId}''))]"
                                   },
                                   </#if>
                                   "subnet": {
                                       "id": "[variables(''subnet1Ref'')]"
                                   }
                                   <#if instanceGroup == "GATEWAY" && !noPublicIp>
                                   ,"loadBalancerBackendAddressPools": [
                                       {
                                           "id": "[variables(''ilbBackendAddressPoolID'')]"
                                       }
                                   ]
                                   <#if !noFirewallRules>
                                   ,"loadBalancerInboundNatRules": [
                                   <#list securities[instance.groupName] as port>
                                       {
                                           "id": "[concat(variables(''lbID''),''/inboundNatRules/'', ''endpoint${port_index}inr'')]"
                                       }<#if (port_index + 1) != securities[instance.groupName]?size>,</#if>
                                   </#list>
                                   ]
                                   </#if>
                                   </#if>
                               }
                           }
                       ]
                   }
                 },
                 {
                   "apiVersion": "2015-06-15",
                   "type": "Microsoft.Compute/virtualMachines",
                   "name": "[concat(parameters(''vmNamePrefix''), ''${instance.instanceId}'')]",
                   "location": "[parameters(''region'')]",
                   "dependsOn": [
                       "[concat(''Microsoft.Network/networkInterfaces/'', parameters(''nicNamePrefix''), ''${instance.instanceId}'')]"
                   ],
                   "properties": {
                       "hardwareProfile": {
                           "vmSize": "${instance.flavor}"
                       },
                       "osProfile": {
                           "computername": "${instance.hostName}",
                           "adminUsername": "[parameters(''adminUsername'')]",
                           <#if disablePasswordAuthentication == false>
                           "adminPassword": "${credential.password}",
                           </#if>
                           "linuxConfiguration": {
                               "disablePasswordAuthentication": "${disablePasswordAuthentication?c}",
                               "ssh": {
                                   "publicKeys": [
                                    <#if disablePasswordAuthentication == true>
                                       {
                                           "path": "[variables(''sshKeyPath'')]",
                                           "keyData": "[parameters(''sshKeyData'')]"
                                       }
                                    </#if>
                                   ]
                               }
                           },
                           <#if instanceGroup == "CORE">
                           "customData": "${corecustomData}"
                           </#if>
                           <#if instanceGroup == "GATEWAY">
                           "customData": "${gatewaycustomData}"
                           </#if>
                       },
                       "storageProfile": {
                           "osDisk" : {
                               "name" : "[concat(parameters(''vmNamePrefix''),''-osDisk'', ''${instance.instanceId}'')]",
                               "osType" : "linux",
                               "image" : {
                                   "uri" : "[variables(''userImageName'')]"
                               },
                               "vhd" : {
                                   "uri" : "[concat(variables(''osDiskVhdName''), ''${instance.instanceId}'',''.vhd'')]"
                               },
                               "createOption": "FromImage"
                           },
                           "dataDisks": [
                           <#list instance.volumes as volume>
                               {
                                   "name": "[concat(''datadisk'', ''${instance.instanceId}'', ''${volume_index}'')]",
                                   "diskSizeGB": ${volume.size},
                                   "lun":  ${volume_index},
                                   "vhd": {
                                       "Uri": "[concat(''${instance.attachedDiskStorageUrl}'',parameters(''userDataStorageContainerName''),''/'',parameters(''vmNamePrefix''),''datadisk'',''${instance.instanceId}'', ''${volume_index}'', ''.vhd'')]"
                                   },
                                   "caching": "None",
                                   "createOption": "Empty"
                               } <#if (volume_index + 1) != instance.volumes?size>,</#if>
                           </#list>
                           ]
                       },
                       "networkProfile": {
                           "networkInterfaces": [
                               {
                                   "id": "[resourceId(''Microsoft.Network/networkInterfaces'',concat(parameters(''nicNamePrefix''), ''${instance.instanceId}''))]"
                               }
                           ]
                       }
                       <#if instance.bootDiagnosticsEnabled>
                       ,"diagnosticsProfile": {
                         "bootDiagnostics": {
                           "enabled": true,
                           "storageUri": "${instance.attachedDiskStorageUrl}"
                         }
                       }
                       </#if>
                   }
                 }<#if (instance_index + 1) != groups[instanceGroup]?size>,</#if>
             </#list>
             <#if (instanceGroup_index + 1) != groups?size>,</#if>
             </#list>

     	]
}' WHERE stack.cloudplatform = 'AZURE_RM';

UPDATE stack SET stackTemplate = '<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "Deploys a Hortonworks Data Platform cluster on AWS.",

  "Parameters" : {

    "StackName" : {
      "Description" : "Name of the CloudFormation stack that is used to tag instances",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },

    <#if enableInstanceProfile && existingRole>
     "InstanceProfile" : {
          "Description" : "InstanceProfile name or ARN that is assigned to every virtual machine",
          "Type" : "String"
     },
    </#if>

    <#if existingVPC>
    "VPCId" : {
      "Description" : "Id of the VPC where to deploy the cluster",
      "Type" : "String",
      "MinLength": "12",
      "MaxLength": "12",
      "AllowedPattern" : "vpc-[a-z0-9]{8}"
    },

    <#if !existingSubnet>
    "SubnetCIDR" : {
      "Description" : "IP address range in the securityRule specified as CIDR notation",
      "Type" : "String",
      "MinLength": "9",
      "MaxLength": "18",
      "AllowedPattern" : "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})"
    },

    <#else>
    "SubnetId" : {
      "Description" : "Id of the Subnet within the existing VPC where to deploy the cluster",
      "Type" : "String",
      "MinLength": "15",
      "MaxLength": "255",
      "AllowedPattern" : "subnet-[a-z0-9]{8}(?:,subnet-[a-z0-9]{8})*"
    },
    </#if>

    <#if existingIGW>
    "InternetGatewayId" : {
       "Description" : "Id of the internet gateway used by the VPC",
       "Type" : "String",
       "MinLength": "12",
       "MaxLength": "12",
       "AllowedPattern" : "igw-[a-z0-9]{8}"
    },
    </#if>

    </#if>

    <#if availabilitySetNeeded>
    "AvailabilitySet" : {
      "Description" : "Availability set name",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },
    </#if>

    "StackOwner" : {
      "Description" : "The instances will have this parameter as an Owner tag.",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },

    "CBUserData" : {
      "Description" : "User data to be executed",
      "Type" : "String",
      "MinLength": "9",
      "MaxLength": "50000"
    },

    "CBGateWayUserData" : {
      "Description" : "Gateway user data to be executed",
      "Type" : "String",
      "MinLength": "9",
      "MaxLength": "50000"
    },

    "KeyName": {
      "Description" : "Name of an existing EC2 KeyPair to enable SSH access to the instances",
      "Type": "String",
      "MinLength": "1",
      "MaxLength": "255",
      "AllowedPattern" : "[\\x20-\\x7E]*",
      "ConstraintDescription" : "can contain only ASCII characters."
    },

    "AMI" : {
      "Description" : "AMI that''s used to start instances",
      "Type" : "String",
      "MinLength": "12",
      "MaxLength": "12",
      "AllowedPattern" : "ami-[a-z0-9]{8}",
      "ConstraintDescription" : "must follow pattern: ami-xxxxxxxx"
    },

    "RootDeviceName" : {
      "Description" : "Name of the root device that comes with the AMI",
      "Type" : "String",
      "MinLength": "8",
      "MaxLength": "12"
    }

  },

  <#if !existingVPC>
  "Mappings" : {
    "SubnetConfig" : {
      "VPC"     : { "CIDR" : "${cbSubnet?first}" },
      "Public"  : { "CIDR" : "${cbSubnet?first}" }
    }
  },
  </#if>

  "Resources" : {

  <#if enableInstanceProfile && !existingRole>
        "S3AccessRole" : {
            "Type"  : "AWS::IAM::Role",
            "Properties" : {
                "AssumeRolePolicyDocument" : {
                    "Statement" : [ {
                        "Effect" : "Allow",
                        "Principal" : {
                            "Service" : [ "ec2.amazonaws.com" ]
                        },
                        "Action" : [ "sts:AssumeRole" ]
                    } ]
                },
                "Path" : "/"
            }
        },

        "S3RolePolicies" : {
            "Type" : "AWS::IAM::Policy",
            "Properties" : {
                "PolicyName" : "s3access",
                "PolicyDocument" : {
                    "Statement" : [ {
                        "Effect" : "Allow",
                        "Action" : "s3:*",
                        "Resource" : "*"
                    }]
                },
                "Roles" : [ { "Ref" : "S3AccessRole" } ]
            }
        },

        "S3InstanceProfile" : {
            "Type" : "AWS::IAM::InstanceProfile",
            "Properties" : {
                "Path" : "/",
                "Roles" : [ { "Ref" : "S3AccessRole" } ]
            }
        },
    </#if>

    <#if mapPublicIpOnLaunch>
    "EIP" : {
       "Type" : "AWS::EC2::EIP",
       "Properties" : {
          "Domain" : "vpc"
       }
    },
    </#if>

    <#if !existingVPC>
    "VPC" : {
      "Type" : "AWS::EC2::VPC",
      "Properties" : {
        "CidrBlock" : { "Fn::FindInMap" : [ "SubnetConfig", "VPC", "CIDR" ]},
        "EnableDnsSupport" : "true",
        "EnableDnsHostnames" : "true",
        <#if dedicatedInstances>
        "InstanceTenancy": "dedicated",
        </#if>
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },
    </#if>

    <#if !existingSubnet>
    "PublicSubnet" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
        "MapPublicIpOnLaunch" : true,
        <#if existingVPC>
        "VpcId" : { "Ref" : "VPCId" },
        "CidrBlock" : { "Ref" : "SubnetCIDR" },
        <#else>
        <#if availabilitySetNeeded>
        "AvailabilityZone" : {"Fn::Join" : ["",[ { "Ref" : "AvailabilitySet" } ] ]} ,
        </#if>
        "VpcId" : { "Ref" : "VPC" },
        "CidrBlock" : { "Fn::FindInMap" : [ "SubnetConfig", "Public", "CIDR" ]},
        </#if>
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },
    </#if>

    <#if !existingVPC>
    "InternetGateway" : {
      "Type" : "AWS::EC2::InternetGateway",
      "Properties" : {
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    <#if !existingIGW>
    "AttachGateway" : {
       "Type" : "AWS::EC2::VPCGatewayAttachment",
       "Properties" : {
         "VpcId" : { "Ref" : "VPC" },
         "InternetGatewayId" : { "Ref" : "InternetGateway" }
       }
    },
    </#if>

    </#if>

	<#if !existingSubnet>
    "PublicRouteTable" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        <#if existingVPC>
        "VpcId" : { "Ref" : "VPCId" },
        <#else>
        "VpcId" : { "Ref" : "VPC" },
        </#if>
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "PublicRoute" : {
          "Type" : "AWS::EC2::Route",
          <#if existingVPC>
          "DependsOn" : "PublicRouteTable",
          <#else>
          "DependsOn" : [ "PublicRouteTable", "AttachGateway" ],
          </#if>
          "Properties" : {
            "RouteTableId" : { "Ref" : "PublicRouteTable" },
            "DestinationCidrBlock" : "0.0.0.0/0",
            <#if existingVPC>
            "GatewayId" : { "Ref" : "InternetGatewayId" }
            <#else>
            "GatewayId" : { "Ref" : "InternetGateway" }
            </#if>
          }
    },

    "PublicSubnetRouteTableAssociation" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
        "SubnetId" : { "Ref" : "PublicSubnet" },
        "RouteTableId" : { "Ref" : "PublicRouteTable" }
      }
    },
    </#if>

    <#list instanceGroups as group>
	"AmbariNodes${group.groupName?replace(''_'', '''')}" : {
      "Type" : "AWS::AutoScaling::AutoScalingGroup",
      <#if !existingSubnet>
      "DependsOn" : [ "PublicSubnetRouteTableAssociation", "PublicRoute" ],
      </#if>
      "Properties" : {
        <#if !existingSubnet>
        "AvailabilityZones" : [{ "Fn::GetAtt" : [ "PublicSubnet", "AvailabilityZone" ] }],
        "VPCZoneIdentifier" : [{ "Ref" : "PublicSubnet" }],
        <#else>
        "VPCZoneIdentifier" : [{ "Ref" : "SubnetId" }],
        </#if>
        "LaunchConfigurationName" : { "Ref" : "AmbariNodeLaunchConfig${group.groupName?replace(''_'', '''')}" },
        "MinSize" : 0,
        "MaxSize" : ${group.instanceCount},
        "DesiredCapacity" : ${group.instanceCount},
        "Tags" : [ { "Key" : "Name", "Value" : { "Fn::Join" : ["-", [ { "Ref" : "StackName" }, "${group.groupName}"]] }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "owner", "Value" : { "Ref" : "StackOwner" }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "instanceGroup", "Value" : "${group.groupName}", "PropagateAtLaunch" : "true" }]
      }
    },

    "AmbariNodeLaunchConfig${group.groupName?replace(''_'', '''')}"  : {
      "Type" : "AWS::AutoScaling::LaunchConfiguration",
      "Properties" : {
        <#if group.ebsOptimized == true>
        "EbsOptimized" : "true",
        </#if>
        <#if enableInstanceProfile && !existingRole>
        "IamInstanceProfile" : { "Ref": "S3InstanceProfile" },
        </#if>
        <#if existingRole && enableInstanceProfile>
        "IamInstanceProfile" : { "Ref": "InstanceProfile" },
        </#if>
      	"BlockDeviceMappings" : [
      	  {
            "DeviceName" : { "Ref" : "RootDeviceName" },
            "Ebs" : {
              "VolumeSize" : "50",
              "VolumeType" : "gp2"
            }
          }
		  <#assign seq = ["b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"]>
			<#list seq as x>
			<#if x_index = group.volumeCount><#break></#if>
  		  ,{
          	"DeviceName" : "/dev/xvd${x}",
          	<#if group.volumeType == "ephemeral">
            "VirtualName" : "ephemeral${x_index}"
            <#else>
            "Ebs" : {
            <#if group.ebsEncrypted == true>
              "SnapshotId" : "${snapshotId}",
            </#if>
              "VolumeSize" : ${group.volumeSize},
              "VolumeType" : "${group.volumeType}"
            }
            </#if>
      	  }
			</#list>
      	],
        "ImageId"        : { "Ref" : "AMI" },
        "SecurityGroups" : [ { "Ref" : "ClusterNodeSecurityGroup${group.groupName?replace(''_'', '''')}" } ],
        "InstanceType"   : "${group.flavor}",
        "KeyName"        : { "Ref" : "KeyName" },
        <#if group.spotPrice??>
        "SpotPrice"      : "${group.spotPrice}",
        </#if>
        <#if group.type == "CORE">
        "UserData"       : { "Fn::Base64" : { "Ref" : "CBUserData"}}
        </#if>
        <#if group.type == "GATEWAY">
        "UserData"       : { "Fn::Base64" : { "Ref" : "CBGateWayUserData"}}
        </#if>
      }
    },

    "ClusterNodeSecurityGroup${group.groupName?replace(''_'', '''')}" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Allow access from web and bastion as well as outbound HTTP and HTTPS traffic",
        <#if existingVPC>
        "VpcId" : { "Ref" : "VPCId" },
        <#else>
        "VpcId" : { "Ref" : "VPC" },
        </#if>
        "SecurityGroupIngress" : [
          <#if defaultInboundSecurityGroup??>
              { "IpProtocol" : "tcp", "FromPort" : "22", "ToPort" : "22", "SourceSecurityGroupId" : "${defaultInboundSecurityGroup}"} ,
              { "IpProtocol" : "tcp", "FromPort" : "${gatewayPort}", "ToPort" : "${gatewayPort}", "SourceSecurityGroupId" : "${defaultInboundSecurityGroup}"},
          </#if>
          <#if cloudbreakPublicIp??>
              { "IpProtocol" : "tcp", "FromPort" : "22", "ToPort" : "22", "CidrIp" : "${cloudbreakPublicIp}/32"} ,
              { "IpProtocol" : "tcp", "FromPort" : "${gatewayPort}", "ToPort" : "${gatewayPort}", "CidrIp" : "${cloudbreakPublicIp}/32"},
          </#if>
          <#list group.rules as r>
            <#list r.ports as p>
              { "IpProtocol" : "${r.protocol}", "FromPort" : "${p}", "ToPort" : "${p}", "CidrIp" : "${r.cidr}"} ,
            </#list>
		  </#list>
          <#list cbSubnet as s>
              { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "${s}"} ,
              { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"} ,
              { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"}<#if (s_index + 1) != cbSubnet?size> ,</#if>
          </#list>
        ]
      }
    }<#if (group_index + 1) != instanceGroups?size>,</#if>
    </#list>
  }

  <#if mapPublicIpOnLaunch>
  ,
  "Outputs" : {
    "AmbariUrl" : {
        "Value" : { "Fn::Join" : ["", ["https://", { "Ref" : "EIP" }, "/ambari/"]] }
    },
    "EIPAllocationID" : {
        "Value" : {"Fn::GetAtt" : [ "EIP" , "AllocationId" ]}
    }
  }
  </#if>

}' WHERE stack.cloudplatform = 'AWS';

UPDATE stack SET stackTemplate = '<#setting number_format="computer">
heat_template_version: 2014-10-16

description: >
  Heat template for Cloudbreak

parameters:

  key_name:
    type: string
    description : Name of a KeyPair to enable SSH access to the instance
  image_id:
    type: string
    description: ID of the image
  app_net_cidr:
    type: string
    description: app network address (CIDR notation)
  <#if network.assignFloatingIp>
  public_net_id:
    type: string
    description: The ID of the public network. You will need to replace it with your DevStack public network ID
  </#if>
  <#if existingNetwork>
  app_net_id:
    type: string
    description: ID of the custom network
  </#if>
  <#if existingNetwork && !existingSubnet>
  router_id:
    type: string
    description: ID of the custom router which belongs to the custom network
  </#if>
  <#if existingSubnet>
  subnet_id:
    type: string
    description: ID of the custom subnet which belongs to the custom network
  </#if>

resources:

  <#if !network.providerNetwork>
  <#if !existingNetwork>
  app_network:
      type: OS::Neutron::Net
      properties:
        admin_state_up: true
        name: app_network
  </#if>

  <#if !existingSubnet>
  app_subnet:
      type: OS::Neutron::Subnet
      properties:
        <#if existingNetwork>
        network_id: { get_param: app_net_id }
        <#else>
        network_id: { get_resource: app_network }
        </#if>
        cidr: { get_param: app_net_cidr }
  </#if>

  <#if !existingNetwork>
  router:
      type: OS::Neutron::Router

  router_gateway:
      type: OS::Neutron::RouterGateway
      properties:
        router_id: { get_resource: router }
        <#if network.assignFloatingIp>
        network_id: { get_param: public_net_id }
        </#if>
  </#if>

  <#if !existingSubnet>
  router_interface:
      type: OS::Neutron::RouterInterface
      properties:
        <#if existingNetwork>
        router_id: { get_param: router_id }
        <#else>
        router_id: { get_resource: router }
        </#if>
        subnet_id: { get_resource: app_subnet }
  </#if>
  </#if>

  gw_user_data_config:
      type: OS::Heat::SoftwareConfig
      properties:
        config: |
${gateway_user_data}

  core_user_data_config:
      type: OS::Heat::SoftwareConfig
      properties:
        config: |
${core_user_data}

  <#list agents as agent>

  ambari_${agent.instanceId}:
    type: OS::Nova::Server
    properties:
      image: { get_param: image_id }
      name: ${agent.name}
      flavor: ${agent.flavor}
      <#if availability_zone?has_content>
      availability_zone : ${availability_zone}
      </#if>
      key_name: { get_param: key_name }
      metadata: ${agent.metadata}
      networks:
        - port: { get_resource: ambari_app_port_${agent.instanceId} }
      <#if agent.volumesCount != 0>
      block_device_mapping:
      <#list agent.volumes as volume>
        - device_name: ${volume.device}
          volume_id: { get_resource: ambari_volume_${agent.instanceId}_${volume_index} }
      </#list>
      </#if>
      user_data_format: SOFTWARE_CONFIG
      <#if agent.type == "GATEWAY">
      user_data:  { get_resource: gw_user_data_config }
      <#elseif agent.type == "CORE">
      user_data:  { get_resource: core_user_data_config }
      </#if>

  <#list agent.volumes as volume>

  ambari_volume_${agent.instanceId}_${volume_index}:
    type: OS::Cinder::Volume
    properties:
      name: hdfs-volume
      size: ${volume.size}

  </#list>

  ambari_app_port_${agent.instanceId}:
    type: OS::Neutron::Port
    properties:
      <#if existingNetwork>
      network_id: { get_param: app_net_id }
      <#else>
      network_id: { get_resource: app_network }
      </#if>
      replacement_policy: AUTO
      <#if !network.providerNetwork>
      fixed_ips:
      <#if existingSubnet>
        - subnet_id: { get_param: subnet_id }
      <#else>
        - subnet_id: { get_resource: app_subnet }
      </#if>
      </#if>
      security_groups: [ { get_resource: security_group_${agent.instance.groupName} } ]

  <#if network.assignFloatingIp>
  ambari_server_floatingip_${agent.instanceId}:
    type: OS::Neutron::FloatingIP
    properties:
      floating_network_id: { get_param: public_net_id }
      port_id: { get_resource: ambari_app_port_${agent.instanceId} }
  </#if>
  </#list>

  <#list groups as group>

  security_group_${group.name}:
    type: OS::Neutron::SecurityGroup
    properties:
      description: Cloudbreak security group
      name: cb-sec-group_${cb_stack_name}_${group.name}
      rules: [
        <#list group.security.rules as r>
        <#list r.getPorts() as p>
        {remote_ip_prefix: ${r.cidr},
        protocol: ${r.protocol},
        port_range_min: ${p},
        port_range_max: ${p}},
        </#list>
        </#list>
        {remote_ip_prefix: { get_param: app_net_cidr },
        protocol: tcp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: { get_param: app_net_cidr },
        protocol: udp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: 0.0.0.0/0,
        protocol: icmp}]

  </#list>

outputs:
  <#list agents as agent>
  instance_uuid_${agent.instanceId}:
    value: { get_attr: [ambari_${agent.instanceId}, show, id] }
  </#list>' WHERE stack.cloudplatform = 'OPENSTACK';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN stackTemplate;
