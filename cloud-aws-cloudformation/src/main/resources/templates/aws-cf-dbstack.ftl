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
        "MaxValue": 35
    },
    "DBInstanceClassParameter": {
        "Type": "String",
        "Default": "db.m5.large",
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
        "Description": "Engine version",
        "MinLength": 1,
        "MaxLength": 64
    },
    "MasterUsernameParameter": {
        "Type": "String",
        "Description": "Master username",
        "AllowedPattern": "[A-Za-z][A-Za-z0-9]+",
        "MinLength": 1,
        "MaxLength": 16,
        "NoEcho": "true"
    },
    "MasterUserPasswordParameter": {
        "Type": "String",
        "Description": "Master user password",
        "MinLength": 8,
        "MaxLength": 30,
        "NoEcho": "true"
    },
    "MultiAZParameter": {
        "Type": "String",
        "Default": "true",
        "Description": "Whether to use a multi-AZ deployment",
        "AllowedValues": [ "true", "false" ]
    },
    "StorageTypeParameter": {
        "Type": "String",
        "Default": "gp3",
        "Description": "Storage type",
        "AllowedValues": [ "standard", "gp2", "gp3", "io1" ]
    },
    "DeletionProtectionParameter": {
        "Type": "String",
        "Default": "true",
        "Description": "Value indicates whether the DB instance has deletion protection enabled.",
        "AllowedValues": [ "true", "false"]
    },
    <#if hasPort>
    "PortParameter": {
        "Type": "Number",
        "Description": "Database port",
        "MinValue": 0,
        "MaxValue": 65355
    },
    </#if>
    <#if useSslEnforcement>
    "DBParameterGroupNameParameter": {
        "Type": "String",
        "Description": "DB parameter group name"
    },
    "DBParameterGroupFamilyParameter": {
        "Type": "String",
        "Description": "DB parameter group family"
    },
    <#if sslCertificateIdentifierDefined>
    "SslCertificateIdentifierParameter": {
        "Type": "String",
        "Description": "SSL CA certificate identifier"
    },
    </#if>
    </#if>
    <#if hasSecurityGroup>
    "VPCSecurityGroupsParameter": {
        "Type": "List<AWS::EC2::SecurityGroup::Id>",
        "Description": "VPC security groups"
    }
    <#else>
    "DBSecurityGroupNameParameter": {
        "Type": "String",
        "Description": "DB security group name"
    },
    "VPCIdParameter": {
        "Type":"AWS::EC2::VPC::Id",
        "Description":"VPC ID"
    }
    </#if>
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
                    <#list networkCidrs as cidr>
                    {
                      "IpProtocol" : "tcp",
                      <#if !hasPort>
                      "FromPort": 5432,
                      "ToPort" : 5432,
                      <#else>
                      "FromPort": { "Ref": "PortParameter" },
                      "ToPort" : { "Ref": "PortParameter" },
                      </#if>
                      "CidrIp" : "${cidr}"
                    }<#if cidr_has_next>,</#if>
                    </#list>
              ],
              "VpcId" : { "Ref": "VPCIdParameter" }
            }
        },
        </#if>
        <#if useSslEnforcement>
        "DBParameterGroup": {
            "Type": "AWS::RDS::DBParameterGroup",
            "Properties": {
                "Description": { "Fn::Sub": "DB parameter group for ${r"${DBInstanceIdentifierParameter}"}" },
                "Family": { "Ref": "DBParameterGroupFamilyParameter" },
                "Parameters": { "rds.force_ssl": "1" },
                "Tags": [
                    { "Key" : "Name", "Value" : { "Ref" : "DBParameterGroupNameParameter" } }
                ]
            }
        },
        </#if>
        "DBSubnetGroup": {
            "Type": "AWS::RDS::DBSubnetGroup",
            "Properties": {
                "DBSubnetGroupDescription": { "Fn::Sub": "DB subnet group for ${r"${DBInstanceIdentifierParameter}"}" },
                "DBSubnetGroupName": { "Ref": "DBSubnetGroupNameParameter" },
                "SubnetIds": { "Ref": "DBSubnetGroupSubnetIdsParameter" }
            }
        },
        "DBInstance": {
            "Type": "AWS::RDS::DBInstance",
            "Properties": {
                "AllocatedStorage": { "Ref": "AllocatedStorageParameter" },
                "BackupRetentionPeriod": { "Ref": "BackupRetentionPeriodParameter" },
                "DBInstanceClass": { "Ref": "DBInstanceClassParameter" },
                "DBInstanceIdentifier": { "Ref": "DBInstanceIdentifierParameter" },
                <#if useSslEnforcement>
                "DBParameterGroupName": { "Ref": "DBParameterGroup" },
                <#if sslCertificateIdentifierDefined>
                "CACertificateIdentifier": { "Ref": "SslCertificateIdentifierParameter" },
                </#if>
                </#if>
                "DBSubnetGroupName": { "Ref": "DBSubnetGroup" },
                "Engine": { "Ref": "EngineParameter" },
                "CopyTagsToSnapshot":true,
                "EngineVersion": { "Ref": "EngineVersionParameter" },
                "MasterUserPassword": { "Ref": "MasterUserPasswordParameter" },
                "MasterUsername": { "Ref": "MasterUsernameParameter" },
                "DeletionProtection": { "Ref": "DeletionProtectionParameter" },
                "MultiAZ": { "Fn::If" : [ "UseMultiAZ", true, false ] },
                "AutoMinorVersionUpgrade": { "Fn::If" : [ "UseMultiAZ", true, false ] },
                <#if hasPort>
                "Port": { "Ref": "PortParameter" },
                </#if>
                "StorageEncrypted": true,
                <#if hasCustomKmsEnabled>
                "KmsKeyId" : "${kmsKey}",
                </#if>
                "StorageType": { "Ref": "StorageTypeParameter" },
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
      <#if useSslEnforcement>
      "CreatedDBParameterGroup": { "Value": { "Ref": "DBParameterGroup" } },
      </#if>
      "CreatedDBSubnetGroup": { "Value": { "Ref": "DBSubnetGroup" } }
  }
}