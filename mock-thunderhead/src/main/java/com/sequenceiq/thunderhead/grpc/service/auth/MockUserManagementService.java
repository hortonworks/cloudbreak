package com.sequenceiq.thunderhead.grpc.service.auth;

import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_AUTO_JAVA_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_CERTIFICATE_AUTH;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE_LONG_POLLING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_DATABASE_SINGLE_SERVER_REJECT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_IMAGE_MARKETPLACE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_IMAGE_MARKETPLACE_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_UAE_CENTRAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_BASE_IMAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_VARIANT_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_ADD_DISK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_DELETE_DISK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_MULTIAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_RESIZE_DISK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_CM_TEMPLATE_SYNC;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_CO2_CALCULATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_CONFIGURE_ENCRYPTION_PROFILE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_COST_CALCULATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_FREEIPA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_GCP_MULTIAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_GCP_SECURE_BOOT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_PREFER_MINIFI_LOGGING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_SECRET_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_SUPPORTS_TLS_1_3_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_TLS_1_3;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_UPGRADE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_USE_DEV_TELEMETRY_YUM_REPO;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_VERTICAL_SCALE_HA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_XFS_FOR_EPHEMERAL_DISK_SUPPORTED;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CENTRAL_COMPUTE_MONITORING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_IDENTITY_MAPPING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AWS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_BULK_HOSTS_REMOVAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CONCLUSION_CHECKER_SEND_USER_EVENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_EXPERIMENTAL_SCALE_LIMITS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_FORCE_OS_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_LONG_TIMEOUT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_RESIZE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_DB_BACKUP_ENABLE_COMPRESSION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_RESIZE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_BACKUP_RESTORE_PERMISSION_CHECKS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_DISTROX_INSTANCE_TYPES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_RHEL9_IMAGES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPRESS_ONBOARDING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_REBUILD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_GCP_RAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_HYBRID_CLOUD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_LAKEHOUSE_OPTIMIZER_ENABLED;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MICRO_DUTY_SDX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MITIGATE_RELEASE_FAILURE_7218P1100;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_EXCEPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PREFER_RHEL9_IMAGES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PRIVATELINKS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RANGER_LDAP_USERSYNC;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS_SDX_INTEGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SDX_HBASE_CLOUD_STORAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SECURITY_ENFORCING_SELINUX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SKIP_ROLLING_UPGRADE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_TARGETED_UPSCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UNBOUND_ELIMINATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USERSYNC_SPLIT_FREEIPA_USER_RETRIEVAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USE_CM_SYNC_COMMAND_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_VM_DIAGNOSTICS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CLOUDERA_INTERNAL_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.COMPUTE_API_LIFTIE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.COMPUTE_API_LIFTIE_BETA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.COMPUTE_UI;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AWS_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AZURE_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_IMPALA_SCHEDULE_BASED_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_STOP_START_SCALING_FAILURE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATALAKE_HORIZONTAL_SCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.E2E_TEST_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.FMS_FREEIPA_BATCH_CALL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.LOCAL_DEV;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_DMP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_REAL_TIME_JOBS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_SAAS_PREMIUM;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_SAAS_TRIAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OJDBC_TOKEN_DH;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.PERSONAL_VIEW_CB_BY_RIGHT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.TARGETING_SUBNETS_FOR_ENDPOINT_ACCESS_GATEWAY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.UI_EDP_PROGRESS_BAR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.WORKLOAD_IAM_SYNC;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.WORKLOAD_IAM_USERSYNC_ROUTING;
import static java.util.Collections.newSetFromMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc.UserManagementImplBase;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKey;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyType;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccountType;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignResourceRoleResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AssignRoleResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AuthenticateResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CheckRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateMachineUserResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateWorkloadMachineUserResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteMachineUserResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Entitlement;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAssigneeAuthorizationInformationResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GrantEntitlementResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListAccessKeysResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListResourceAssigneesResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListRolesResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse.Builder;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.NotifyResourceDeletedResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Policy;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.PolicyDefinition;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.PolicyStatement;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ResourceAssignee;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ResourceAssignment;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RevokeEntitlementResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RightsCheck;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Role;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RoleAssignment;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SshPublicKey;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignResourceRoleResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.UnassignRoleResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.VerifyInteractiveUserSessionTokenResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadAdministrationGroup;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadPasswordPolicy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.UmsResourceRole;
import com.sequenceiq.cloudbreak.auth.altus.service.UmsRole;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.thunderhead.grpc.GrpcActorContext;
import com.sequenceiq.thunderhead.grpc.service.auth.roles.MockEnvironmentUserResourceRole;
import com.sequenceiq.thunderhead.model.AltusToken;
import com.sequenceiq.thunderhead.service.LoadResourcesForAccountIdService;
import com.sequenceiq.thunderhead.service.MockUmsService;
import com.sequenceiq.thunderhead.util.CrnHelper;
import com.sequenceiq.thunderhead.util.IniUtil;
import com.sequenceiq.thunderhead.util.JsonUtil;

import io.grpc.Status;
import io.grpc.internal.testing.StreamRecorder;
import io.grpc.stub.StreamObserver;

@Service
public class MockUserManagementService extends UserManagementImplBase {

    public static final String ACCOUNT_SUBDOMAIN = "xcu2-8y8x";

    @VisibleForTesting
    static final long PASSWORD_LIFETIME_IN_MILLISECONDS = 31449600000L;

    private static final Logger LOGGER = LoggerFactory.getLogger(MockUserManagementService.class);

    // copy static value from thunderhead repository, ApiAccessConfiguration.java class
    private static final String CLOUDERA_ADMINISTRATION_EXTERNAL_ACCOUNT_ID = "0018000000ik5epAAA:Cloudera-Administration";

    private static final String MOCK_CLOUDBREAKADMIN_ACCOUNT_ID = "cloudbreakadmin";

    private static final MacSigner SIGNATURE_VERIFIER = new MacSigner(MockUmsService.MAC_SIGNER_SECRET_KEY);

    private static final String ALTUS_ACCESS_KEY_ID = "altus_access_key_id";

    private static final String ALTUS_PRIVATE_KEY = "altus_private_key";

    private static final String CDP_ACCESS_KEY_ID = "cdp_access_key_id";

    private static final String CDP_PRIVATE_KEY = "cdp_private_key";

    private static final int MOCK_USER_COUNT = 10;

    private static final String ACCOUNT_ID_ALTUS = "altus";

    private static final long CREATION_DATE_MS = 1483228800000L;

    private static final String ALL_RIGHTS_AND_RESOURCES = "*";

    private static final String MOCK_RESOURCE = "mock_resource";

    private static final String SSH_PUBLIC_KEY_PATTERN = "^ssh-(rsa|ed25519|ecdsa)\\s+AAAA(B|C)3NzaC1.*(|\\n)";

    // See com.cloudera.thunderhead.service.common.entitlements.CdpEntitlements.CDP_CP_CUSTOM_DL_TEMPLATE
    // not used in CB, but used in CDP CLI, so we need this in mock for local development
    private static final String CDP_CP_CUSTOM_DL_TEMPLATE = "CDP_CM_ADMIN_CREDENTIALS";

    private static final String CRN_COMPONENT_SEPARATOR_REGEX = ":";

    private final Map<String, Set<String>> accountUsers = new ConcurrentHashMap<>();

    private final Set<String> grantedEntitlements = new ConcurrentSkipListSet<>();

    private final Set<String> revokedEntitlements = new ConcurrentSkipListSet<>();

    @Inject
    private JsonUtil jsonUtil;

    @Inject
    private IniUtil iniUtil;

    @Inject
    private MockCrnService mockCrnService;

    @Inject
    private MockGroupManagementService mockGroupManagementService;

    @Value("#{'${auth.config.dir:}/${auth.license.file:}'}")
    private String cmLicenseFilePath;

    @Value("#{'${auth.config.dir:}/${auth.databus.credential.tp.file:}'}")
    private String databusTpCredentialFile;

    @Value("#{'${auth.config.dir:}/${auth.databus.credential.fluent.file:}'}")
    private String databusFluentCredentialFile;

    @Value("#{'${auth.config.dir:}/${auth.mock.sshpublickey.file:}'}")
    private String sshPublicKeyFilePath;

    @Value("${auth.databus.credential.tp.profile:default}")
    private String databusTpCredentialProfile;

