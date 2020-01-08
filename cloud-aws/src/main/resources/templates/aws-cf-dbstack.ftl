<#setting number_format="computer">
{
  "AWSTemplateFormatVersion" : "2010-09-09",

  "Description" : "Deploys an RDS instance and associated DB subnet group on AWS.",

  "Parameters" : {

    "AllocatedStorageParameter": {
        "Type": "Number",
        "Default": 10,
        "Description": "Allocated storage, in GB",
        "MinValue": 10
    },
    "BackupRetentionPeriodParameter": {
        "Type": "Number",
        "Default": 3,
        "Description": "Backup retention period, in days",
        "MinValue": 0,
        "MaxValue": 8
    },
    "DBInstanceClassParameter": {
        "Type": "String",
        "Default": "db.t3.medium",
        "Description": "DB instance class"
    },
    "DBInstanceIdentifierParameter": {
        "Type": "String",
        "Description": "DB instance identifier",
        "AllowedPattern": "[A-Za-z][A-Za-z0-9-]*",
        "MinLength": 1,
        "MaxLength": 63
    },
    "DBSubnetGroupNameParameter": {
        "Type": "String",
        "Description": "DB subnet group name"
    },
    "DBSubnetGroupSubnetIdsParameter": {
        "Type": "List<AWS::EC2::Subnet::Id>",
        "Description": "DB subnet group subnet IDs"
    },
    "EngineParameter": {
        "Type": "String",
        "Default": "postgres",
        "Description": "Engine",
        "AllowedValues": [ "postgres" ]
    },
    "EngineVersionParameter": {
        "Type": "String",
        "Default": "10.6",
        "Description": "Engine version",
        "MinLength": 1,
        "MaxLength": 64
    },
    "MasterUsernameParameter": {
        "Type": "String",
        "Description": "Master username",
        "AllowedPattern": "[A-Za-z][A-Za-z0-9]+",
        "MinLength": 1,
        "MaxLength": 16
    },
    "MasterUserPasswordParameter": {
        "Type": "String",
        "Description": "Master user password",
        "MinLength": 8,
        "MaxLength": 30
    },
    "MultiAZParameter": {
        "Type": "String",
        "Default": "true",
        "Description": "Whether to use a multi-AZ deployment",
        "AllowedValues": [ "true", "false" ]
    },
    "StorageTypeParameter": {
        "Type": "String",
        "Default": "gp2",
        "Description": "Storage type",
        "AllowedValues": [ "standard", "gp2", "io1" ]
    },
    <#if hasPort>
    "PortParameter": {
        "Type": "Number",
        "Description": "Database port",
        "MinValue": 0,
        "MaxValue": 65355
    },
    </#if>
    <#if hasSecurityGroup>
    "VPCSecurityGroupsParameter": {
        "Type": "List<AWS::EC2::SecurityGroup::Id>",
        "Description": "VPC security groups"
    },
    <#else>
    "DBSecurityGroupNameParameter": {
        "Type": "String",
        "Description": "DB security group name"
    },
    "VPCIdParameter": {
        "Type":"AWS::EC2::VPC::Id",
        "Description":"VPC ID"
    },
    "VPCCidrParameter": {
        "Type":"String",
        "Description":"VPC Cidr"
    },
    </#if>
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

  "Conditions" : {
    "UseMultiAZ" : { "Fn::Equals" : [{ "Ref": "MultiAZParameter" }, "true"] }
  },

  "Resources" : {
        <#if !hasSecurityGroup>
        "VPCSecurityGroup": {
          "Type" : "AWS::EC2::SecurityGroup",
          "Properties" : {
              "GroupDescription" : "Security group attached to the database server",
              "GroupName" : { "Ref":"DBSecurityGroupNameParameter"},
              "SecurityGroupEgress" : [
                    {
                      "CidrIp": "0.0.0.0/0",
                      "IpProtocol": "-1"
                    }
              ],
              "SecurityGroupIngress" : [
                    {
                      "IpProtocol" : "tcp",
                      <#if !hasPort>
                      "FromPort": 5432,
                      "ToPort" : 5432,
                      <#else>
                      "FromPort": { "Ref": "PortParameter" },
                      "ToPort" : { "Ref": "PortParameter" },
                      </#if>
                      "CidrIp" : {"Ref":"VPCCidrParameter"}
                    }
              ],
              "Tags" : [
                    { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
                    { "Key" : "cb-resource-type", "Value" : "${database_resource}" },
                    { "Key" : "owner", "Value" : { "Ref" : "stackowner" } },
                    { "Key" : "Owner", "Value" : { "Ref" : "StackOwner" } }
              ],
              "VpcId" : { "Ref": "VPCIdParameter" }
            }
        },
        </#if>
        "DBSubnetGroup": {
            "Type": "AWS::RDS::DBSubnetGroup",
            "Properties": {
                "DBSubnetGroupDescription": { "Fn::Sub": "DB subnet group for ${r"${DBInstanceIdentifierParameter}"}" },
                "DBSubnetGroupName": { "Ref": "DBSubnetGroupNameParameter" },
                "SubnetIds": { "Ref": "DBSubnetGroupSubnetIdsParameter" },
                "Tags": [
                    { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
                    { "Key" : "cb-resource-type", "Value" : "${database_resource}" },
                    { "Key" : "owner", "Value" : { "Ref" : "stackowner" } },
                    { "Key" : "Owner", "Value" : { "Ref" : "StackOwner" } }
                ]
            }
        },
        "DBInstance": {
            "Type": "AWS::RDS::DBInstance",
            "Properties": {
                "AllocatedStorage": { "Ref": "AllocatedStorageParameter" },
                "BackupRetentionPeriod": { "Ref": "BackupRetentionPeriodParameter" },
                "DBInstanceClass": { "Ref": "DBInstanceClassParameter" },
                "DBInstanceIdentifier": { "Ref": "DBInstanceIdentifierParameter" },
                "DBSubnetGroupName": { "Ref": "DBSubnetGroup" },
                "Engine": { "Ref": "EngineParameter" },
                "EngineVersion": { "Ref": "EngineVersionParameter" },
                "MasterUserPassword": { "Ref": "MasterUserPasswordParameter" },
                "MasterUsername": { "Ref": "MasterUsernameParameter" },
                "MultiAZ": { "Fn::If" : [ "UseMultiAZ", true, false ] },
                <#if hasPort>
                "Port": { "Ref": "PortParameter" },
                </#if>
                "StorageEncrypted": true,
                "StorageType": { "Ref": "StorageTypeParameter" },
                "Tags": [
                    { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
                    { "Key" : "cb-resource-type", "Value" : "${database_resource}" },
                    { "Key" : "owner", "Value" : { "Ref" : "stackowner" } },
                    { "Key" : "Owner", "Value" : { "Ref" : "StackOwner" } }
                ],
                <#if hasSecurityGroup>
                "VPCSecurityGroups": { "Ref": "VPCSecurityGroupsParameter" }
                <#else>
                "VPCSecurityGroups": [{ "Ref": "VPCSecurityGroup" }]
                </#if>
            },
            "DeletionPolicy": "Delete"
        }
  },
  "Outputs" : {
      "Hostname": { "Value" : { "Fn::GetAtt" : [ "DBInstance", "Endpoint.Address" ]} },
      "Port": { "Value" : { "Fn::GetAtt" : [ "DBInstance", "Endpoint.Port" ]} },
      "CreatedDBInstance": { "Value": { "Ref": "DBInstance" } },
      "CreatedDBSubnetGroup": { "Value": { "Ref": "DBSubnetGroup" } }
  }
}
