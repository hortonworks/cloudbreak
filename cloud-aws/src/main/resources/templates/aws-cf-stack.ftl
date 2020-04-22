<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "Deploys a Cloudera Data Platform cluster on AWS.",

  "Parameters" : {

    "StackName" : {
      "Description" : "Name of the CloudFormation stack that is used to tag instances",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },

    <#if existingVPC>
    "VPCId" : {
      "Description" : "Id of the VPC where to deploy the cluster",
      "Type" : "String",
      "MinLength": "12",
      "AllowedPattern" : "vpc-[a-z0-9]*"
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
      "AllowedPattern" : "subnet-[a-z0-9]*(?:,subnet-[a-z0-9]*)*"
    },
    </#if>

    <#if existingIGW>
    "InternetGatewayId" : {
       "Description" : "Id of the internet gateway used by the VPC",
       "Type" : "String",
       "MinLength": "12",
       "AllowedPattern" : "igw-[a-z0-9]*"
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

    "CBUserData" : {
      "Description" : "User data to be executed",
      "Type" : "String",
      "MinLength": "9",
      "MaxLength": "4096"
    },

    "CBUserData1" : {
      "Description" : "User data to be executed (continued)",
      "Type" : "String",
      "MinLength": "0",
      "MaxLength": "4096"
    },

    "CBUserData2" : {
      "Description" : "User data to be executed (continued)",
      "Type" : "String",
      "MinLength": "0",
      "MaxLength": "4096"
    },

    "CBUserData3" : {
      "Description" : "User data to be executed (continued)",
      "Type" : "String",
      "MinLength": "0",
      "MaxLength": "4096"
    },

    "CBGateWayUserData" : {
      "Description" : "Gateway user data to be executed",
      "Type" : "String",
      "MinLength": "9",
      "MaxLength": "4096"
    },

    "CBGateWayUserData1" : {
      "Description" : "Gateway user data to be executed (continued)",
      "Type" : "String",
      "MinLength": "0",
      "MaxLength": "4096"
    },

    "CBGateWayUserData2" : {
      "Description" : "Gateway user data to be executed (continued)",
      "Type" : "String",
      "MinLength": "0",
      "MaxLength": "4096"
    },

    "CBGateWayUserData3" : {
      "Description" : "Gateway user data to be executed (continued)",
      "Type" : "String",
      "MinLength": "0",
      "MaxLength": "4096"
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
      "Description" : "AMI that's used to start instances",
      "Type" : "String",
      "MinLength": "12",
      "AllowedPattern" : "ami-[a-z0-9]*",
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

    <#if mapPublicIpOnLaunch>
        <#list gatewayGroups as group>
            <#list 1..group.instanceCount as nth>
                "EIP${group.groupName?replace('_', '')}${nth}" : {
                   "Type" : "AWS::EC2::EIP",
                   "Properties" : {
                      "Domain" : "vpc"
                   }
                },
            </#list>
        </#list>
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
	"${group.autoScalingGroupName}" : {
      "Type" : "AWS::AutoScaling::AutoScalingGroup",
      <#if !existingSubnet>
      "DependsOn" : [ "PublicSubnetRouteTableAssociation", "PublicRoute" ],
      </#if>
      "Properties" : {
        <#if !existingSubnet>
        "AvailabilityZones" : [{ "Fn::GetAtt" : [ "PublicSubnet", "AvailabilityZone" ] }],
        "VPCZoneIdentifier" : [{ "Ref" : "PublicSubnet" }],
        <#else>
        <#if group.subnetId??>
        "VPCZoneIdentifier" : [ "${group.subnetId}" ],
        <#else>
        "VPCZoneIdentifier" : [{ "Ref" : "SubnetId" }],
        </#if>
        </#if>
        "MixedInstancesPolicy": {
          "LaunchTemplate" : {
            "LaunchTemplateSpecification": {
              "LaunchTemplateId": { "Ref" : "ClusterManagerNodeLaunchTemplate${group.groupName?replace('_', '')}" },
              "Version": { "Fn::GetAtt": "ClusterManagerNodeLaunchTemplate${group.groupName?replace('_', '')}.LatestVersionNumber" }
            }
          },
          "InstancesDistribution": {
            "OnDemandBaseCapacity": 0,
            "OnDemandPercentageAboveBaseCapacity": ${group.onDemandPercentage},
            "SpotAllocationStrategy": "capacity-optimized"
          }
        },
        "TerminationPolicies" : [ "NewestInstance" ],
        "MinSize" : 0,
        "MaxSize" : ${group.instanceCount},
        "DesiredCapacity" : ${group.instanceCount},
        "Tags" : [ { "Key" : "Name", "Value" : { "Fn::Join" : ["-", [ { "Ref" : "StackName" }, "${group.groupName}"]] }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "instanceGroup", "Value" : "${group.groupName}", "PropagateAtLaunch" : "true" }]
      }
    },

    "${group.launchTemplateName}"  : {
      "Type" : "AWS::EC2::LaunchTemplate",
      "Properties" : {
        "LaunchTemplateData": {
          <#if group.ebsOptimized == true>
          "EbsOptimized" : "true",
          </#if>
          <#if group.hasInstanceProfile>
          "IamInstanceProfile" : {
            "Arn": "${group.instanceProfile}"
          },
          </#if>
          "BlockDeviceMappings" : [
            {
              "DeviceName" : { "Ref" : "RootDeviceName" },
              "Ebs" : {
                "VolumeSize" : "${group.rootVolumeSize}",
                "VolumeType" : "gp2"
              }
            }
            <#assign ephemeralCount = group.getVolumeCount("ephemeral")>
            <#if ephemeralCount != 0>
              <#assign seq = ["b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"]>
              <#list seq as x>
                <#if x_index = ephemeralCount><#break></#if>
                ,{
                  "DeviceName" : "/dev/xvd${x}",
                  "VirtualName" : "ephemeral${x_index}"
                }
              </#list>
            </#if>
          ],
          <#if group.ebsEncrypted == true>
          "ImageId"        : "${group.encryptedAMI}",
          <#else>
          "ImageId"        : { "Ref" : "AMI" },
          </#if>
          <#if group.cloudSecurityIds?size != 0>
          "SecurityGroupIds" : [ <#list group.cloudSecurityIds as cloudSecurityId>
                                 "${cloudSecurityId}"<#if cloudSecurityId_has_next>,</#if>
                               </#list>
                             ],
          <#else>
          "SecurityGroupIds" : [ { "Ref" : "ClusterNodeSecurityGroup${group.groupName?replace('_', '')}" } ],
          </#if>
          "InstanceType"   : "${group.flavor}",
          "KeyName"        : { "Ref" : "KeyName" },
          <#if group.type == "CORE">
          "UserData"       : { "Fn::Base64" : { "Fn::Join" : ["", [ { "Ref" : "CBUserData"},
                                                                    { "Ref" : "CBUserData1"},
                                                                    { "Ref" : "CBUserData2"},
                                                                    { "Ref" : "CBUserData3"}]] }}
          </#if>
          <#if group.type == "GATEWAY">
          "UserData"       : { "Fn::Base64" : { "Fn::Join" : ["", [ { "Ref" : "CBGateWayUserData"},
                                                                    { "Ref" : "CBGateWayUserData1"},
                                                                    { "Ref" : "CBGateWayUserData2"},
                                                                    { "Ref" : "CBGateWayUserData3"}]] }}
          </#if>
        }
      }
    }

	<#if group.cloudSecurityIds?size == 0>,
    "ClusterNodeSecurityGroup${group.groupName?replace('_', '')}" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Allow access from web and bastion as well as outbound HTTP and HTTPS traffic",
        <#if existingVPC>
        "VpcId" : { "Ref" : "VPCId" },
        <#else>
        "VpcId" : { "Ref" : "VPC" },
        </#if>
        "SecurityGroupIngress" : [
          <#list group.rules as r>
            <#list r.ports as p>
              { "IpProtocol" : "${r.protocol}", "FromPort" : "${p.from}", "ToPort" : "${p.to}", "CidrIp" : "${r.cidr}"} ,
            </#list>
		  </#list>
          <#list cbSubnet as s>
              { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "${s}"} ,
              { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"} ,
              { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"}<#if (s_index + 1) != cbSubnet?size> ,</#if>
          </#list>
          <#if group.useNetworkCidrAsSourceForDefaultRules>
              <#if vpcSubnet?size != 0 >,</#if>
            <#list vpcSubnet as s>
                { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "${s}"} ,
                { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"} ,
                { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"}<#if (s_index + 1) != vpcSubnet?size> ,</#if>
            </#list>
          </#if>
        ]
      }
    }
    </#if>
    <#if group_has_next>,</#if>
    </#list>
  }
  <#if mapPublicIpOnLaunch || (enableInstanceProfile && !existingRole)>
  ,
  "Outputs" : {
  <#if mapPublicIpOnLaunch>
    <#list gatewayGroups as group>
      <#list 1..group.instanceCount as nth>
          "EIPAllocationID${group.groupName?replace('_', '')}${nth}" : {
              "Value" : {"Fn::GetAtt" : [ "EIP${group.groupName?replace('_', '')}${nth}" , "AllocationId" ]}
          }<#if (nth_index + 1) != group.instanceCount || group?has_next>,</#if>
        </#list>
    </#list>
  </#if>
  <#if enableInstanceProfile && !existingRole>
    <#if mapPublicIpOnLaunch>,</#if>
    "S3AccessRole": {
        "Value" : {"Fn::GetAtt" : ["S3AccessRole", "Arn"] }
    }
  </#if>
  <#if !existingVPC>
    ,"CreatedVpc": {
        "Value" : { "Ref" : "VPC" }
    }
  </#if>
  <#if !existingSubnet>
    ,"CreatedSubnet": {
        "Value" :  { "Ref" : "PublicSubnet" }
    }
  </#if>
  }
  </#if>
}