    @Value("${auth.databus.credential.fluent.profile:default}")
    private String databusFluentCredentialProfile;

    @Value("${auth.mock.baseimage.enable}")
    private boolean enableBaseImages;

    @Value("${auth.mock.freeipa.rebuild.enable}")
    private boolean enableFreeIpaRebuild;

    @Value("${auth.mock.freeipa.loadbalancer.enable}")
    private boolean enableFreeIpaLoadBalancer;

    @Value("${auth.mock.cloudstoragevalidation.enable.global}")
    private boolean enableCloudStorageValidation;

    @Value("${auth.mock.cloudstoragevalidation.enable.aws}")
    private boolean enableAwsCloudStorageValidation;

    @Value("${auth.mock.cloudstoragevalidation.enable.azure}")
    private boolean enableAzureCloudStorageValidation;

    @Value("${auth.mock.cloudstoragevalidation.enable.gcp}")
    private boolean enableGcpCloudStorageValidation;

    @Value("${auth.mock.microdutysdx.enable}")
    private boolean microDutySdxEnabled;

    @Value("${auth.mock.ha.repair.enable}")
    private boolean haRepairEnabled;

    @Value("${auth.mock.azure.single.resourcegroup.dedicated.storage.account.enable}")
    private boolean enableAzureSingleResourceGroupDedicatedStorageAccount;

    @Value("${auth.mock.azure.marketplace.images.enable}")
    private boolean enableAzureMarketplaceImages;

    @Value("${auth.mock.azure.marketplace.images.only.enable}")
    private boolean enableAzureMarketplaceImagesOnly;

    @Value("${auth.mock.cloudidentitymapping.enable}")
    private boolean enableCloudIdentityMapping;

    @Value("${auth.mock.upgrade.internalrepo.enable}")
    private boolean enableInternalRepositoryForUpgrade;

    @Value("${auth.mock.hbase.cloudstorage.enable}")
    private boolean enableHbaseCloudStorage;

    @Value("${auth.mock.differentdatahubversionthandatalake.enabled}")
    private boolean enableDifferentDataHubVersionThanDataLake;

    @Value("${auth.mock.datalake.loadbalancer.enable}")
    private boolean datalakeLoadBalancerEnabled;

    @Value("${auth.mock.environment.experience.deletion.enable}")
    private boolean enableExperienceDeletionByEnvironment;

    @Value("${auth.mock.endpointgateway.enable.azure}")
    private boolean azureEndpointGatewayEnabled;

    @Value("${auth.mock.endpointgateway.enable.gcp}")
    private boolean gcpEndpointGatewayEnabled;

    @Value("${auth.mock.datalake.backup.on.resize.enable}")
    private boolean datalakeBackupOnResize;

    @Value("${auth.mock.datalake.backup.restore.permission.checks.enabled}")
    private boolean datalakeBackupRestorePermissionChecks;

    @Value("${auth.mock.datalake.recovery.resize.enable}")
    private boolean datalakeResizeRecovery;

    @Value("${auth.mock.datalake.light.to.medium.migration.enable}")
    private boolean datalakeLightToMediumMigration;

    @Value("${auth.mock.cm.sync.command.poller.enable}")
    private boolean cmSyncCommandPollerEnabled;

    @Value("${auth.mock.conclusion.checker.send.user.event.enable:true}")
    private boolean conclusionCheckerSendUserEvent;

    @Value("${auth.mock.diagnostics.vm.enable}")
    private boolean diagnosticsEnabled;

    @Value("${auth.mock.unbound.elimination.enable}")
    private boolean enableUnboundElimination;

    @Value("${auth.mock.targeted.upscale.enable}")
    private boolean enableTargetedUpscale;

    @Value("${auth.mock.aws.native.variant.migration.enable}")
    private boolean enableAwsVariantMigration;

    @Value("${auth.mock.e2e.test.only.enable}")
    private boolean enableE2ETestOnly;

    @Value("${auth.mock.upgrade.skip.rolling.upgrade.validation}")
    private boolean skipRollingUpgradeValidationEnabled;

    @Value("${auth.mock.workloadiam.sync.enable}")
    private boolean enableWorkloadIamSync;

    @Value("${auth.mock.workloadiam.sync.routing.enable}")
    private boolean enableWorkloadIamSyncRouting;

    @Value("${auth.mock.postgres.upgrade.exception.enable}")
    private boolean enablePostgresUpgradeException;

    @Value("${auth.mock.postgres.upgrade.skip.attached.datahubs.check.enable}")
    private boolean skipPostgresUpgradeAttachedDatahubsCheck;

    @Value("${auth.mock.upgrade.skip.attached.datahubs.check.enable}")
    private boolean skipUpgradeAttachedDatahubsCheck;

    @Value("${auth.mock.postgres.upgrade.skip.service.stop.enable}")
    private boolean skipPostgresUpgradeServicesAndCmStop;

    @Value("${auth.mock.datalake.backup.compression.enable}")
    private boolean dlBackupCompressionEnable;

    @Value("${auth.mock.tlsv13.enable}")
    private boolean tlsv13Enabled;

    private String cbLicense;

    private AltusCredential telemetryPublisherCredential;

    private AltusCredential fluentCredential;

    @Value("${auth.mock.event-generation.expiration-minutes:10}")
    private long eventGenerationExpirationMinutes;

    private LoadingCache<String, GetEventGenerationIdsResponse> eventGenerationIdsCache;

    private GetActorWorkloadCredentialsResponse actorWorkloadCredentialsResponse;

    private WorkloadPasswordPolicy workloadPasswordPolicy;

    private Optional<SshPublicKey> sshPublicKey;

    @Value("${auth.mock.user.sync.credentials.update.optimization.enable}")
    private boolean userSyncCredentialsUpdateOptimizationEnabled;

    @Value("${auth.mock.endpointgateway.skip.validation}")
    private boolean endpointGatewaySkipValidation;

    @Value("${auth.mock.freeipa.batch.call.enable}")
    private boolean enableFmsFreeipaBatchCall;

    @Value("${auth.mock.ui.edp.progress.bar.enable}")
    private boolean edpProgressBarEnabled;

    @Value("${auth.mock.datahub.instancetypes.enable}")
    private boolean enableDistroxInstanceTypes;

    @Value("${auth.mock.saas.enable}")
    private boolean enableSaas;

    @Value("${auth.mock.saas.sdx.integration.enable}")
    private boolean enableSdxSaasIntegration;

    @Value("${auth.mock.compute.monitoring.enable}")
    private boolean enableComputeMonitoring;

    @Value("${auth.mock.user.sync.group-size.enforce-limit.enable}")
    private boolean enableUsersyncEnforceGroupMemberLimit;

    @Value("${auth.mock.user.sync.split-freeipa-user-retrieval.enable}")
    private boolean enableUsersyncSplitFreeIPAUserRetrieval;

    @Value("${auth.mock.datalake.long.time.backup.enable:false}")
    private boolean enableLongTimeDatalakeBackup;

    @Value("${auth.mock.targeting.subnets.for.endpoint.access.gateway.enable}")
    private boolean enableTargetingSubnetsForEndpointAccessGateway;

    @Value("${auth.mock.azure.certificate.auth.enable}")
    private boolean azureCertificateAuth;

    @Value("${auth.mock.cost.calculation.enable}")
    private boolean costCalculationEnabled;

    @Value("${auth.mock.co2.calculation.enable}")
    private boolean co2CalculationEnabled;

    @Value("${auth.mock.enforce.aws.native.single.az.enabled}")
    private boolean enforceAwsNativeForSingleAzEnabled;

    @Value("${auth.mock.secret.encryption.enabled}")
    private boolean secretEncryptionEnabled;

    @Value("${auth.mock.cm.observability.saas.premium}")
    private boolean cmObservabilitySaasPremium;

    @Value("${auth.mock.cm.observability.saas.trial}")
    private boolean cmObservabilitySaasTrial;

    @Value("${auth.mock.cm.observability.realtimejobs}")
    private boolean cmObservabilityRealtimeJobs;

    @Value("${auth.mock.cm.observability.dmp}")
    private boolean cmObservabilityDmp;

    @Value("${auth.mock.compute.ui.enabled}")
    private boolean computeUiEnabled;

    @Value("${auth.mock.ranger.ldap.usersync}")
    private boolean rangerLdapUsersyncEnabled;

