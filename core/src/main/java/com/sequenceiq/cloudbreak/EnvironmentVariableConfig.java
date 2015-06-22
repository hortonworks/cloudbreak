package com.sequenceiq.cloudbreak;

public class EnvironmentVariableConfig {

    public static final String CB_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_THREADPOOL_CAPACITY_SIZE = "4000";
    public static final String CB_INTERMEDIATE_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_INTERMEDIATE_THREADPOOL_CAPACITY_SIZE = "4000";
    public static final String CB_CONTAINER_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_CONTAINER_THREADPOOL_CAPACITY_SIZE = "4000";

    public static final String CB_EVENTBUS_THREADPOOL_CORE_SIZE = "100";

    public static final String CB_CERT_DIR = "/certs/";
    public static final String CB_TLS_PRIVATE_KEY_FILE = "client-key.pem";
    public static final String CB_TLS_CERT_FILE = "client.pem";

    public static final String CB_SMTP_SENDER_HOST = "";
    public static final String CB_SMTP_SENDER_PORT = "25";
    public static final String CB_SMTP_SENDER_USERNAME = "";
    public static final String CB_SMTP_SENDER_PASSWORD = "";

    public static final String CB_DB_ENV_USER = "postgres";
    public static final String CB_DB_ENV_PASS = "";
    public static final String CB_DB_ENV_DB = "postgres";

    public static final String CB_PLUGINS_TRUSTED_SOURCES = "all-accounts";

    public static final String CB_AWS_EXTERNAL_ID = "provision-ambari";

    public static final String CB_PUBLIC_IP = "0.0.0.0/0";

    public static final String CB_AWS_CF_TEMPLATE_PATH = "templates/aws-cf-stack.ftl";

    public static final String CB_OPENSTACK_HEAT_TEMPLATE_PATH = "templates/openstack-heat.ftl";

    public static final String CB_BLUEPRINT_DEFAULTS = "hdp-small-default,hdp-spark-cluster,hdp-streaming-cluster";
    public static final String CB_TEMPLATE_DEFAULTS = "minviable-gcp,minviable-azure,minviable-aws";

    public static final String CB_SMTP_SENDER_FROM = "no-reply@sequenceiq.com";
    public static final String CB_SUCCESS_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH = "templates/cluster-installer-mail-success.ftl";
    public static final String CB_FAILED_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH = "templates/cluster-installer-mail-fail.ftl";

    public static final String CB_CONTAINER_ORCHESTRATOR = "SWARM";
    public static final String CB_SUPPORTED_CONTAINER_ORCHESTRATORS = "com.sequenceiq.cloudbreak.orchestrator.swarm.SwarmContainerOrchestrator";
    public static final String CB_DOCKER_CONTAINER_AMBARI_WARMUP = "sequenceiq/ambari-warmup:2.0.0-consul";
    public static final String CB_DOCKER_CONTAINER_AMBARI = "sequenceiq/ambari:2.0.0-consul";
    public static final String CB_DOCKER_CONTAINER_REGISTRATOR = "sequenceiq/registrator:v5.2";
    public static final String CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN = "sequenceiq/docker-consul-watch-plugn:2.0.0-consul";
    public static final String CB_DOCKER_CONTAINER_AMBARI_DB = "postgres:9.4.1";
    public static final String CB_DOCKER_CONTAINER_BAYWATCH_SERVER = "sequenceiq/baywatch:v0.5.0";
    public static final String CB_DOCKER_CONTAINER_BAYWATCH_CLIENT = "sequenceiq/baywatch-client:v0.5.0";

    public static final String CB_BAYWATCH_ENABLED = "false";
    public static final String CB_BAYWATCH_EXTERN_LOCATION = "";

    public static final String CB_AZURE_IMAGE_URI = "https://102589fae040d8westeurope.blob.core.windows.net/images/packer-cloudbreak-2015-06-25-centos71_2015-June-25_8-55-os-2015-06-25.vhd";
    public static final String CB_AWS_AMI_MAP = "ap-northeast-1:ami-6ce13b6c,ap-southeast-1:ami-9ebfbacc,ap-southeast-2:ami-4d2d5777,eu-west-1:ami-cb1962bc,sa-east-1:ami-330a892e,us-east-1:ami-2d986246,us-west-1:ami-3bc93d7f,us-west-2:ami-cb162cfb";
    public static final String CB_OPENSTACK_IMAGE = "cb-centos71-amb200-2015-06-25";
    public static final String CB_GCP_SOURCE_IMAGE_PATH = "cb-centos71-amb200-2015-06-25-centos7";
    public static final String CB_GCP_AND_AZURE_USER_NAME = "cloudbreak";

    private EnvironmentVariableConfig() {

    }
}
