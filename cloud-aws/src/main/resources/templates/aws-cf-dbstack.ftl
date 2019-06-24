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
    "VPCSecurityGroupsParameter": {
        "Type": "List<AWS::EC2::SecurityGroup::Id>",
        "Description": "VPC security groups"
    },

    "StackOwner" : {
      "Description" : "The instances will have this parameter as an Owner tag.",
      "Type" : "String",
      "MinLength": "1",
      "MaxLength": "50"
    }
  },

  "Resources" : {

        "DBSubnetGroup": {
            "Type": "AWS::RDS::DBSubnetGroup",
            "Properties": {
                "DBSubnetGroupDescription": { "Fn::Sub": "DB subnet group for ${r"${DBInstanceIdentifierParameter}"}" },
                "DBSubnetGroupName": { "Ref": "DBSubnetGroupNameParameter" },
                "SubnetIds": { "Ref": "DBSubnetGroupSubnetIdsParameter" },
                "Tags": [
                    { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
                    { "Key" : "cb-resource-type", "Value" : "${database_resource}" },
                    { "Key" : "owner", "Value" : { "Ref" : "StackOwner" } }
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
                "MultiAZ": true,
                "StorageEncrypted": true,
                "Tags": [
                    { "Key" : "Application", "Value" : { "Ref" : "AWS::StackId" } },
                    { "Key" : "cb-resource-type", "Value" : "${database_resource}" },
                    { "Key" : "owner", "Value" : { "Ref" : "StackOwner" } }
                ],
                "VPCSecurityGroups": { "Ref": "VPCSecurityGroupsParameter" }
            }
        }
  },
  "Outputs" : {
      "CreatedDBInstance": { "Value": { "Ref": "DBInstance" } },
      "CreatedDBSubnetGroup": { "Value": { "Ref": "DBSubnetGroup" } }
  }
}
