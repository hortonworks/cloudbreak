<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "Deploys a Cloudera Data Platform FreeIPA cluster on AWS.",

  "Parameters" : {

    "StackName" : {
      "Description" : "Name of the CloudFormation stack that is used to tag instances",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },

    "VPCId" : {
      "Description" : "Id of the VPC where to deploy the cluster",
      "Type" : "String",
      "MinLength": "12",
      "AllowedPattern" : "vpc-[a-z0-9]*"
    },

    "SubnetId" : {
      "Description" : "Id of the Subnet within the existing VPC where to deploy the cluster",
      "Type" : "String",
      "MinLength": "15",
      "MaxLength": "255",
      "AllowedPattern" : "subnet-[a-z0-9]*(?:,subnet-[a-z0-9]*)*"
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
    <#if availabilitySetNeeded>
    "AvailabilitySet" : {
        "Description" : "Availability set name",
        "Type" : "String",
        "MinLength": "1",
        "MaxLength": "50"
    },
    </#if>

    "RootDeviceName" : {
      "Description" : "Name of the root device that comes with the AMI",
      "Type" : "String",
      "MinLength": "8",
      "MaxLength": "12"
    }

  },

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

    <#list instanceGroups as group>
    "${group.autoScalingGroupName}" : {
      "Type" : "AWS::AutoScaling::AutoScalingGroup",
      "Properties" : {
        <#if group.subnetId??>
        "VPCZoneIdentifier" : [ "${group.subnetId}" ],
        <#else>
        "VPCZoneIdentifier" : [{ "Ref" : "SubnetId" }],
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
            <#if group.spotMaxPrice??>
            "SpotMaxPrice": "${group.spotMaxPrice}",
            </#if>
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

    "ClusterManagerNodeLaunchTemplate${group.groupName?replace('_', '')}"  : {
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
                <#if group.ebsEncrypted>
                "Encrypted" : "true",
                  <#if group.kmsKeyDefined>
                  "KmsKeyId" : "${group.kmsKey}",
                  </#if>
                </#if>
                "VolumeSize" : "${group.rootVolumeSize}",
                "VolumeType" : "gp2"
              }
            }
          ],
          "ImageId"        : { "Ref" : "AMI" },
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
          <#if group.spotPrice??>
          "SpotPrice"      : "${group.spotPrice}",
          </#if>
          "UserData"       : { "Fn::Base64" : { "Fn::Join" : ["", [ { "Ref" : "CBGateWayUserData"},
                                                                    { "Ref" : "CBGateWayUserData1"},
                                                                    { "Ref" : "CBGateWayUserData2"},
                                                                    { "Ref" : "CBGateWayUserData3"}]] }}
        }
      }
    }

    <#if group.cloudSecurityIds?size == 0>,
    "ClusterNodeSecurityGroup${group.groupName?replace('_', '')}" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Allow access from web and bastion as well as outbound HTTP and HTTPS traffic",
        "VpcId" : { "Ref" : "VPCId" },
        "SecurityGroupIngress" : [
          <#list group.rules as r>
            <#list r.ports as p>
              { "IpProtocol" : "${r.protocol}", "FromPort" : "${p.from}", "ToPort" : "${p.to}", "CidrIp" : "${r.cidr}"} ,
            </#list>
          </#list>
          <#if vpcSubnet?has_content && vpcSubnet?size != 0>
            <#list vpcSubnet as s>
                { "IpProtocol" : "icmp", "FromPort" : "-1", "ToPort" : "-1", "CidrIp" : "${s}"} ,
                { "IpProtocol" : "tcp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"} ,
                { "IpProtocol" : "udp", "FromPort" : "0", "ToPort" : "65535", "CidrIp" : "${s}"}<#if (s_index + 1) != vpcSubnet?size> ,</#if>
            </#list>
          </#if>
        ]
        <#if outboundInternetTraffic == "DISABLED" && (prefixListIds?has_content || vpcCidrs?has_content)>
        ,
        "SecurityGroupEgress" : [
          <#list vpcCidrs as vpcCidr>
                { "IpProtocol" : "-1", "CidrIp" : "${vpcCidr}" }<#if (vpcCidr_index + 1) != vpcCidrs?size> ,</#if>
          </#list>
          <#if prefixListIds?has_content && vpcCidrs?has_content>,</#if>
          <#list prefixListIds as pl>
                { "IpProtocol" : "-1", "FromPort" : "0", "ToPort" : "65535", "DestinationPrefixListId" : "${pl}" }<#if (pl_index + 1) != prefixListIds?size> ,</#if>
          </#list>
        ]
        </#if>
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
  }
  </#if>
}