    @Value("${auth.mock.aws.arm.datahub}")
    private boolean awsArmDataHubEnabled;

    @Value("${auth.mock.aws.arm.datalake}")
    private boolean awsArmDataLakeEnabled;

    @Value("${auth.mock.azure.database.flexibleserver.upgrade.longpolling.enabled}")
    private boolean azureFlexibleUpgradeLongPollingEnabled;

    @Value("${auth.mock.azure.database.singleserver.reject.enabled}")
    private boolean azureSingleServerRejectEnabled;

    @Value("${auth.mock.gcp.secureboot.enabled}")
    private boolean gcpSecureBootEnabled;

    @Value("${auth.mock.datahub.force.os.upgrade}")
    private boolean datahubForceOsUpgradeEnabled;

    @Value("${auth.mock.dev.telemetry.yum.repo.enabled}")
    private boolean devTelemetryYumRepoEnabled;

    @Value("${auth.mock.lakehouse.optimizer.enabled}")
    private boolean lakehouseOptimizerEnabled;

    @Value("${auth.mock.ephemeral.xfs.support.enabled}")
    private boolean ephemeralXfsSupportEnabled;

    @Value("${auth.mock.configure.encryption.profile.enabled}")
    private boolean configureEncryptionProfileEnabled;

    @Value("${auth.mock.zookeeper.to.kraft.migration.enabled}")
    private boolean zookeeperToKRaftMigrationEnabled;

    @Value("${auth.mock.mitigate.release.failure.7218P1100.enabled}")
    private boolean mitigateReleaseFailure7218P1100Enabled;

    @Value("${auth.mock.tlsv13.only.enabled}")
    private boolean tlsv13OnlyEnabled;

    @Value("${auth.mock.rhel9.enabled}")
    private boolean rhel9Enabled;

    @Value("${auth.mock.rhel9.preferred}")
    private boolean rhel9Preferred;

    @Value("${auth.mock.verticalscale.ha.enabled}")
    private boolean verticalScaleHaEnabled;

    @Value("${auth.mock.datalake.upgrade.recovery.enabled}")
    private boolean datalakeUpgradeRecoveryEnabled;

    @Value("${auth.mock.cloudprivatelinks.enabled}")
    private boolean cloudprivatelinksEnabled;

    @Value("${auth.mock.prefer.minifi.logging.enabled}")
    private boolean preferMinifiLoggingEnabled;

    @Value("${auth.mock.allow.auto.java.upgrade}")
    private boolean allowAutoJavaUpgrade;

    @Value("${auth.mock.personal.view.cb.by.right.enabled}")
    private boolean personalViewCbByRightEnabled;

    @Inject
    private MockEnvironmentUserResourceRole mockEnvironmentUserResourceRole;

    @Inject
    private Set<LoadResourcesForAccountIdService> loadResourcesForAccountIdServices;

    @PostConstruct
    public void init() {
        cbLicense = getLicense();
        telemetryPublisherCredential = getAltusCredential(databusTpCredentialFile, databusTpCredentialProfile);
        fluentCredential = getAltusCredential(databusFluentCredentialFile, databusFluentCredentialProfile);
        eventGenerationIdsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(eventGenerationExpirationMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public GetEventGenerationIdsResponse load(String key) {
                        GetEventGenerationIdsResponse.Builder respBuilder = GetEventGenerationIdsResponse.newBuilder();
                        respBuilder.setLastRoleAssignmentEventId(UUID.randomUUID().toString());
                        respBuilder.setLastResourceRoleAssignmentEventId(UUID.randomUUID().toString());
                        respBuilder.setLastGroupMembershipChangedEventId(UUID.randomUUID().toString());
                        respBuilder.setLastActorDeletedEventId(UUID.randomUUID().toString());
                        respBuilder.setLastActorWorkloadCredentialsChangedEventId(UUID.randomUUID().toString());
                        return respBuilder.build();
                    }
                });

        initializeActorWorkloadCredentials();
        initializeWorkloadPasswordPolicy();

