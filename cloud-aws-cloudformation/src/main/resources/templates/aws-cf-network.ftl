<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",
  "Description" : "Deploys a Cloudera Data Platform VPC on AWS.",
  "Resources" : {

    "VPC" : {
      "Type" : "AWS::EC2::VPC",
      "Properties" : {
        "CidrBlock" : "${vpcCidr}",
        "EnableDnsSupport" : "true",
        "EnableDnsHostnames" : "true",
        "Tags" : [
          { "Key" : "Name", "Value" : "VPC-${environmentId}" }
        ]
      }
    },
    "InternetGateway" : {
      "Type" : "AWS::EC2::InternetGateway",
      "Properties" : {
        "Tags" : [
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
    <#if vpcInterfaceEndpoints?has_content>
    "EndpointSecurityGroup" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Security group for the private interface service endpoints",
        "SecurityGroupIngress" : [{
          "IpProtocol" : "-1",
          "CidrIp" : "${vpcCidr}"
        }],
        "VpcId" : { "Ref" : "VPC" },
        "Tags" : [
          { "Key" : "Name", "Value" : "vpcep-sg-${environmentId}" }
        ]
      }
    },
    </#if>

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
    <#if privateSubnetEnabled == true>
    "NG${subnet.index}EIP" : {
        "Type" : "AWS::EC2::EIP",
        "DependsOn" : "AttachGateway",
        "Properties" : {
            "Domain" : { "Ref" : "VPC" }
        }
    },
    "NG${subnet.subnetGroup}" : {
        "Type" : "AWS::EC2::NatGateway",
        "Properties" : {
            "AllocationId" : { "Fn::GetAtt" : [ "NG${subnet.index}EIP", "AllocationId" ] },
            "SubnetId" : { "Ref" : "PubS${subnet.index}" }
        }
    },
    </#if>
    </#if>
    <#if privateSubnetEnabled == true>
    <#if subnet.privateSubnetCidr?has_content>
    "PrvS${subnet.index}" : {
        "Type" : "AWS::EC2::Subnet",
        "Properties" : {
            "MapPublicIpOnLaunch" : "false",
            "CidrBlock" : "${subnet.privateSubnetCidr}",
            "VpcId" : { "Ref" : "VPC" },
            "AvailabilityZone" : "${subnet.availabilityZone}",
            "Tags" : [
              { "Key" : "kubernetes.io/role/internal-elb", "Value" : "1" },
              { "Key" : "Name", "Value" : "ps${subnet.index}-${environmentId}" }
            ]
        }
    },
    "PRT${subnet.index}" : {
        "Type" : "AWS::EC2::RouteTable",
        "Properties" : {
            "VpcId" : { "Ref" : "VPC" },
            "Tags" : [
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
    </#if>
    </#list>

    <#list vpcGatewayEndpoints as vpcGatewayEndpoint>
    "${vpcGatewayEndpoint.serviceEndpointTemplateName}" : {
      "Type" : "AWS::EC2::VPCEndpoint",
      "Properties" : {
        "RouteTableIds" : [
          {"Ref" : "PublicRouteTable"} <#if privateSubnetEnabled == true>, </#if>
          <#if privateSubnetEnabled == true>
          <#list subnetDetails as subnet>
            <#if subnet.privateSubnetCidr?has_content>
              {"Ref" : "PRT${subnet.index}"}<#if subnet_has_next>,</#if>
            </#if>
          </#list>
          </#if>
        ],
        "ServiceName" : { "Fn::Sub": "com.amazonaws.${r"${AWS::Region}"}.${vpcGatewayEndpoint.serviceName}" },
        "VpcId" : {"Ref" : "VPC"}
      }
    },
    </#list>
    <#list vpcInterfaceEndpoints as vpcInterfaceEndpoint>
    "${vpcInterfaceEndpoint.serviceEndpointTemplateName}" : {
      "Type" : "AWS::EC2::VPCEndpoint",
      "Properties" : {
        "PrivateDnsEnabled" : "true",
        "SecurityGroupIds" : [{ "Ref" : "EndpointSecurityGroup" }],
        "ServiceName" : { "Fn::Sub": "com.amazonaws.${r"${AWS::Region}"}.${vpcInterfaceEndpoint.serviceName}" },
        "SubnetIds" : [
        <#list vpcInterfaceEndpoint.subnetRequests as subnet>
          {"Ref" : "PubS${subnet.index}"}<#if subnet_has_next>,</#if>
        </#list>
        ],
        "VpcEndpointType" : "Interface",
        "VpcId" : {"Ref" : "VPC"}
      }
    },
    </#list>

    "PublicRouteTable" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        "VpcId" : { "Ref" : "VPC" },
        "Tags" : [
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
    <#if privateSubnetEnabled == true>
    <#if subnet.privateSubnetCidr?has_content>
    "id${subnet.index}" : {
        "Value" :  { "Ref" : "PrvS${subnet.index}" }
    },
    </#if>
    </#if>
    </#list>
    "CreatedVpc": {
        "Value" : { "Ref" : "VPC" }
    }
  }
}