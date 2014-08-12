{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "CloudFormation template to create a VPC with a public subnet on EC2",

  "Parameters" : {
  
    "SSHLocation" : {
      "Description" : "Lockdown SSH access (default can be accessed from anywhere)",
      "Type" : "String",
      "MinLength": "9",
      "MaxLength": "18",
      "AllowedPattern" : "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})",
      "ConstraintDescription" : "must be a valid CIDR range of the form x.x.x.x/x."
    },
    
    "StackName" : {
      "Description" : "Name of the CloudFormation stack that is used to tag instances",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },
    
    "StackOwner" : {
      "Description" : "The instances will have this parameter as an Owner tag.",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },
    
    "InstanceCount" : {
      "Description" : "Number of instances that should be started in the stack.",
      "Type" : "Number",
      "Default" : "3",
      "MinValue" : "1",
      "MaxValue" : "99"
    },
    
    "CBUserData" : {
      "Description" : "User data to be executed",
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
 
    "InstanceType" : {
      "Description" : "EC2 instance type of nodes to start",
      "Type" : "String",
      "Default" : "t2.small",
      "AllowedValues" : [ "t2.micro","t2.small","t2.medium","m3.medium","m3.large","m3.xlarge","m3.2xlarge"],
      "ConstraintDescription" : "must be a valid EC2 instance type."
    },
    
    "AMI" : {
      "Description" : "AMI that's used to start instances",
      "Type" : "String",
      "MinLength": "12",
      "MaxLength": "12",
      "AllowedPattern" : "ami-[a-z0-9]{8}",
      "ConstraintDescription" : "must follow pattern: ami-xxxxxxxx"
    },
    
    "VolumeSize" : {
      "Description" : "Size of the attached volumes in GB",
      "Type" : "Number",
      "Default" : "40",
      "MinValue": "10",
      "MaxValue": "1000"
    },
    
    "VolumeType" : {
      "Description" : "Type of the attached volumes (SSD or magnetic)",
      "Type" : "String",
      "Default" : "gp2",
      "AllowedValues" : [ "gp2","standard"],
      "ConstraintDescription" : "must be 'gp2' or 'standard'."
    }

  },

  "Mappings" : {
    "SubnetConfig" : {
      "VPC"     : { "CIDR" : "10.0.0.0/16" },
      "Public"  : { "CIDR" : "10.0.0.0/24" }
    }
  },

  "Resources" : {

    "VPC" : {
      "Type" : "AWS::EC2::VPC",
      "Properties" : {
        "CidrBlock" : { "Fn::FindInMap" : [ "SubnetConfig", "VPC", "CIDR" ]},
        "EnableDnsSupport" : "true",
        "EnableDnsHostnames" : "true",
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "PublicSubnet" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "CidrBlock" : { "Fn::FindInMap" : [ "SubnetConfig", "Public", "CIDR" ]},
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "InternetGateway" : {
      "Type" : "AWS::EC2::InternetGateway",
      "Properties" : {
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "AttachGateway" : {
       "Type" : "AWS::EC2::VPCGatewayAttachment",
       "Properties" : {
         "VpcId" : { "Ref" : "VPC" },
         "InternetGatewayId" : { "Ref" : "InternetGateway" }
       }
    },

	
    "PublicRouteTable" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" }
        ]
      }
    },

    "PublicRoute" : {
      "Type" : "AWS::EC2::Route",
      "DependsOn" : "AttachGateway",
      "Properties" : {
        "RouteTableId" : { "Ref" : "PublicRouteTable" },
        "DestinationCidrBlock" : "0.0.0.0/0",
        "GatewayId" : { "Ref" : "InternetGateway" }
      }
    },

    "PublicSubnetRouteTableAssociation" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
        "SubnetId" : { "Ref" : "PublicSubnet" },
        "RouteTableId" : { "Ref" : "PublicRouteTable" }
      }
    },
    
	"AmbariNodes" : {
      "Type" : "AWS::AutoScaling::AutoScalingGroup",
      "DependsOn" : "PublicSubnet",
      "Properties" : {
        "AvailabilityZones" : [{ "Fn::GetAtt" : [ "PublicSubnet", "AvailabilityZone" ] }],
        "VPCZoneIdentifier" : [{ "Ref" : "PublicSubnet" }],
        "LaunchConfigurationName" : { "Ref" : "AmbariNodeLaunchConfig" },
        "MinSize" : { "Ref" : "InstanceCount" },
        "MaxSize" : { "Ref" : "InstanceCount" },
        "DesiredCapacity" : { "Ref" : "InstanceCount" },
        "Tags" : [ { "Key" : "Name", "Value" : { "Ref" : "StackName" }, "PropagateAtLaunch" : "true" },
        		   { "Key" : "owner", "Value" : { "Ref" : "StackOwner" }, "PropagateAtLaunch" : "true" } ]
      }
    },

    "AmbariNodeLaunchConfig"  : {
      "Type" : "AWS::AutoScaling::LaunchConfiguration",
      "Properties" : {
      	"BlockDeviceMappings" : [
      	  {
            "DeviceName" : "/dev/sda1",
            "Ebs" : {
              "VolumeSize" : "50",
              "VolumeType" : "gp2"
            }
          }, {
            "DeviceName" : "/dev/sdb",
            "NoDevice" : true,
            "Ebs": {}
      	  }, {
            "DeviceName" : "/dev/sdc",
            "NoDevice" : true,
            "Ebs": {}
          }, {
            "DeviceName" : "/dev/sdd",
            "NoDevice" : true,
            "Ebs": {}
          }, {
            "DeviceName" : "/dev/sde",
            "NoDevice" : true,
            "Ebs": {}
          }
		  <#assign seq = ["f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"]>
			<#list seq as x>
			<#if x_index = volumeCount><#break></#if>
  		  ,{
          	"DeviceName" : "/dev/xvd${x}",
          	"Ebs" : {
           	  "VolumeSize" : { "Ref" : "VolumeSize" },
           	  "VolumeType" : { "Ref" : "VolumeType" }
            }
      	  }	
			</#list>
      	],
        "ImageId"        : { "Ref" : "AMI" },
        "SecurityGroups" : [ { "Ref" : "ClusterNodeSecurityGroup" } ],
        "InstanceType"   : { "Ref" : "InstanceType" },
        "KeyName"        : { "Ref" : "KeyName" },
        "AssociatePublicIpAddress" : "true",
        <#if useSpot>
        "SpotPrice"     : "0.4",
        </#if>
        "UserData"       : { "Fn::Base64" : { "Ref" : "CBUserData"}}
      }
    },
    
    "ClusterNodeSecurityGroup" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Allow access from web and bastion as well as outbound HTTP and HTTPS traffic",
        "VpcId" : { "Ref" : "VPC" },
        "SecurityGroupIngress" : [
		  { "IpProtocol" : "tcp", "FromPort" : "80", "ToPort" : "80", "CidrIp" : "0.0.0.0/0"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "8080", "ToPort" : "8080", "CidrIp" : "0.0.0.0/0"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "8088", "ToPort" : "8088", "CidrIp" : "0.0.0.0/0"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "8050", "ToPort" : "8050", "CidrIp" : "0.0.0.0/0"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "8020", "ToPort" : "8020", "CidrIp" : "0.0.0.0/0"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "10020", "ToPort" : "10020", "CidrIp" : "0.0.0.0/0"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "19888", "ToPort" : "19888", "CidrIp" : "0.0.0.0/0"} ,
          { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "10.0.0.0/24"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "10.0.0.0/24"} ,
		  { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "10.0.0.0/24"} ,
		  { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "172.17.0.0/16"} ,
		  { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "172.17.0.0/16"} ,
		  { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "172.17.0.0/16"} ,
          { "IpProtocol" : "tcp", "FromPort" : "22", "ToPort" : "22", "CidrIp" : { "Ref" : "SSHLocation" } } ]
      }
    }
    
  },
  
  "Outputs" : {
  
	"Subnet" : {
	  "Value" : { "Ref" : "PublicSubnet" }
    },
    "SecurityGroup" : {
      "Value" : { "Ref" : "ClusterNodeSecurityGroup" }
    }
    
  }

}