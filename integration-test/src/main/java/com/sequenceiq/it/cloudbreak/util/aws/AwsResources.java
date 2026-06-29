package com.sequenceiq.it.cloudbreak.util.aws;

public enum AwsResources {

    CLOUDFORMATION_STACK("AWS::CloudFormation::Stack", "cloudformation:stack"),

    AWS_RESERVED_IP("AWS::EC2::EIP", "ec2:elastic-ip"),
    AWS_SUBNET("AWS::EC2::Subnet", "ec2:subnet"),
    AWS_VPC("AWS::EC2::VPC", "ec2:vpc"),
    AWS_ENCRYPTED_VOLUME("AWS::EC2::Volume", "ec2:volume"),
    AWS_ENCRYPTED_AMI("AWS::EC2::Image", "ec2:image"),
    AWS_VOLUMESET("AWS::EC2::Volume", "ec2:volume"),
    AWS_INSTANCE("AWS::EC2::Instance", "ec2:instance"),
    AWS_SECURITY_GROUP("AWS::EC2::SecurityGroup", "ec2:security-group"),
    AWS_ROOT_DISK("AWS::EC2::Volume", "ec2:volume"),

    AWS_CLOUD_WATCH("AWS::CloudWatch::Alarm", "cloudwatch:alarm"),

    RDS_INSTANCE("AWS::RDS::DBInstance", "rds:db"),
    RDS_DB_SUBNET_GROUP("AWS::RDS::DBSubnetGroup", "rds:subgrp"),
    RDS_DB_PARAMETER_GROUP("AWS::RDS::DBParameterGroup", "rds:pg"),

    ELASTIC_LOAD_BALANCER("AWS::ElasticLoadBalancingV2::LoadBalancer", "elasticloadbalancing:loadbalancer"),
    ELASTIC_LOAD_BALANCER_LISTENER("AWS::ElasticLoadBalancingV2::Listener", "elasticloadbalancing:listener"),
    ELASTIC_LOAD_BALANCER_TARGET_GROUP("AWS::ElasticLoadBalancingV2::TargetGroup", "elasticloadbalancing:targetgroup");

    private final String resourceType;

    private final String taggingApiType;

    AwsResources(String resourceType, String taggingApiType) {
        this.resourceType = resourceType;
        this.taggingApiType = taggingApiType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getTaggingApiType() {
        return taggingApiType;
    }
}