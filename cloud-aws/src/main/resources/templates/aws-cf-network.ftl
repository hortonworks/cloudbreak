<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "Deploys a Cloudera Data Platform VPC on AWS.",

  "Parameters" : {
    "StackOwner" : {
        "Description" : "The instances will have this parameter as an Owner tag.",
        "Type" : "String",
        "MinLength": "1",
        "MaxLength": "200"
    },
    "stackowner" : {
        "Description" : "The instances will have this parameter as an owner tag.",
        "Type" : "String",
        "MinLength": "1",
        "MaxLength": "200"
    }
  },

  "Resources" : {

    "VPC" : {
      "Type" : "AWS::EC2::VPC",
      "Properties" : {
        "CidrBlock" : "${vpcCidr}",
        "EnableDnsSupport" : "true",
        "EnableDnsHostnames" : "true",
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Name", "Value" : "VPC-${environmentId}" }
        ]
      }
    },
    "InternetGateway" : {
      "Type" : "AWS::EC2::InternetGateway",
      "Properties" : {
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Name", "Value" : "ig-${environmentId}" }
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
    <#if subnet.publicSubnetCidr?has_content>
    "PubS${subnet.index}" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
        "MapPublicIpOnLaunch" : "true",
        "CidrBlock" : "${subnet.publicSubnetCidr}",
        "VpcId" : { "Ref" : "VPC" },
        "AvailabilityZone" : "${subnet.availabilityZone}",
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "kubernetes.io/role/elb", "Value" : "1" },
          { "Key" : "Name", "Value" : "ps${subnet.index}-${environmentId}" }
        ]
      }
    },
    "PubSRTA${subnet.index}" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
        "SubnetId" : { "Ref" : "PubS${subnet.index}" },
        "RouteTableId" : { "Ref" : "PublicRouteTable" }
      }
    },
    "NG${subnet.index}EIP" : {
        "Type" : "AWS::EC2::EIP",
        "DependsOn" : "AttachGateway",
        "Properties" : {
            "Domain" : { "Ref" : "VPC" }
        }
    },
    "NG${subnet.index}" : {
        "Type" : "AWS::EC2::NatGateway",
        "Properties" : {
            "AllocationId" : { "Fn::GetAtt" : [ "NG${subnet.index}EIP", "AllocationId" ] },
            "SubnetId" : { "Ref" : "PubS${subnet.index}" }
        }
    },
    </#if>
    <#if subnet.privateSubnetCidr?has_content>
    "PrvS${subnet.index}" : {
        "Type" : "AWS::EC2::Subnet",
        "Properties" : {
            "MapPublicIpOnLaunch" : "false",
            "CidrBlock" : "${subnet.privateSubnetCidr}",
            "VpcId" : { "Ref" : "VPC" },
            "AvailabilityZone" : "${subnet.availabilityZone}",
            "Tags" : [
              { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
              { "Key" : "kubernetes.io/role/elb", "Value" : "1" },
              { "Key" : "Name", "Value" : "ps${subnet.index}-${environmentId}" }
            ]
        }
    },
    "PRT${subnet.index}" : {
        "Type" : "AWS::EC2::RouteTable",
        "Properties" : {
            "VpcId" : { "Ref" : "VPC" },
            "Tags" : [
              { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
              { "Key" : "Name", "Value" : "prt${subnet.index}-${environmentId}" }
            ]
        }
    },
    "PSRTA${subnet.index}" : {
        "Type" : "AWS::EC2::SubnetRouteTableAssociation",
        "Properties" : {
            "SubnetId" : { "Ref" : "PrvS${subnet.index}" },
            "RouteTableId" : { "Ref" : "PRT${subnet.index}" }
        }
    },
    "PRt${subnet.index}" : {
        "Type" : "AWS::EC2::Route",
        "DependsOn" : [ "PRT${subnet.index}", "AttachGateway" ],
        "Properties" : {
            "DestinationCidrBlock" : "0.0.0.0/0",
            "RouteTableId" : { "Ref" : "PRT${subnet.index}" },
            "NatGatewayId" : { "Ref" : "NG${subnet.subnetGroup}" }
        }
    },
    </#if>
    </#list>

    <#if privateSubnetEnabled == true>
    "S3Endpoint" : {
      "Type" : "AWS::EC2::VPCEndpoint",
      "Properties" : {
        "RouteTableIds" : [
        <#list subnetDetails as subnet>
            <#if subnet.privateSubnetCidr?has_content>
             {"Ref" : "PRT${subnet.index}"}<#if subnet_has_next>,</#if>
            </#if>
        </#list>
        ],
        "ServiceName" : { "Fn::Sub": "com.amazonaws.${r"${AWS::Region}"}.s3" },
        "VpcId" : {"Ref" : "VPC"}
      }
    },
    "DDBEndpoint" : {
      "Type" : "AWS::EC2::VPCEndpoint",
      "Properties" : {
        "RouteTableIds" : [
        <#list subnetDetails as subnet>
            <#if subnet.privateSubnetCidr?has_content>
             {"Ref" : "PRT${subnet.index}"}<#if subnet_has_next>,</#if>
            </#if>
        </#list>
        ],
        "ServiceName" : { "Fn::Sub": "com.amazonaws.${r"${AWS::Region}"}.dynamodb" },
        "VpcId" : {"Ref" : "VPC"}
      }
    },
    </#if>

    "PublicRouteTable" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "Tags" : [
          { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
          { "Key" : "Name", "Value" : "prt-${environmentName}-${environmentId}" }
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
    <#if subnet.publicSubnetCidr?has_content>
    "id${subnet.index}" : {
        "Value" :  { "Ref" : "PubS${subnet.index}" }
    },
    </#if>
    <#if subnet.privateSubnetCidr?has_content>
    "id${subnet.index}" : {
        "Value" :  { "Ref" : "PrvS${subnet.index}" }
    },
    </#if>
    </#list>
    "CreatedVpc": {
        "Value" : { "Ref" : "VPC" }
    }
  }
}