        listEntitlements();
    }

    @VisibleForTesting
    void initializeActorWorkloadCredentials() {
        GetActorWorkloadCredentialsResponse.Builder builder = GetActorWorkloadCredentialsResponse.newBuilder();
        try {
            JsonFormat.parser().merge(Resources.toString(
                    Resources.getResource("mock-responses/ums/get-workload-credentials.json"), StandardCharsets.UTF_8), builder);
            actorWorkloadCredentialsResponse = builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sshPublicKey = getSshPublicKey();
    }

    @VisibleForTesting
    void initializeWorkloadPasswordPolicy() {
        WorkloadPasswordPolicy.Builder builder = WorkloadPasswordPolicy.newBuilder();
        builder.setWorkloadPasswordMaxLifetime(PASSWORD_LIFETIME_IN_MILLISECONDS);
        workloadPasswordPolicy = builder.build();
    }

    private void listEntitlements() {
        GetAccountRequest req = GetAccountRequest.getDefaultInstance();
        StreamRecorder<GetAccountResponse> observer = StreamRecorder.create();
        getAccount(req, observer);
        GetAccountResponse res = observer.getValues().get(0);
        Account account = res.getAccount();
        List<String> entitlements = account.getEntitlementsList().stream()
                .map(Entitlement::getEntitlementName)
                .toList();
        LOGGER.info("Granted entitlements: {}", entitlements);
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        LOGGER.info("Get user: {}", request.getUserIdOrCrn());
        String userIdOrCrn = request.getUserIdOrCrn();
        String[] splitCrn = userIdOrCrn.split(CRN_COMPONENT_SEPARATOR_REGEX);
        String userName = splitCrn[6];
        String accountId = splitCrn[4];
        addUserAndCreateAccountIfNeeded(accountId, userName);
        responseObserver.onNext(
                GetUserResponse.newBuilder()
                        .setUser(createUser(accountId, userName))
                        .build());
        responseObserver.onCompleted();
    }

    private void addUserAndCreateAccountIfNeeded(String accountId, String userName) {
        accountUsers.computeIfAbsent(accountId, key -> {
            loadResourcesForAccountIdServices.forEach(service -> {
                try {
                    LOGGER.debug("Loading resources with service {} for account id {}", service.getClass(), accountId);
                    service.load(accountId);
                } catch (Exception e) {
                    LOGGER.error("Failed to load resources with service {} for account id {}", service.getClass(), accountId);
                }
            });
            return newSetFromMap(new ConcurrentHashMap<>());
        }).add(userName);
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        LOGGER.info("List users for account: {}", request.getAccountId());
        Builder userBuilder = ListUsersResponse.newBuilder();
        if (request.getUserIdOrCrnCount() == 0) {
            if (isNotEmpty(request.getAccountId())) {
                ofNullable(accountUsers.get(request.getAccountId())).orElse(Set.of()).stream()
                        .map(userName -> createUser(request.getAccountId(), userName))
                        .forEach(userBuilder::addUser);
                for (int i = 0; i < MOCK_USER_COUNT; i++) {
                    User user = createUser(request.getAccountId(), "fakeMockUser" + i);
                    userBuilder.addUser(user);
                }

                mockEnvironmentUserResourceRole.getUserNames().forEach(user -> userBuilder.addUser(createUser(request.getAccountId(), user)));
            }
            responseObserver.onNext(userBuilder.build());
        } else {
            String userIdOrCrn = request.getUserIdOrCrn(0);
            String[] splitCrn = userIdOrCrn.split(CRN_COMPONENT_SEPARATOR_REGEX);
            String userName = splitCrn[6];
            String accountId = splitCrn[4];
            responseObserver.onNext(
                    userBuilder
                            .addUser(createUser(accountId, userName))
                            .build());
        }
        responseObserver.onCompleted();
    }

    private GetRightsResponse buildGetRightsResponse(String accountId) {
        List<Group> workloadGroups = List.copyOf(mockGroupManagementService.getOrCreateWorkloadGroups(accountId));
        List<Group> userGroups = List.copyOf(mockGroupManagementService.getOrCreateUserGroups(accountId));
        PolicyStatement policyStatement = PolicyStatement.newBuilder()
                .addRight(ALL_RIGHTS_AND_RESOURCES)
                .addResource(ALL_RIGHTS_AND_RESOURCES)
                .build();
        PolicyDefinition policyDefinition = PolicyDefinition.newBuilder().addStatement(policyStatement).build();
        Policy powerUserPolicy = Policy.newBuilder()
                .setCrn(mockCrnService.createCrn(ACCOUNT_ID_ALTUS, CrnResourceDescriptor.POLICY, "PowerUserPolicy").toString())
                .setCreationDateMs(CREATION_DATE_MS)
                .setPolicyDefinition(policyDefinition)
                .build();
        Role powerUserRole = Role.newBuilder()
                .setCrn("crn:altus:iam:us-west-1:altus:role:PowerUser")
                .setCreationDateMs(CREATION_DATE_MS)
                .addPolicy(powerUserPolicy)
                .build();
        RoleAssignment roleAssignment = RoleAssignment.newBuilder().setRole(powerUserRole).build();
        GetRightsResponse.Builder rightsBuilder = GetRightsResponse.newBuilder()
                .addRoleAssignment(roleAssignment);
        workloadGroups.forEach(group -> rightsBuilder.addGroupCrn(group.getCrn()));
        userGroups.forEach(group -> rightsBuilder.addGroupCrn(group.getCrn()));
        return rightsBuilder.build();
    }

    @Override
    public void getRights(GetRightsRequest request, StreamObserver<GetRightsResponse> responseObserver) {
        LOGGER.info("Get rights for: {}", request.getActorCrn());
        String actorCrn = request.getActorCrn();
        String accountId = Crn.fromString(actorCrn).getAccountId();
        responseObserver.onNext(buildGetRightsResponse(accountId));
        responseObserver.onCompleted();
    }

    @Override
    public void listGroupsForMember(ListGroupsForMemberRequest request, StreamObserver<ListGroupsForMemberResponse> responseObserver) {
        String accountId = request.getMember().getAccountId();
        LOGGER.info("List groups for member: {}", accountId);
        ListGroupsForMemberResponse.Builder responseBuilder = ListGroupsForMemberResponse.newBuilder();
        List<Group> userGroups = List.copyOf(mockGroupManagementService.getOrCreateUserGroups(accountId));
        userGroups.forEach(g -> responseBuilder.addGroupCrn(g.getCrn()));
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listWorkloadAdministrationGroups(ListWorkloadAdministrationGroupsRequest request,
            StreamObserver<ListWorkloadAdministrationGroupsResponse> responseObserver) {
        mockCrnService.ensureInternalActor();
        String accountId = request.getAccountId();
        LOGGER.info("List workload administration groups: {}", accountId);

        ListWorkloadAdministrationGroupsResponse.Builder responseBuilder = ListWorkloadAdministrationGroupsResponse.newBuilder();
        for (UmsVirtualGroupRight right : UmsVirtualGroupRight.values()) {
            Group group = mockGroupManagementService.createGroup(accountId, mockGroupManagementService.generateWorkloadGroupName(right.getRight()));
            responseBuilder.addWorkloadAdministrationGroup(
                    WorkloadAdministrationGroup.newBuilder()
                            .setWorkloadAdministrationGroupName(group.getGroupName())
                            .setRightName(right.getRight())
                            .setResource(MOCK_RESOURCE)
                            .build()
            );
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listServicePrincipalCloudIdentities(ListServicePrincipalCloudIdentitiesRequest request,
            StreamObserver<ListServicePrincipalCloudIdentitiesResponse> responseObserver) {
        mockCrnService.ensureInternalActor();
        LOGGER.info("List service principal cloud identities for account: {}, environment: {}", request.getAccountId(), request.getEnvironmentCrn());
        ListServicePrincipalCloudIdentitiesResponse.Builder responseBuilder = ListServicePrincipalCloudIdentitiesResponse.newBuilder();
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listWorkloadAdministrationGroupsForMember(ListWorkloadAdministrationGroupsForMemberRequest request,
            StreamObserver<ListWorkloadAdministrationGroupsForMemberResponse> responseObserver) {
        String memberCrn = request.getMemberCrn();
        String accountId = Crn.fromString(memberCrn).getAccountId();
        LOGGER.info("List workload administration groups for member: {}, accountId: {}", memberCrn, accountId);
        Set<String> groups;
        if (mockEnvironmentUserResourceRole.hasMatchingUser(memberCrn)) {
            groups = mockEnvironmentUserResourceRole.getWorkloadAdministrationGroupNames();
        } else {
            groups = mockGroupManagementService.getOrCreateWorkloadGroups(accountId).stream().map(Group::getGroupName).collect(Collectors.toSet());
        }
        LOGGER.info("List workload administration groups for member: {}, accountId: {}, workloadAdministratorGroups: {}", memberCrn, accountId, groups);
        ListWorkloadAdministrationGroupsForMemberResponse.Builder responseBuilder = ListWorkloadAdministrationGroupsForMemberResponse.newBuilder();
        responseBuilder.addAllWorkloadAdministrationGroupName(groups);
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private User createUser(String accountId, String userName) {
        String userCrn = mockCrnService.createCrn(accountId, CrnResourceDescriptor.USER, userName).toString();
        return User.newBuilder()
                .setUserId(UUID.nameUUIDFromBytes((accountId + '#' + userName).getBytes()).toString())
                .setCrn(userCrn)
                .setEmail(userName.contains("@") ? userName : userName + "@ums.mock")
                .setWorkloadUsername(sanitizeWorkloadUsername(userName))
                .addCloudIdentities(UserManagementProto.CloudIdentity.newBuilder()
                        .setCloudIdentityName(UserManagementProto.CloudIdentityName
                                .newBuilder()
                                .setAzureCloudIdentityName(UserManagementProto.AzureCloudIdentityName
                                        .newBuilder()
                                        .setObjectId(userCrn + "-aoid")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void listGroups(ListGroupsRequest request, StreamObserver<ListGroupsResponse> responseObserver) {
        mockGroupManagementService.listGroups(request, responseObserver);
    }

    @Override
    public void listMachineUsers(ListMachineUsersRequest request, StreamObserver<ListMachineUsersResponse> responseObserver) {
        LOGGER.info("List machine users for: {}", request.getAccountId());
        if (request.getMachineUserNameOrCrnCount() == 0) {
            responseObserver.onNext(ListMachineUsersResponse.newBuilder().build());
        } else {
            String machineUserIdOrCrn = request.getMachineUserNameOrCrn(0);
            String[] splitCrn = machineUserIdOrCrn.split(CRN_COMPONENT_SEPARATOR_REGEX);
            String userName;
            String accountId = request.getAccountId();
            String crnString;
            if (splitCrn.length > 1) {
                userName = splitCrn[6];
                crnString = machineUserIdOrCrn;
            } else {
                userName = machineUserIdOrCrn;
                Crn crn = mockCrnService.createCrn(accountId, CrnResourceDescriptor.MACHINE_USER, userName);
                crnString = crn.toString();
            }
            responseObserver.onNext(
                    ListMachineUsersResponse.newBuilder()
                            .addMachineUser(MachineUser.newBuilder()
                                    .setMachineUserId(UUID.nameUUIDFromBytes((accountId + '#' + userName).getBytes()).toString())
                                    .setCrn(crnString)
                                    .setWorkloadUsername(sanitizeWorkloadUsername(userName))
                                    .build())
                            .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
        String accountId = request.getAccountId();
        mockCrnService.ensureProperAccountIdUsage(accountId);
        LOGGER.info("Get account: {}", accountId);
        Account.Builder builder = Account.newBuilder();
        if (enableBaseImages) {
            builder.addEntitlements(createEntitlement(CDP_BASE_IMAGE));
        }
        if (enableFreeIpaRebuild) {
            builder.addEntitlements(createEntitlement(CDP_FREEIPA_REBUILD));
        }
        if (enableFreeIpaLoadBalancer) {
            builder.addEntitlements(createEntitlement(CDP_FREEIPA_LOAD_BALANCER));
        }
        if (enableCloudStorageValidation) {
            builder.addEntitlements(createEntitlement(CDP_CLOUD_STORAGE_VALIDATION));
        }
        if (enableAwsCloudStorageValidation) {
            builder.addEntitlements(createEntitlement(CDP_CLOUD_STORAGE_VALIDATION_AWS));
        }
        if (enableAzureCloudStorageValidation) {
            builder.addEntitlements(createEntitlement(CDP_CLOUD_STORAGE_VALIDATION_AZURE));
        }
        if (enableGcpCloudStorageValidation) {
            builder.addEntitlements(createEntitlement(CDP_CLOUD_STORAGE_VALIDATION_GCP));
        }
        if (microDutySdxEnabled) {
            builder.addEntitlements(createEntitlement(CDP_MICRO_DUTY_SDX));
        }
        if (haRepairEnabled) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_HA_REPAIR));
        }
        if (enableAzureSingleResourceGroupDedicatedStorageAccount) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT));
        }
        if (enableAzureMarketplaceImages) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_IMAGE_MARKETPLACE));
        }
        if (enableAzureMarketplaceImagesOnly) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_IMAGE_MARKETPLACE_ONLY));
        }
        if (enableCloudIdentityMapping) {
            builder.addEntitlements(createEntitlement(CDP_CLOUD_IDENTITY_MAPPING));
        }
        if (enableInternalRepositoryForUpgrade) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE));
        }
        if (enableHbaseCloudStorage) {
            builder.addEntitlements(createEntitlement(CDP_SDX_HBASE_CLOUD_STORAGE));
        }
        if (enableDifferentDataHubVersionThanDataLake) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE));
        }
        if (datalakeLoadBalancerEnabled) {
            builder.addEntitlements(createEntitlement(CDP_DATA_LAKE_LOAD_BALANCER));
        }
        if (enableExperienceDeletionByEnvironment) {
            builder.addEntitlements(createEntitlement(CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT));
        }
        if (azureEndpointGatewayEnabled) {
            builder.addEntitlements(createEntitlement(CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE));
        }
        if (gcpEndpointGatewayEnabled) {
            builder.addEntitlements(createEntitlement(CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP));
        }
        if (datalakeBackupOnResize) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_BACKUP_ON_RESIZE));
        }
        if (datalakeBackupRestorePermissionChecks) {
            builder.addEntitlements(createEntitlement(CDP_DATA_LAKE_BACKUP_RESTORE_PERMISSION_CHECKS));
        }
        if (datalakeResizeRecovery) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_RESIZE_RECOVERY));
        }
        if (datalakeLightToMediumMigration) {
            builder.addEntitlements(createEntitlement(DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION));
        }
        if (cmSyncCommandPollerEnabled) {
            builder.addEntitlements(createEntitlement(CDP_USE_CM_SYNC_COMMAND_POLLER));
        }
        if (conclusionCheckerSendUserEvent) {
            builder.addEntitlements(createEntitlement(CDP_CONCLUSION_CHECKER_SEND_USER_EVENT));
        }
        if (userSyncCredentialsUpdateOptimizationEnabled) {
            builder.addEntitlements(createEntitlement(CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION));
        }
        if (endpointGatewaySkipValidation) {
            builder.addEntitlements(createEntitlement(CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION));
        }
        if (diagnosticsEnabled) {
            builder.addEntitlements(createEntitlement(CDP_VM_DIAGNOSTICS));
        }
        if (enableSaas || accountId.contains("CDP_SAAS")) {
            builder.addEntitlements(createEntitlement(CDP_SAAS));
        }
        if (enableFmsFreeipaBatchCall) {
            builder.addEntitlements(createEntitlement(FMS_FREEIPA_BATCH_CALL));
        }
        if (enableAwsVariantMigration) {
            builder.addEntitlements(createEntitlement(CDP_CB_AWS_VARIANT_MIGRATION));
        }
        if (edpProgressBarEnabled) {
            builder.addEntitlements(createEntitlement(UI_EDP_PROGRESS_BAR));
        }
        if (enableDistroxInstanceTypes) {
            builder.addEntitlements(createEntitlement(CDP_ENABLE_DISTROX_INSTANCE_TYPES));
        }
        if (enableUnboundElimination) {
            builder.addEntitlements(createEntitlement(CDP_UNBOUND_ELIMINATION));
        }
        if (enableTargetedUpscale) {
            builder.addEntitlements(createEntitlement(CDP_TARGETED_UPSCALE));
        }
        if (enableE2ETestOnly) {
            builder.addEntitlements(createEntitlement(E2E_TEST_ONLY));
        }
        if (skipRollingUpgradeValidationEnabled) {
            builder.addEntitlements(createEntitlement(CDP_SKIP_ROLLING_UPGRADE_VALIDATION));
        }
        if (enableWorkloadIamSync) {
            builder.addEntitlements(createEntitlement(WORKLOAD_IAM_SYNC));
        }
        if (enableWorkloadIamSyncRouting) {
            builder.addEntitlements(createEntitlement(WORKLOAD_IAM_USERSYNC_ROUTING));
        }
        if (enableSdxSaasIntegration) {
            builder.addEntitlements(createEntitlement(CDP_SAAS_SDX_INTEGRATION));
        }
        if ((enableComputeMonitoring || grantedEntitlements.contains(CDP_CENTRAL_COMPUTE_MONITORING.name()))
                && !revokedEntitlements.contains(CDP_CENTRAL_COMPUTE_MONITORING.name())) {
            builder.addEntitlements(createEntitlement(CDP_CENTRAL_COMPUTE_MONITORING));
        }
        if (enableUsersyncEnforceGroupMemberLimit) {
            builder.addEntitlements(createEntitlement(CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT));
        }
        if (enablePostgresUpgradeException) {
            builder.addEntitlements(createEntitlement(CDP_POSTGRES_UPGRADE_EXCEPTION));
        }
        if (enableUsersyncSplitFreeIPAUserRetrieval) {
            builder.addEntitlements(createEntitlement(CDP_USERSYNC_SPLIT_FREEIPA_USER_RETRIEVAL));
        }
        if (enableLongTimeDatalakeBackup) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_BACKUP_LONG_TIMEOUT));
        }
        if (skipPostgresUpgradeAttachedDatahubsCheck) {
            builder.addEntitlements(createEntitlement(CDP_POSTGRES_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK));
        }
        if (skipUpgradeAttachedDatahubsCheck) {
            builder.addEntitlements(createEntitlement(CDP_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK));
        }
        if (skipPostgresUpgradeServicesAndCmStop) {
            builder.addEntitlements(createEntitlement(CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP));
        }
        if (enableTargetingSubnetsForEndpointAccessGateway) {
            builder.addEntitlements(createEntitlement(TARGETING_SUBNETS_FOR_ENDPOINT_ACCESS_GATEWAY));
        }
        if (azureCertificateAuth) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_CERTIFICATE_AUTH));
        }
        if (costCalculationEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_COST_CALCULATION));
        }
        if (co2CalculationEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_CO2_CALCULATION));
        }
        if (enforceAwsNativeForSingleAzEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_FREEIPA));
            builder.addEntitlements(createEntitlement(CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATALAKE));
            builder.addEntitlements(createEntitlement(CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATAHUB));
        }
        if (dlBackupCompressionEnable) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_DB_BACKUP_ENABLE_COMPRESSION));
        }
        if (secretEncryptionEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_SECRET_ENCRYPTION));
        }
        if (cmObservabilitySaasPremium) {
            builder.addEntitlements(createEntitlement(OBSERVABILITY_SAAS_PREMIUM));
        }
        if (cmObservabilitySaasTrial) {
            builder.addEntitlements(createEntitlement(OBSERVABILITY_SAAS_TRIAL));
        }
        if (cmObservabilityRealtimeJobs) {
            builder.addEntitlements(createEntitlement(OBSERVABILITY_REAL_TIME_JOBS));
        }
        if (cmObservabilityDmp) {
            builder.addEntitlements(createEntitlement(OBSERVABILITY_DMP));
        }
        if (computeUiEnabled) {
            builder.addEntitlements(createEntitlement(COMPUTE_UI));
            builder.addEntitlements(createEntitlement(COMPUTE_API_LIFTIE));
            builder.addEntitlements(createEntitlement(COMPUTE_API_LIFTIE_BETA));
        }
        if (rangerLdapUsersyncEnabled) {
            builder.addEntitlements(createEntitlement(CDP_RANGER_LDAP_USERSYNC));
        }
        if (azureFlexibleUpgradeLongPollingEnabled) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE_LONG_POLLING));
        }
        if (azureSingleServerRejectEnabled) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_DATABASE_SINGLE_SERVER_REJECT));
        }
        if (gcpSecureBootEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_GCP_SECURE_BOOT));
        }
        if (datahubForceOsUpgradeEnabled) {
            builder.addEntitlements(createEntitlement(CDP_DATAHUB_FORCE_OS_UPGRADE));
        }
        if (tlsv13Enabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_TLS_1_3));
        }
        if (devTelemetryYumRepoEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_USE_DEV_TELEMETRY_YUM_REPO));
        }
        if (lakehouseOptimizerEnabled) {
            builder.addEntitlements(createEntitlement(CDP_LAKEHOUSE_OPTIMIZER_ENABLED));
        }
        if (ephemeralXfsSupportEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_XFS_FOR_EPHEMERAL_DISK_SUPPORTED));
        }
        if (configureEncryptionProfileEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_CONFIGURE_ENCRYPTION_PROFILE));
        }
        if (zookeeperToKRaftMigrationEnabled) {
            builder.addEntitlements(createEntitlement(CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION));
        }
        if (mitigateReleaseFailure7218P1100Enabled) {
            builder.addEntitlements(createEntitlement(CDP_MITIGATE_RELEASE_FAILURE_7218P1100));
        }
        if (tlsv13OnlyEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_SUPPORTS_TLS_1_3_ONLY));
        }
        if (rhel9Enabled) {
            builder.addEntitlements(createEntitlement(CDP_ENABLE_RHEL9_IMAGES));
        }
        if (rhel9Preferred) {
            builder.addEntitlements(createEntitlement(CDP_PREFER_RHEL9_IMAGES));
        }
        if (verticalScaleHaEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_VERTICAL_SCALE_HA));
        }
        if (datalakeUpgradeRecoveryEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_UPGRADE_RECOVERY));
        }
        if (cloudprivatelinksEnabled) {
            builder.addEntitlements(createEntitlement(CDP_PRIVATELINKS));
        }
        if (preferMinifiLoggingEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CB_PREFER_MINIFI_LOGGING));
        }
        if (allowAutoJavaUpgrade) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_AUTO_JAVA_UPGRADE));
        }
        if (personalViewCbByRightEnabled) {
            builder.addEntitlements(createEntitlement(PERSONAL_VIEW_CB_BY_RIGHT));
        }

        responseObserver.onNext(
                GetAccountResponse.newBuilder()
                        .setAccount(builder
                                .setClouderaManagerLicenseKey(cbLicense)
                                .setWorkloadSubdomain(ACCOUNT_SUBDOMAIN)
                                .addEntitlements(createEntitlement(CLOUDERA_INTERNAL_ACCOUNT))
                                .addEntitlements(createEntitlement(CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED))
                                .addEntitlements(createEntitlement(CDP_AZURE_UAE_CENTRAL))
                                .addEntitlements(createEntitlement(DATAHUB_IMPALA_SCHEDULE_BASED_SCALING))
                                .addEntitlements(createEntitlement(DATAHUB_GCP_AUTOSCALING))
                                .addEntitlements(createEntitlement(DATAHUB_AWS_STOP_START_SCALING))
                                .addEntitlements(createEntitlement(DATAHUB_AZURE_STOP_START_SCALING))
                                .addEntitlements(createEntitlement(DATAHUB_GCP_STOP_START_SCALING))
                                .addEntitlements(createEntitlement(DATAHUB_STOP_START_SCALING_FAILURE_RECOVERY))
                                .addEntitlements(createEntitlement(LOCAL_DEV))
                                .addEntitlements(createEntitlement(CDP_CP_CUSTOM_DL_TEMPLATE))
                                .addEntitlements(createEntitlement(OJDBC_TOKEN_DH))
                                .addEntitlements(createEntitlement(CDP_CM_BULK_HOSTS_REMOVAL))
                                .addEntitlements(createEntitlement(CDP_DATAHUB_EXPERIMENTAL_SCALE_LIMITS))
                                .addEntitlements(createEntitlement(CDP_GCP_RAZ))
                                .addEntitlements(createEntitlement(CDP_CB_AZURE_MULTIAZ))
                                .addEntitlements(createEntitlement(CDP_CB_GCP_MULTIAZ))
                                .addEntitlements(createEntitlement(CDP_CB_AZURE_RESIZE_DISK))
                                .addEntitlements(createEntitlement(CDP_CB_AZURE_DELETE_DISK))
                                .addEntitlements(createEntitlement(CDP_CB_AZURE_ADD_DISK))
                                .addEntitlements(createEntitlement(DATALAKE_HORIZONTAL_SCALE))
                                .addEntitlements(createEntitlement(CDP_EXPRESS_ONBOARDING))
                                .addEntitlements(createEntitlement(CDP_HYBRID_CLOUD))
                                .addEntitlements(createEntitlement(CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION))
                                .addEntitlements(createEntitlement(CDP_SECURITY_ENFORCING_SELINUX))
                                .addEntitlements(createEntitlement(CDP_CB_CM_TEMPLATE_SYNC))
                                .setGlobalPasswordPolicy(workloadPasswordPolicy)
                                .setAccountId(getAccountId(request.getExternalAccountId(), accountId))
                                .setExternalAccountId(getExternalAccountId(request.getExternalAccountId(), accountId))
                                .build())
                        .build());
        responseObserver.onCompleted();
    }

    private String getExternalAccountId(String externalAccountId, String accountId) {
        return CLOUDERA_ADMINISTRATION_EXTERNAL_ACCOUNT_ID.equals(externalAccountId) ? externalAccountId : "external-" + accountId;
    }

    private String getAccountId(String externalAccountId, String accountId) {
        return CLOUDERA_ADMINISTRATION_EXTERNAL_ACCOUNT_ID.equals(externalAccountId) ? MOCK_CLOUDBREAKADMIN_ACCOUNT_ID : accountId;
    }

    private Entitlement createEntitlement(String entitlement) {
        return Entitlement.newBuilder()
                .setEntitlementName(entitlement)
                .build();
    }

    private Entitlement createEntitlement(com.sequenceiq.cloudbreak.auth.altus.model.Entitlement entitlement) {
        return createEntitlement(entitlement.name());
    }

    @Override
    public void grantEntitlement(GrantEntitlementRequest request, StreamObserver<GrantEntitlementResponse> responseObserver) {
        LOGGER.info("Grant entitlement '{}' for account: {}", request.getEntitlementName(), request.getAccountId());
        if (request.getEntitlementName().equalsIgnoreCase("CLEAN_UP")) {
            grantedEntitlements.clear();
            revokedEntitlements.clear();
        } else {
            grantedEntitlements.add(request.getEntitlementName());
            revokedEntitlements.remove(request.getEntitlementName());
        }
        responseObserver.onNext(GrantEntitlementResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void revokeEntitlement(RevokeEntitlementRequest request, StreamObserver<RevokeEntitlementResponse> responseObserver) {
        LOGGER.info("Revoke entitlement '{}' for account: {}", request.getEntitlementName(), request.getAccountId());
        grantedEntitlements.remove(request.getEntitlementName());
        revokedEntitlements.add(request.getEntitlementName());
        responseObserver.onNext(RevokeEntitlementResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void listTerms(ListTermsRequest request, StreamObserver<ListTermsResponse> responseObserver) {
        responseObserver.onNext(
                ListTermsResponse.newBuilder().build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void verifyInteractiveUserSessionToken(VerifyInteractiveUserSessionTokenRequest request,
            StreamObserver<VerifyInteractiveUserSessionTokenResponse> responseObserver) {
        LOGGER.trace("Verify interactive user session token: {}", request.getSessionToken());
        String sessionToken = request.getSessionToken();
        Jwt token = decodeAndVerify(sessionToken, SIGNATURE_VERIFIER);
        AltusToken introspectResponse = jsonUtil.toObject(token.getClaims(), AltusToken.class);
        String userIdOrCrn = introspectResponse.getSub();
        String[] splitCrn = userIdOrCrn.split(CRN_COMPONENT_SEPARATOR_REGEX);
        responseObserver.onNext(
                VerifyInteractiveUserSessionTokenResponse.newBuilder()
                        .setAccountId(splitCrn[4])
                        .setAccountType(AccountType.REGULAR)
                        .setUserCrn(userIdOrCrn)
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void authenticate(AuthenticateRequest request,
            StreamObserver<AuthenticateResponse> responseObserver) {
        String authHeader = request.getAccessKeyV1AuthRequest().getAuthHeader();
        String crn = CrnHelper.extractCrnFromAuthHeader(authHeader);
        LOGGER.debug("Crn: {}", crn);

        responseObserver.onNext(
                AuthenticateResponse.newBuilder()
                        .setActorCrn(crn)
                        .build());
        responseObserver.onCompleted();
    }

    public void assignResourceRole(AssignResourceRoleRequest request, StreamObserver<AssignResourceRoleResponse> responseObserver) {
        responseObserver.onNext(AssignResourceRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void unassignResourceRole(UnassignResourceRoleRequest request, StreamObserver<UnassignResourceRoleResponse> responseObserver) {
        LOGGER.info("Unassign resource role: {}", request.getActor());
        responseObserver.onNext(UnassignResourceRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void assignRole(AssignRoleRequest request, StreamObserver<AssignRoleResponse> responseObserver) {
        LOGGER.info("Assign role: {}", request.getActor());
        responseObserver.onNext(AssignRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void unassignRole(UnassignRoleRequest request, StreamObserver<UnassignRoleResponse> responseObserver) {
        LOGGER.info("Unassign role: {}", request.getActor());
        responseObserver.onNext(UnassignRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAssigneeAuthorizationInformation(GetAssigneeAuthorizationInformationRequest request,
            StreamObserver<GetAssigneeAuthorizationInformationResponse> responseObserver) {
        LOGGER.info("Get assignee authorization information for crn: {}", request.getAssigneeCrn());
        responseObserver.onNext(GetAssigneeAuthorizationInformationResponse.newBuilder()
                .setResourceAssignment(0, createResourceAssignment(request.getAssigneeCrn()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listResourceAssignees(ListResourceAssigneesRequest request,
            StreamObserver<ListResourceAssigneesResponse> responseObserver) {
        LOGGER.info("List resource assignees for resource: {}", request.getResourceCrn());
        responseObserver.onNext(ListResourceAssigneesResponse.newBuilder()
                .setResourceAssignee(0, createResourceAssignee())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void notifyResourceDeleted(NotifyResourceDeletedRequest request,
            StreamObserver<NotifyResourceDeletedResponse> responseObserver) {
        LOGGER.info("Notify resource deleted: {}", request.getResourceCrn());
        responseObserver.onNext(NotifyResourceDeletedResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void createAccessKey(CreateAccessKeyRequest request,
            StreamObserver<CreateAccessKeyResponse> responseObserver) {
        LOGGER.info("Create access key for account: {}", request.getAccountId());
        String accessKeyId;
        String privateKey;
        AltusCredential altusCredential = AccessKeyType.Value.UNSET.equals(request.getType())
                ? telemetryPublisherCredential : fluentCredential;
        if (altusCredential != null) {
            accessKeyId = altusCredential.getAccessKey();
            privateKey = new String(altusCredential.getPrivateKey());
        } else {
            accessKeyId = UUID.randomUUID().toString();
            privateKey = UUID.randomUUID().toString();
        }
        responseObserver.onNext(CreateAccessKeyResponse.newBuilder()
                .setPrivateKey(privateKey)
                .setAccessKey(AccessKey.newBuilder()
                        .setAccessKeyId(accessKeyId)
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listAccessKeys(ListAccessKeysRequest request, StreamObserver<ListAccessKeysResponse> responseObserver) {
        LOGGER.info("List access keys for: {}", request.getAccountId());
        responseObserver.onNext(ListAccessKeysResponse.newBuilder()
                .addAccessKey(0, AccessKey.newBuilder()
                        .setAccessKeyId(UUID.randomUUID().toString())
                        .setCrn(UUID.randomUUID().toString())
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteAccessKey(DeleteAccessKeyRequest request,
            StreamObserver<DeleteAccessKeyResponse> responseObserver) {
        LOGGER.info("Delete access key: {}", request.getAccessKeyIdOrCrn());
        responseObserver.onNext(DeleteAccessKeyResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void createMachineUser(CreateMachineUserRequest request,
            StreamObserver<CreateMachineUserResponse> responseObserver) {
        String accountId = Crn.fromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn()).getAccountId();
        String name = request.getMachineUserName();
        LOGGER.info("Create machine user for account {} with name {}", accountId, name);
        responseObserver.onNext(CreateMachineUserResponse.newBuilder()
                .setMachineUser(MachineUser.newBuilder()
                        .setMachineUserId(UUID.nameUUIDFromBytes((accountId + '#' + name).getBytes()).toString())
                        .setCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void createWorkloadMachineUser(CreateWorkloadMachineUserRequest request,
            StreamObserver<CreateWorkloadMachineUserResponse> responseObserver) {
        String accountId = Crn.fromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn()).getAccountId();
        String name = request.getMachineUserName();
        LOGGER.info("Create workload machine user for account {} with name {}", accountId, name);
        responseObserver.onNext(CreateWorkloadMachineUserResponse.newBuilder()
                .setPrivateKey(UUID.randomUUID().toString())
                .setAccessKeyId(UUID.randomUUID().toString())
                .setMachineUser(MachineUser.newBuilder()
                        .setMachineUserId(UUID.nameUUIDFromBytes((accountId + '#' + name).getBytes()).toString())
                        .setCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteMachineUser(DeleteMachineUserRequest request, StreamObserver<DeleteMachineUserResponse> responseObserver) {
        LOGGER.info("Delete machine user with name {}", request.getMachineUserNameOrCrn());
        responseObserver.onNext(DeleteMachineUserResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getIdPMetadataForWorkloadSSO(
            GetIdPMetadataForWorkloadSSORequest request,
            StreamObserver<GetIdPMetadataForWorkloadSSOResponse> responseObserver) {
        checkArgument(!Strings.isNullOrEmpty(request.getAccountId()));
        LOGGER.info("Get IdP Metadata For Workload SSO: {}", request.getAccountId());
        try {
            String metadata = Resources.toString(
                    Resources.getResource("sso/cdp-idp-metadata.xml"),
                    Charsets.UTF_8).trim();
            metadata = metadata.replace("accountId_REPLACE_ME", request.getAccountId());
            metadata = metadata.replace("hostname_REPLACE_ME", "localhost");
            responseObserver.onNext(
                    GetIdPMetadataForWorkloadSSOResponse.newBuilder()
                            .setMetadata(metadata)
                            .build());
            responseObserver.onCompleted();
        } catch (IOException e) {
            throw Status.INTERNAL
                    .withDescription("Could not find IdP metadata resource")
                    .withCause(e)
                    .asRuntimeException();
        }
    }

    @Override
    public void listRoles(ListRolesRequest request, StreamObserver<ListRolesResponse> responseObserver) {
        LOGGER.info("List roles for account: {}", request.getAccountId());
        ListRolesResponse.Builder builder = ListRolesResponse.newBuilder();
        Arrays.stream(UmsRole.values()).forEach(role -> builder.addRole(getRole(role)));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private UserManagementProto.Role getRole(UmsRole role) {
        checkNotNull(role);
        return UserManagementProto.Role.newBuilder()
                .setCrn("crn:altus:iam:us-west-1:altus:role:" + role.getRoleName())
                .build();
    }

    /**
     * NOTE: This is a mock implementation and meant only for testing.
     * This implementation returns hard coded pre-defined set of response for workload credentials.
     * For any integration test of debugging purpose, this should not be used and intent is purely mock.
     * Hashed value used internally is sha256 hash of <i>Password123!</i>.
     */
    @Override
    public void getActorWorkloadCredentials(GetActorWorkloadCredentialsRequest request,
            io.grpc.stub.StreamObserver<GetActorWorkloadCredentialsResponse> responseObserver) {
        Crn actorCrn = Crn.safeFromString(request.getActorCrn());
        LOGGER.info("Get actor workload credentials: {}", actorCrn);
        GetActorWorkloadCredentialsResponse.Builder builder = GetActorWorkloadCredentialsResponse.newBuilder(actorWorkloadCredentialsResponse);
        builder.setPasswordHashExpirationDate(System.currentTimeMillis() + PASSWORD_LIFETIME_IN_MILLISECONDS);
        if (sshPublicKey.isPresent()) {
            builder.addSshPublicKey(SshPublicKey.newBuilder(sshPublicKey.get())
                    .setCrn(mockCrnService.createCrn(actorCrn.getAccountId(), CrnResourceDescriptor.PUBLIC_KEY, UUID.randomUUID().toString()).toString())
                    .build());
        }
        builder.setWorkloadUsername(sanitizeWorkloadUsername(actorCrn.getUserId()));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getEventGenerationIds(GetEventGenerationIdsRequest request, StreamObserver<GetEventGenerationIdsResponse> responseObserver) {
        mockCrnService.ensureInternalActor();
        LOGGER.info("Get event generation ids for account: {}", request.getAccountId());
        try {
            responseObserver.onNext(eventGenerationIdsCache.get(request.getAccountId()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            throw Status.INTERNAL
                    .withDescription("Error retrieving/creating event generation ids.")
                    .withCause(e)
                    .asRuntimeException();
        }
    }

    private String getLicense() {
        if (Files.exists(Paths.get(cmLicenseFilePath))) {
            try {
                String license = Files.readString(Path.of(cmLicenseFilePath));
                LOGGER.info("Cloudbreak license file successfully loaded.");
                return license;
            } catch (IOException e) {
                throw new RuntimeException("Error during reading license.", e);
            }
        } else {
            throw new IllegalStateException("The license file could not be found on path: '" + cmLicenseFilePath + "'. "
                    + "Please place your CM license file in your '<cbd_path>/etc' folder. "
                    + "By default the name of the file should be 'license.txt'.");
        }
    }

    private AltusCredential getAltusCredential(String altusCredentialFile, String altusCredentialProfile) {
        if (StringUtils.isNoneEmpty(altusCredentialFile, altusCredentialProfile)
                && Files.exists(Paths.get(altusCredentialFile))) {
            try {
                Map<String, Properties> propsMap = iniUtil.parseIni(new FileReader(altusCredentialFile));
                if (propsMap.containsKey(altusCredentialProfile)) {
                    Properties prop = propsMap.get(altusCredentialProfile);
                    String accessKey = prop.getProperty(ALTUS_ACCESS_KEY_ID, prop.getProperty(CDP_ACCESS_KEY_ID));
                    String privateKey = prop.getProperty(ALTUS_PRIVATE_KEY, prop.getProperty(CDP_PRIVATE_KEY));
                    return new AltusCredential(accessKey, privateKey.toCharArray());
                }
            } catch (IOException e) {
                LOGGER.warn("Error occurred during reading altus credential.", e);
            }
        }
        return null;
    }

    private Optional<SshPublicKey> getSshPublicKey() {
        if (null != sshPublicKeyFilePath) {
            if (Files.exists(Paths.get(sshPublicKeyFilePath))) {
                try {
                    String publicKey = Files.readString(Path.of(sshPublicKeyFilePath));
                    if (publicKey.matches(SSH_PUBLIC_KEY_PATTERN)) {
                        byte[] keyData = Base64Util.decodeAsByteArray(publicKey.trim().split(" ")[1]);

                        try {
                            MessageDigest digest = MessageDigest.getInstance("SHA-256");
                            byte[] keyDigest = digest.digest(keyData);
                            String fingerprint = Base64Util.encode(keyDigest);
                            while (fingerprint.endsWith("=")) {
                                fingerprint = fingerprint.substring(0, fingerprint.length() - 1);
                            }
                            SshPublicKey sshPublicKey = SshPublicKey.newBuilder()
                                    .setPublicKey(publicKey)
                                    .setPublicKeyFingerprint(fingerprint)
                                    .build();
                            LOGGER.info("Ssh public key file loaded for mocking");
                            return Optional.of(sshPublicKey);
                        } catch (NoSuchAlgorithmException ex) {
                            LOGGER.warn("Unable to calculate public ssh key fingerprint. Proceeding without ssh public key.", ex);
                        }
                    } else {
                        LOGGER.warn("The provided ssh public key at path '{}' is invalid. It must be an RSA or ED25519 key." +
                                "Proceeding without ssh public key.", sshPublicKeyFilePath);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Unable to load ssh public key from '{}'. Proceeding without ssh public key", sshPublicKeyFilePath);
                }
            } else {
                LOGGER.warn("ssh public key not available at path '{}'. Proceeding without ssh public key", sshPublicKeyFilePath);
            }
        } else {
            LOGGER.warn("ssh public key file path not specified. Proceeding without ssh public key");
        }
        return Optional.empty();
    }

    @Override
    public void getWorkloadAdministrationGroupName(GetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<GetWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockGroupManagementService.getWorkloadAdministrationGroupName(request, responseObserver);
    }

    @Override
    public void setWorkloadAdministrationGroupName(SetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<SetWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockGroupManagementService.setWorkloadAdministrationGroupName(request, responseObserver);
    }

    @Override
    public void deleteWorkloadAdministrationGroupName(DeleteWorkloadAdministrationGroupNameRequest request,
            StreamObserver<DeleteWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockGroupManagementService.deleteWorkloadAdministrationGroupName(request, responseObserver);
    }

    @Override
    public void listResourceRoles(UserManagementProto.ListResourceRolesRequest request,
            StreamObserver<UserManagementProto.ListResourceRolesResponse> responseObserver) {
        UserManagementProto.ListResourceRolesResponse.Builder builder = UserManagementProto.ListResourceRolesResponse.newBuilder();
        Arrays.stream(UmsResourceRole.values()).forEach(umsResourceRole -> builder.addResourceRole(getResourceRole(umsResourceRole)));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteWorkloadMachineUser(UserManagementProto.DeleteWorkloadMachineUserRequest request,
            StreamObserver<UserManagementProto.DeleteWorkloadMachineUserResponse> responseObserver) {
        String accountId = Crn.fromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn()).getAccountId();
        String name = request.getMachineUserNameOrCrn();
        LOGGER.info("Delete workload machine user for account {} with name {}", accountId, name);
        responseObserver.onNext(UserManagementProto.DeleteWorkloadMachineUserResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkloadAuthConfiguration(UserManagementProto.GetWorkloadAuthConfigurationRequest request,
            StreamObserver<UserManagementProto.GetWorkloadAuthConfigurationResponse> responseObserver) {
        LOGGER.info("Get workload auth configuration for account: {}", request.getAccountId());
        UserManagementProto.GetWorkloadAuthConfigurationResponse response = UserManagementProto.GetWorkloadAuthConfigurationResponse.newBuilder()
                .addResponseTypesSupported("id_token")
                .addSubjectTypesSupported("public")
                .addIdTokenSigningAlgValuesSupported("RS256")
                .addAllClaimsSupported(Arrays.asList("sub", "aud", "iss", "nbf", "exp", "given_name", "family_name", "email", "groups", "type"))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAccessKeyVerificationData(UserManagementProto.GetAccessKeyVerificationDataRequest request,
            StreamObserver<UserManagementProto.GetAccessKeyVerificationDataResponse> responseObserver) {
        String accountId = Crn.fromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn()).getAccountId();
        LOGGER.info("Get access key verification data {}", request);
        responseObserver.onNext(UserManagementProto.GetAccessKeyVerificationDataResponse.newBuilder()
                .setAccessKeyId(request.getAccessKeyId())
                .setAccountType(AccountType.REGULAR)
                .setAccountId(accountId)
                .setType(AccessKeyType.Value.ED25519)
                .setTypeValue(2)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkRights(CheckRightsRequest request, StreamObserver<CheckRightsResponse> responseObserver) {
        LOGGER.info("Check {} rights for {}, ", request.getCheckList(), request.getActorCrn());
        CheckRightsResponse.Builder builder = CheckRightsResponse.newBuilder();
        for (RightsCheck rightsCheck : request.getCheckList()) {
            builder.addResult(true);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private UserManagementProto.ResourceRole getResourceRole(UmsResourceRole resourceRole) {
        checkNotNull(resourceRole);
        return UserManagementProto.ResourceRole.newBuilder()
                .setCrn("crn:altus:iam:us-west-1:altus:resourceRole:" + resourceRole.getResourceRoleName())
                .build();
    }

    private ResourceAssignee createResourceAssignee() {
        return ResourceAssignee.newBuilder()
                .setAssigneeCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                .setResourceRoleCrn("crn:altus:iam:us-west-1:altus:resourceRole:WorkspaceManager")
                .build();
    }

    private ResourceAssignment createResourceAssignment(String assigneeCrn) {
        String resourceCrn = mockCrnService.createCrn(assigneeCrn, CrnResourceDescriptor.CREDENTIAL, Crn.fromString(assigneeCrn).getAccountId()).toString();
        return ResourceAssignment.newBuilder()
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn("crn:altus:iam:us-west-1:altus:resourceRole:WorkspaceManager")
                .build();
    }

    @VisibleForTesting
    String sanitizeWorkloadUsername(String userName) {
        return SanitizerUtil.sanitizeWorkloadUsername(userName);
    }
}
