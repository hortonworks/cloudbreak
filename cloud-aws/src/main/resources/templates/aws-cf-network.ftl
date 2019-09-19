<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "Deploys a Cloudera Data Platform cluster on AWS.",

  "Resources" : {

    "VPC" : {
      "Type" : "AWS::EC2::VPC",
      "Properties" : {
        "CidrBlock" : "${vpcCidr}",
        "EnableDnsSupport" : "true",
        "EnableDnsHostnames" : "true",
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" },
          { "Key" : "cb-resource-type", "Value" : "${network_resource}" }
        ]
      }
    },
    "InternetGateway" : {
      "Type" : "AWS::EC2::InternetGateway",
      "Properties" : {
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" },
          { "Key" : "cb-resource-type", "Value" : "${network_resource}" }
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

    <#list subnetDetails as subnet>
    "PublicSubnet${subnet?index}" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
        "MapPublicIpOnLaunch" : "true",
        "CidrBlock" : "${subnet.publicSubnetCidr}",
        "VpcId" : { "Ref" : "VPC" },
        "AvailabilityZone" : "${subnet.availabilityZone}",
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" },
          { "Key" : "cb-resource-type", "Value" : "${network_resource}" },
          { "Key" : "kubernetes.io/role/elb", "Value" : "1" }
        ]
      }
    },
    <#if privateSubnetEnabled == true>
    "PrivateSubnet${subnet?index}" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
        "MapPublicIpOnLaunch" : "false",
        "CidrBlock" : "${subnet.privateSubnetCidr}",
        "VpcId" : { "Ref" : "VPC" },
        "AvailabilityZone" : "${subnet.availabilityZone}",
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Private" },
          { "Key" : "cb-resource-type", "Value" : "${network_resource}" },
          { "Key" : "kubernetes.io/role/elb", "Value" : "1" }
        ]
      }
    },
    "NatGateway${subnet?index}EIP" : {
      "Type" : "AWS::EC2::EIP",
      "DependsOn" : "AttachGateway",
      "Properties" : {
        "Domain" : { "Ref" : "VPC" }
      }
    },
    "NatGateway${subnet?index}" : {
      "Type" : "AWS::EC2::NatGateway",
      "Properties" : {
        "AllocationId" : { "Fn::GetAtt" : [ "NatGateway${subnet?index}EIP", "AllocationId" ] },
        "SubnetId" : { "Ref" : "PublicSubnet${subnet?index}" }
      }
    },
    "PrivateRouteTable${subnet?index}" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Private" },
          { "Key" : "cb-resource-type", "Value" : "${network_resource}" }
        ]
      }
    },
    "PrivateSubnetRouteTableAssociation${subnet?index}" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
        "SubnetId" : { "Ref" : "PrivateSubnet${subnet?index}" },
        "RouteTableId" : { "Ref" : "PrivateRouteTable${subnet?index}" }
      }
    },
    "PrivateRoute${subnet?index}" : {
          "Type" : "AWS::EC2::Route",
          "DependsOn" : [ "PrivateRouteTable${subnet?index}", "AttachGateway" ],
          "Properties" : {
            "DestinationCidrBlock" : "0.0.0.0/0",
            "RouteTableId" : { "Ref" : "PrivateRouteTable${subnet?index}" },
            "NatGatewayId" : { "Ref" : "NatGateway${subnet?index}" }
          }
    },
    </#if>
    "PublicSubnetRouteTableAssociation${subnet?index}" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
        "SubnetId" : { "Ref" : "PublicSubnet${subnet?index}" },
        "RouteTableId" : { "Ref" : "PublicRouteTable" }
      }
    },
    </#list>

    "PublicRouteTable" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Network", "Value" : "Public" },
          { "Key" : "cb-resource-type", "Value" : "${network_resource}" }
        ]
      }
    },
    "PublicRoute" : {
          "Type" : "AWS::EC2::Route",
          "DependsOn" : [ "PublicRouteTable", "AttachGateway" ],
          "Properties" : {
            "RouteTableId" : { "Ref" : "PublicRouteTable" },
            "DestinationCidrBlock" : "0.0.0.0/0",
            "GatewayId" : { "Ref" : "InternetGateway" }
          }
    }
  },

  "Outputs" : {
    <#list subnetDetails as subnet>
    "PublicSubnetId${subnet?index}" : {
        "Value" :  { "Ref" : "PublicSubnet${subnet?index}" }
    },
    "PublicSubnetCidr${subnet?index}" : {
        "Value" : "${subnet.publicSubnetCidr}"
    },
    <#if privateSubnetEnabled == true>
    "PrivateSubnetId${subnet?index}" : {
        "Value" :  { "Ref" : "PrivateSubnet${subnet?index}" }
    },
    "PrivateSubnetCidr${subnet?index}" : {
        "Value" : "${subnet.privateSubnetCidr}"
    },
    </#if>
    "Az${subnet?index}" : {
        "Value" : {"Fn::GetAtt" : ["PublicSubnet${subnet?index}", "AvailabilityZone"] }
    },
    </#list>
    "CreatedVpc": {
        "Value" : { "Ref" : "VPC" }
    }
  }
}