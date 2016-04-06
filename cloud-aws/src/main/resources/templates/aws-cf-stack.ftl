<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "CloudFormation template to create a VPC with a public securityRule on EC2",

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
      "MaxLength": "15",
      "AllowedPattern" : "subnet-[a-z0-9]{8}"
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
      "Description" : "AMI that's used to start instances",
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
      "VPC"     : { "CIDR" : "${cbSubnet}" },
      "Public"  : { "CIDR" : "${cbSubnet}" }
    }
  },
  </#if>

  "Resources" : {

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
        <#if existingVPC>
        "VpcId" : { "Ref" : "VPCId" },
        "CidrBlock" : { "Ref" : "SubnetCIDR" },
        <#else>
        <#if availabilitySetNeeded>
        "AvailabilityZone" : {"Fn::Join" : ["",[ { "Ref" : "AvailabilitySet" } ] ]} ,
        </#if>
        "MapPublicIpOnLaunch" : true,
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

    <#if !existingIGW>
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
    </#if>


    "PublicSubnetRouteTableAssociation" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
        "SubnetId" : { "Ref" : "PublicSubnet" },
        "RouteTableId" : { "Ref" : "PublicRouteTable" }
      }
    },
    </#if>

    <#list instanceGroups as group>
	"AmbariNodes${group.groupName?replace('_', '')}" : {
      "Type" : "AWS::AutoScaling::AutoScalingGroup",
      <#if !existingSubnet>
      "DependsOn" : "PublicSubnet",
      </#if>
      "Properties" : {
        <#if !existingSubnet>
        "AvailabilityZones" : [{ "Fn::GetAtt" : [ "PublicSubnet", "AvailabilityZone" ] }],
        "VPCZoneIdentifier" : [{ "Ref" : "PublicSubnet" }],
        <#else>
        "VPCZoneIdentifier" : [{ "Ref" : "SubnetId" }],
        </#if>
        "LaunchConfigurationName" : { "Ref" : "AmbariNodeLaunchConfig${group.groupName?replace('_', '')}" },
        "MinSize" : 1,
        "MaxSize" : ${group.instanceCount},
        "DesiredCapacity" : ${group.instanceCount},
        "Tags" : [ { "Key" : "Name", "Value" : { "Ref" : "StackName" }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "owner", "Value" : { "Ref" : "StackOwner" }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "instanceGroup", "Value" : "${group.groupName}", "PropagateAtLaunch" : "true" }]
      }
    },

    "AmbariNodeLaunchConfig${group.groupName?replace('_', '')}"  : {
      "Type" : "AWS::AutoScaling::LaunchConfiguration",
      "Properties" : {
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
        "SecurityGroups" : [ { "Ref" : "ClusterNodeSecurityGroup" } ],
        "InstanceType"   : "${group.flavor}",
        "KeyName"        : { "Ref" : "KeyName" },
        <#if group.spotPrice??>
        "SpotPrice"      : ${group.spotPrice},
        </#if>
        <#if group.type == "CORE">
        "UserData"       : { "Fn::Base64" : { "Ref" : "CBUserData"}}
        </#if>
        <#if group.type == "GATEWAY">
        "UserData"       : { "Fn::Base64" : { "Ref" : "CBGateWayUserData"}}
        </#if>
      }
    },
    </#list>

    "ClusterNodeSecurityGroup" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Allow access from web and bastion as well as outbound HTTP and HTTPS traffic",
        <#if existingVPC>
        "VpcId" : { "Ref" : "VPCId" },
        <#else>
        "VpcId" : { "Ref" : "VPC" },
        </#if>
        "SecurityGroupIngress" : [
          <#list securityRules.rules as r>
            <#list r.ports as p>
                { "IpProtocol" : "${r.protocol}", "FromPort" : "${p}", "ToPort" : "${p}", "CidrIp" : "${r.cidr}"} ,
            </#list>
		  </#list>
		  { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "${cbSubnet}"} ,
          { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${cbSubnet}"} ,
          { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${cbSubnet}"}
        ]
      }
    }
    
  },
  
  "Outputs" : {
  
	"Subnet" : {
	  <#if !existingSubnet>
	  "Value" : { "Ref" : "PublicSubnet" }
	  <#else>
	  "Value" : { "Ref" : "SubnetId" }
	  </#if>
    },
    "SecurityGroup" : {
      "Value" : { "Ref" : "ClusterNodeSecurityGroup" }
    }
    
  }

}