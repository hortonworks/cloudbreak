package com.sequenceiq.cloudbreak;

public class EnvironmentVariableConfig {

    public static final String CB_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_THREADPOOL_CAPACITY_SIZE = "4000";
    public static final String CB_INTERMEDIATE_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_INTERMEDIATE_THREADPOOL_CAPACITY_SIZE = "4000";
    public static final String CB_CONTAINER_THREADPOOL_CORE_SIZE = "40";
    public static final String CB_CONTAINER_THREADPOOL_CAPACITY_SIZE = "4000";

    public static final String CB_REACTOR_THREADPOOL_CORE_SIZE = "100";

    public static final String CB_SMTP_SENDER_HOST = "";
    public static final String CB_SMTP_SENDER_PORT = "25";
    public static final String CB_SMTP_SENDER_USERNAME = "";
    public static final String CB_SMTP_SENDER_PASSWORD = "";

    public static final String CB_DB_ENV_USER = "postgres";
    public static final String CB_DB_ENV_PASS = "";
    public static final String CB_DB_ENV_DB = "postgres";
    public static final String CB_HBM2DDL_STRATEGY = "update";

    public static final String CB_PLUGINS_TRUSTED_SOURCES = "all-accounts";

    public static final String CB_AWS_EXTERNAL_ID = "provision-ambari";

    public static final String CB_PUBLIC_IP = "0.0.0.0/0";

    public static final String CB_AWS_CF_TEMPLATE_PATH = "templates/aws-cf-stack.ftl";

    public static final String CB_OPENSTACK_HEAT_TEMPLATE_PATH = "templates/openstack-heat.ftl";

    public static final String CB_BLUEPRINT_DEFAULTS = "multi-node-hdfs-yarn,hdp-multinode-default";

    public static final String CB_SMTP_SENDER_FROM = "no-reply@sequenceiq.com";
    public static final String CB_SUCCESS_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH = "templates/cluster-installer-mail-success.ftl";
    public static final String CB_FAILED_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH = "templates/cluster-installer-mail-fail.ftl";

    public static final String CB_CONTAINER_ORCHESTRATOR = "SWARM";
    public static final String CB_DOCKER_CONTAINER_AMBARI_WARMUP = "sequenceiq/ambari-warmup:2.0.0-consul";
    public static final String CB_DOCKER_CONTAINER_AMBARI = "sequenceiq/ambari:2.0.0-consul";
    public static final String CB_DOCKER_CONTAINER_REGISTRATOR = "sequenceiq/registrator:v5.2";
    public static final String CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN = "sequenceiq/docker-consul-watch-plugn:2.0.0-consul";
    public static final String CB_DOCKER_CONTAINER_AMBARI_DB = "postgres:9.4.1";

    public static final String CB_AZURE_IMAGE_URI = "https://102589fae040d8westeurope.blob.core.windows.net/images/packer-cloudbreak-2015-05-19-centos6_2015-May-19_16-30-os-2015-05-19.vhd";
    public static final String CB_AWS_AMI_MAP = "ap-northeast-1:ami-80fa2f80,ap-southeast-1:ami-7a102928,ap-southeast-2:ami-51fa846b,eu-west-1:ami-d37b0ca4,sa-east-1:ami-f15ed8ec,us-east-1:ami-18100970,us-west-1:ami-e33fd6a7,us-west-2:ami-df6d50ef";
    public static final String CB_OPENSTACK_IMAGE = "cb-centos66-amb200-2015-05-19";
    public static final String CB_GCP_SOURCE_IMAGE_PATH = "sequenceiqimage/cb-centos66-amb200-2015-05-19-1810.image.tar.gz";

    public static final String CB_GCP_AND_AZURE_USER_NAME = "cloudbreak";

    private EnvironmentVariableConfig() {

    }
}
