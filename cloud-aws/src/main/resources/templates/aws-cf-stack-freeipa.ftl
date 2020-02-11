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

    "StackOwner" : {
      "Description" : "The instances will have this parameter as an Owner tag.",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    },

    "stackowner" : {
       "Description" : "The instances will have this parameter as an owner tag.",
       "Type" : "String",
       "MinLength": "1",
       "MaxLength": "200"
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
        "LaunchConfigurationName" : { "Ref" : "AmbariNodeLaunchConfig${group.groupName?replace('_', '')}" },
        "TerminationPolicies" : [ "NewestInstance" ],
        "MinSize" : 0,
        "MaxSize" : ${group.instanceCount},
        "DesiredCapacity" : ${group.instanceCount},
        "Tags" : [ { "Key" : "Name", "Value" : { "Fn::Join" : ["-", [ { "Ref" : "StackName" }, "${group.groupName}"]] }, "PropagateAtLaunch" : "true" },
                   { "Key" : "owner", "Value" : { "Ref" : "stackowner" }, "PropagateAtLaunch" : "true" },
                   { "Key" : "Owner", "Value" : { "Ref" : "StackOwner" }, "PropagateAtLaunch" : "true" },
                   { "Key" : "instanceGroup", "Value" : "${group.groupName}", "PropagateAtLaunch" : "true" }]
      }
    },

    "AmbariNodeLaunchConfig${group.groupName?replace('_', '')}"  : {
      "Type" : "AWS::AutoScaling::LaunchConfiguration",
      "Properties" : {
        <#if group.ebsOptimized == true>
        "EbsOptimized" : "true",
        </#if>
        <#if group.hasInstanceProfile>
        "IamInstanceProfile" : "${group.instanceProfile}",
        </#if>
        "BlockDeviceMappings" : [
          {
            "DeviceName" : { "Ref" : "RootDeviceName" },
            "Ebs" : {
              "VolumeSize" : "${group.rootVolumeSize}",
              "VolumeType" : "gp2"
            }
          }
        ],
        <#if group.ebsEncrypted == true>
        "ImageId"        : "${group.encryptedAMI}",
        <#else>
        "ImageId"        : { "Ref" : "AMI" },
        </#if>
        <#if group.cloudSecurityIds?size != 0>
        "SecurityGroups" : [ <#list group.cloudSecurityIds as cloudSecurityId>
                               "${cloudSecurityId}"<#if cloudSecurityId_has_next>,</#if>
                             </#list>
                           ],
        <#else>
        "SecurityGroups" : [ { "Ref" : "ClusterNodeSecurityGroup${group.groupName?replace('_', '')}" } ],
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

    <#if group.cloudSecurityIds?size == 0>,
    "ClusterNodeSecurityGroup${group.groupName?replace('_', '')}" : {
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
        "GroupDescription" : "Allow access from web and bastion as well as outbound HTTP and HTTPS traffic",
        "Tags" : [
            { "Key" : "owner", "Value" : { "Ref" : "stackowner" }},
            { "Key" : "Owner", "Value" : { "Ref" : "StackOwner" }}
        ],
        "VpcId" : { "Ref" : "VPCId" },
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
  }
  </#if>
}