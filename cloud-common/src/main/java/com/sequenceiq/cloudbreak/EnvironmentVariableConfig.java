package com.sequenceiq.cloudbreak;

public class EnvironmentVariableConfig {

    public static final String CB_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_THREADPOOL_CAPACITY_SIZE = "4000";
    public static final String CB_INTERMEDIATE_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_INTERMEDIATE_THREADPOOL_CAPACITY_SIZE = "4000";
    public static final String CB_CONTAINER_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_CONTAINER_THREADPOOL_CAPACITY_SIZE = "4000";

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

    public static final String CB_AWS_SPOTINSTANCE_ENABLED = "false";

    public static final String CB_AWS_EXTERNAL_ID = "provision-ambari";

    public static final String CB_AWS_CF_TEMPLATE_PATH = "templates/aws-cf-stack.ftl";

    public static final String CB_BLUEPRINT_DEFAULTS = "hdp-small-default,hdp-spark-cluster,hdp-streaming-cluster";
    public static final String CB_TEMPLATE_DEFAULTS = "minviable-gcp,minviable-azure,minviable-aws";

    public static final String CB_SMTP_SENDER_FROM = "no-reply@sequenceiq.com";
    public static final String CB_MAIL_SMTP_AUTH = "true";
    public static final String CB_MAIL_SMTP_STARTTLS_ENABLE = "true";
    public static final String CB_MAIL_SMTP_TYPE = "smtp";
    public static final String CB_SUCCESS_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH = "templates/cluster-installer-mail-success.ftl";
    public static final String CB_FAILED_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH = "templates/cluster-installer-mail-fail.ftl";

    public static final String CB_CONTAINER_ORCHESTRATOR = "SWARM";
    public static final String CB_SUPPORTED_CONTAINER_ORCHESTRATORS = "com.sequenceiq.cloudbreak.orchestrator.swarm.SwarmContainerOrchestrator";
    public static final String CB_DOCKER_AMBARI_IMAGE_NAME = "sequenceiq/ambari:2.1.2-v4";
    public static final String CB_DOCKER_CONTAINER_AMBARI_WARMUP = CB_DOCKER_AMBARI_IMAGE_NAME;
    public static final String CB_DOCKER_CONTAINER_AMBARI = CB_DOCKER_AMBARI_IMAGE_NAME;
    public static final String CB_DOCKER_CONTAINER_REGISTRATOR = "sequenceiq/registrator:v5.2";
    public static final String CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN = "sequenceiq/docker-consul-watch-plugn:1.0";
    public static final String CB_DOCKER_CONTAINER_AMBARI_DB = "postgres:9.4.1";
    public static final String CB_DOCKER_CONTAINER_KERBEROS = "sequenceiq/kerberos:2.1.0-consul";
    public static final String CB_DOCKER_CONTAINER_LOGROTATE = "sequenceiq/logrotate:v0.5.1";
    public static final String CB_DOCKER_CONTAINER_MUNCHAUSEN = "sequenceiq/munchausen:0.5.5";

    public static final String CB_DOCKER_RELOCATE = "true";

    public static final String CB_AZURE_IMAGE_NAME = "cb-centos71-amb212-2015-10-296_2015-October-29_13-7-os-2015-10-29.vhd";
    public static final String CB_AZURE_IMAGE_URI = "https://02589fae040d8westeurope2.blob.core.windows.net/images/" + CB_AZURE_IMAGE_NAME;
    public static final String CB_AZURE_RM_IMAGE = new StringBuilder()
            .append(String.format("East Asia:https://sequenceiqeastasia2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("East US:https://sequenceiqeastus12.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("Central US:https://sequenceiqcentralus2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("North Europe:https://sequenceiqnortheurope2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("South Central US:https://sequenceiqouthcentralus2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("North Central US:https://sequenceiqorthcentralus2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("East US 2:https://sequenceiqeastus22.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("Japan East:https://sequenceiqjapaneast2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("Japan West:https://sequenceiqjapanwest2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("Southeast Asia:https://sequenceiqsoutheastasia2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("West US:https://sequenceiqwestus2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("West Europe:https://sequenceiqwesteurope2.blob.core.windows.net/images/%s,", CB_AZURE_IMAGE_NAME))
            .append(String.format("Brazil South:https://sequenceiqbrazilsouth2.blob.core.windows.net/images/%s", CB_AZURE_IMAGE_NAME))
            .toString();
    public static final String CB_AWS_AMI_MAP = new StringBuilder()
            .append("ap-northeast-1:ami-4ea78120,")
            .append("ap-southeast-1:ami-babd7bd9,")
            .append("ap-southeast-2:ami-9188d6f2,")
            .append("eu-central-1:ami-1cedfe70,")
            .append("eu-west-1:ami-24a47c57,")
            .append("sa-east-1:ami-6408b308,")
            .append("us-east-1:ami-2393ed49,")
            .append("us-west-1:ami-4af49b2a,")
            .append("us-west-2:ami-203c2a41")
            .toString();
    public static final String CB_OPENSTACK_IMAGE = "cb-centos71-amb212-2015-10-27";
    public static final String CB_GCP_SOURCE_IMAGE_PATH = "sequenceiqimage/cb-centos71-amb212-2015-10-279.tar.gz";

    public static final String CB_ARM_CENTRAL_STORAGE = "cbstore";
    public static final String CB_ARM_TEMPLATE_PATH = "templates/arm-v2.ftl";
    public static final String CB_ARM_PARAMETER_PATH = "templates/parameters.ftl";

    public static final String CB_OPENSTACK_HEAT_TEMPLATE_PATH = "templates/openstack-heat.ftl";
    public static final String CB_OPENSTACK_API_DEBUG = "false";

    public static final String CB_EVENTBUS_THREADPOOL_CORE_SIZE = "100";
    public static final String CB_CLOUD_API_EXECUTORSERVICE_POOL_SIZE = "40";

    public static final String CB_MAX_AZURE_RESOURCE_NAME_LENGTH = "50";
    public static final String CB_MAX_GCP_RESOURCE_NAME_LENGTH = "63";
    public static final String CB_MAX_AWS_RESOURCE_NAME_LENGTH = "50";
    public static final String CB_MAX_OPENSTACK_RESOURCE_NAME_LENGTH = "120";

    public static final String CB_ADDRESS_RESOLVING_TIMEOUT = "60000";

    public static final String CB_PLATFORM_DEFAULT_VARIANTS = "OPENSTACK:HEAT";

    private EnvironmentVariableConfig() {

    }
}
