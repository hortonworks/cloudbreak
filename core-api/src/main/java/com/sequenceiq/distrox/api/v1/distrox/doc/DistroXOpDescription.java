package com.sequenceiq.distrox.api.v1.distrox.doc;

public class DistroXOpDescription {
    public static final String GET_BY_ID = "retrieve stack by id";
    public static final String GET_STATUS_BY_NAME = "retrieve stack status by stack name";
    public static final String GET_STATUS_BY_CRN = "retrieve stack status by stack crn";
    public static final String GET_ENDPOINTS_BY_CRNS = "retrieve stack endpoints by stack crns";
    public static final String PUT_BY_ID = "update stack by id";
    public static final String PUT_BY_NAME = "update stack by name";
    public static final String GET_BY_AMBARI_ADDRESS = "retrieve stack by ambari address";
    public static final String GET_STACK_CERT = "retrieves the TLS certificate used by the gateway";
    public static final String GET_ALL = "retrieve all stacks";
    public static final String LIST = "list stacks by environment crn";
    public static final String LIST_BY_SERVICE_TYPES = "list stacks which have any of the given service types in their cluster template";
    public static final String GET_BY_NAME = "get stack by name";
    public static final String GET_BY_CRN = "get stack by crn";
    public static final String GET_BY_CRN_INTERNAL = "get stack by crn (for internal user)";
    public static final String GET_BY_CRN_INSTANCES_INTERNAL = "get instances by crn (for internal user)";
    public static final String GET_BY_CRNS_INTERNAL = "get stack by crns (for internal user)";
    public static final String CREATE = "create stack";
    public static final String SEND_NOTIFICATION = "send stack health notification";
    public static final String DELETE_BY_NAME = "delete stack by name";
    public static final String DELETE_BY_CRN = "delete stack by crn";
    public static final String DELETE_MULTIPLE = "delete multiple stacks by their names";
    public static final String SYNC_BY_NAME = "syncs the stack by name";
    public static final String SYNC_BY_CRN = "syncs the stack by crn";
    public static final String RETRY_BY_NAME = "retries the stack by name";
    public static final String RETRY_BY_CRN = "retries the stack by crn";
    public static final String STOP_BY_NAME = "stops the stack by name";
    public static final String STOP_BY_CRN = "stops the stack by crn";
    public static final String START_BY_NAME = "starts the stack by name";
    public static final String START_BY_CRN = "starts the stack by crn";
    public static final String ROTATE_SALT_PASSWORD_BY_CRN = "rotates the SaltStack user password of stack by crn";
    public static final String RESTART_CLUSTER_BY_CRN = "restarts the cluster by crn";
    public static final String SCALE_BY_NAME = "scales the stack by name";
    public static final String SCALE_BY_CRN = "scales the stack by crn";
    public static final String VERTICAL_SCALE_BY_NAME = "vertical scale the stack instances (node type/disks) by name";
    public static final String VERTICAL_SCALE_BY_CRN = "vertical scales the instances (node type/disks) stack by crn";
    public static final String DELETE_VOLUMES_BY_STACK_NAME = "delete attached volumes on stack instances by stack name";
    public static final String DELETE_VOLUMES_BY_STACK_CRN = "delete attached volumes on stack instances by stack crn";
    public static final String ADD_VOLUMES_BY_STACK_NAME = "add block storage to stack instance group by stack name";
    public static final String ADD_VOLUMES_BY_STACK_CRN = "add EBS volumes to stack instance group by stack crn";
    public static final String ROOT_VOLUME_UPDATE_BY_DH_NAME = "Update root volume of stack instance group by Datahub name";
    public static final String ROOT_VOLUME_UPDATE_BY_DH_CRN = "Update root volume of to stack instance group by Datahub crn";
    public static final String ROOT_VOLUME_UPDATE_BY_STACK_CRN = "Update root volume of stack instance group by Stack CRN";

    public static final String IMD_UPDATE = "update instance metadata for Distrox's instances";
    public static final String REPAIR_CLUSTER_BY_NAME = "repairs the stack by name";
    public static final String REPAIR_CLUSTER_BY_CRN = "repairs the stack by crn";
    public static final String RDS_CERTIFICATE_ROTATION_BY_NAME = "rotate rds certificate of the stack by name";
    public static final String RDS_CERTIFICATE_ROTATION_BY_CRN = "rotate rds certificate of the stack by crn";
    public static final String MIGRATE_DATABASE_TO_SSL_BY_NAME = "migrate database from non ssl to ssl of the stack by name";
    public static final String MIGRATE_DATABASE_TO_SSL_BY_CRN = "migrate database from non ssl to ssl  of the stack by crn";
    public static final String DELETE_WITH_KERBEROS_BY_NAME = "deletes the stack (with kerberos cluster) by name";
    public static final String DELETE_WITH_KERBEROS_BY_CRN = "deletes the stack (with kerberos cluster) by crn";
    public static final String GET_STACK_REQUEST_BY_NAME = "gets StackRequest by name";
    public static final String GET_STACK_REQUEST_BY_CRN = "gets StackRequest by crn";
    public static final String POST_STACK_FOR_BLUEPRINT = "posts stack for blueprint";
    public static final String DELETE_INSTANCE_BY_ID_BY_NAME = "deletes instance from the stack's cluster by name";
    public static final String DELETE_INSTANCE_BY_ID_BY_CRN = "deletes instance from the stack's cluster by crn";
    public static final String CHECK_IMAGE = "checks image in stack by name";
    public static final String GENERATE_HOSTS_INVENTORY = "Generate hosts inventory";
    public static final String CLI_COMMAND = "produce cli command input";
    public static final String RENEW_CERTIFICATE = "Trigger a certificate renewal on the desired cluster which is identified via crn";
    public static final String RENEW_CERTIFICATE_INTERNAL = "Trigger a certificate renewal on the desired cluster which is identified via crn";
    public static final String GET_DATABASE_SERVER_BY_CLUSTER_CRN = "get database server for Distrox cluster by cluster crn";
    public static final String GET_LAST_FLOW_PROGRESS = "Get last flow operation progress details for resource by resource crn";
    public static final String LIST_FLOW_PROGRESS = "List recent flow operations progress details for resource by resource crn";
    public static final String GET_OPERATION = "Get flow operation progress details for resource by resource crn";
    public static final String GET_DATAHUB_AUDIT_EVENTS = "Get Data Hub audit events";
    public static final String DETERMINE_DATALAKE_DATA_SIZES = "Determines the sizes of the different local data on the datalake";
    public static final String MODIFY_PROXY_CONFIG_INTERNAL = "Modify proxy config of stack";
    public static final String ROTATE_STACK_SECRETS = "Rotate stack secrets";

    public static final String COST = "Get cost calculation for Distrox clusters";
    public static final String CO2 = "Get CO2 cost calculation for Distro clusters";

    private DistroXOpDescription() {
    }
}
