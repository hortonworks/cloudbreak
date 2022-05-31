package com.sequenceiq.distrox.api.v1.distrox.doc;

public class DistroXOpDescription {
    public static final String GET_BY_ID = "retrieve stack by id";
    public static final String GET_STATUS_BY_NAME = "retrieve stack status by stack name";
    public static final String GET_STATUS_BY_CRN = "retrieve stack status by stack crn";
    public static final String PUT_BY_ID = "update stack by id";
    public static final String PUT_BY_NAME = "update stack by name";
    public static final String GET_BY_AMBARI_ADDRESS = "retrieve stack by ambari address";
    public static final String GET_STACK_CERT = "retrieves the TLS certificate used by the gateway";
    public static final String GET_ALL = "retrieve all stacks";
    public static final String LIST = "list stacks by environment crn";
    public static final String GET_BY_NAME = "get stack by name";
    public static final String GET_BY_CRN = "get stack by crn";
    public static final String GET_BY_CRN_INTERNAL = "get stack by crn (for internal user)";
    public static final String GET_BY_CRNS_INTERNAL = "get stack by crns (for internal user)";
    public static final String CREATE = "create stack";
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
    public static final String ROTATE_SALT_PASSWORD_BY_CRN = "rotates the salt password of stack by crn";
    public static final String RESTART_CLUSTER_BY_CRN = "restarts the cluster by crn";
    public static final String SCALE_BY_NAME = "scales the stack by name";
    public static final String SCALE_BY_CRN = "scales the stack by crn";
    public static final String REPAIR_CLUSTER_BY_NAME = "repairs the stack by name";
    public static final String REPAIR_CLUSTER_BY_CRN = "repairs the stack by crn";
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

    private DistroXOpDescription() {
    }
}
