package com.sequenceiq.thunderhead.grpc.service.auth;

import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import static com.google.common.base.Preconditions.checkArgument;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AUTOMATIC_USERSYNC_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_IMAGE_MARKETPLACE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_UAE_CENTRAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_BASE_IMAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_DISK_ENCRYPTION_WITH_CMK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE_FREEIPA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_VARIANT_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_DISK_SSE_WITH_CMK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_ENCRYPTION_AT_HOST;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_DATABASE_WIRE_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V1_TO_V2_JUMPGATE_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_JUMPGATE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_TO_V2_JUMPGATE_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_USE_ONE_WAY_TLS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CENTRAL_COMPUTE_MONITORING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_IDENTITY_MAPPING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AWS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_BULK_HOSTS_REMOVAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_HA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CONCLUSION_CHECKER_SEND_USER_EVENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_CUSTOM_CONFIGS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_NODESTATUS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_RESIZE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_RESIZE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_SELECT_INSTANCE_TYPE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_ZDU_OS_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_AWS_EFS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_LOAD_BALANCER_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_DISTROX_INSTANCE_TYPES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_DELAYED_STOP_START;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_RECIPE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_USERSYNC_THREAD_TIMEOUT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_REBUILD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_SELECT_INSTANCE_TYPE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MICRO_DUTY_SDX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_NODESTATUS_ENABLE_SALT_PING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_OS_UPGRADE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RAW_S3;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ROTATE_SALTUSER_PASSWORD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS_SDX_INTEGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SDX_HBASE_CLOUD_STORAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SHOW_CLI;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_TARGETED_UPSCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UNBOUND_ELIMINATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USE_CM_SYNC_COMMAND_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_VM_DIAGNOSTICS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CLOUDERA_INTERNAL_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AWS_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AWS_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AZURE_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AZURE_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_FLOW_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_STREAMING_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.E2E_TEST_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.EPHEMERAL_DISKS_FOR_TEMP_DATA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.FMS_FREEIPA_BATCH_CALL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.LOCAL_DEV;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OJDBC_TOKEN_DH;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.UI_EDP_PROGRESS_BAR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.WORKLOAD_IAM_SYNC;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.thunderhead.grpc.GrpcActorContext;
import com.sequenceiq.thunderhead.model.AltusToken;
import com.sequenceiq.thunderhead.util.CrnHelper;
import com.sequenceiq.thunderhead.util.IniUtil;
import com.sequenceiq.thunderhead.util.JsonUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class MockUserManagementService extends UserManagementImplBase {

    public static final String ACCOUNT_SUBDOMAIN = "xcu2-8y8x";

    @VisibleForTesting
    static final long PASSWORD_LIFETIME = 31449600000L;

    private static final Logger LOGGER = LoggerFactory.getLogger(MockUserManagementService.class);

    private static final MacSigner SIGNATURE_VERIFIER = new MacSigner("titok");

    private static final String ALTUS_ACCESS_KEY_ID = "altus_access_key_id";

    private static final String ALTUS_PRIVATE_KEY = "altus_private_key";

    private static final String CDP_ACCESS_KEY_ID = "cdp_access_key_id";

    private static final String CDP_PRIVATE_KEY = "cdp_private_key";

    private static final int MOCK_USER_COUNT = 100;

    private static final String ACCOUNT_ID_ALTUS = "altus";

    private static final long CREATION_DATE_MS = 1483228800000L;

    private static final String ALL_RIGHTS_AND_RESOURCES = "*";

    private static final String MOCK_RESOURCE = "mock_resource";

    private static final String SSH_PUBLIC_KEY_PATTERN = "^ssh-(rsa|ed25519)\\s+AAAA(B|C)3NzaC1.*(|\\n)";

    // See com.cloudera.thunderhead.service.common.entitlements.CdpEntitlements.CDP_CP_CUSTOM_DL_TEMPLATE
    // not used in CB, but used in CDP CLI, so we need this in mock for local development
    private static final String CDP_CP_CUSTOM_DL_TEMPLATE = "CDP_CM_ADMIN_CREDENTIALS";

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

    @Value("${auth.mock.freeipa.ha.repair.enable}")
    private boolean enableFreeIpaHaRepair;

    @Value("${auth.mock.freeipa.rebuild.enable}")
    private boolean enableFreeIpaRebuild;

    @Value("${auth.mock.cloudstoragevalidation.enable.global}")
    private boolean enableCloudStorageValidation;

    @Value("${auth.mock.cloudstoragevalidation.enable.aws}")
    private boolean enableAwsCloudStorageValidation;

    @Value("${auth.mock.cloudstoragevalidation.enable.azure}")
    private boolean enableAzureCloudStorageValidation;

    @Value("${auth.mock.cloudstoragevalidation.enable.gcp}")
    private boolean enableGcpCloudStorageValidation;

    @Value("${auth.mock.runtime.upgrade.enable}")
    private boolean runtimeUpgradeEnabled;

    @Value("${auth.mock.datahub.runtime.upgrade.enable}")
    private boolean datahubRuntimeUpgradeEnabled;

    @Value("${auth.mock.datahub.os.upgrade.enable}")
    private boolean datahubOsUpgradeEnabled;

    @Value("${auth.mock.ccmv2.enable}")
    private boolean ccmV2Enabled;

    @Value("${auth.mock.ccmv2jumpgate.enable}")
    private boolean ccmV2JumpgateEnabled;

    @Value("${auth.mock.ccmv2.useOneWayTls}")
    private boolean ccmV2UseOneWayTls;

    @Value("${auth.mock.ccmv1.upgradeToV2Jumpgate.enable}")
    private boolean ccmV1UpgradeToV2JumpgateEnabled;

    @Value("${auth.mock.ccmv2.upgradeToV2Jumpgate.enable}")
    private boolean ccmV2UpgradeToV2JumpgateEnabled;

    @Value("${auth.mock.microdutysdx.enable}")
    private boolean microDutySdxEnabled;

    @Value("${auth.mock.ha.repair.enable}")
    private boolean haRepairEnabled;

    @Value("${auth.mock.ha.upgrade.enable}")
    private boolean haUpgradeEnabled;

    @Value("${auth.mock.azure.single.resourcegroup.enable}")
    private boolean enableAzureSingleResourceGroupDeployment;

    @Value("${auth.mock.azure.single.resourcegroup.dedicated.storage.account.enable}")
    private boolean enableAzureSingleResourceGroupDedicatedStorageAccount;

    @Value("${auth.mock.azure.marketplace.images.enable}")
    private boolean enableAzureMarketplaceImages;

    @Value("${auth.mock.cloudidentitymappinng.enable}")
    private boolean enableCloudIdentityMappinng;

    @Value("${auth.mock.upgrade.internalrepo.enable}")
    private boolean enableInternalRepositoryForUpgrade;

    @Value("${auth.mock.hbase.cloudstorage.enable}")
    private boolean enableHbaseCloudStorage;

    @Value("${auth.mock.datalake.efs.enable}")
    private boolean enableDataLakeEfs;

    @Value("${auth.mock.differentdatahubversionthandatalake.enabled}")
    private boolean enableDifferentDataHubVersionThanDataLake;

    @Value("${auth.mock.database.wire.encryption.enable}")
    private boolean enableDatabaseWireEncryption;

    @Value("${auth.mock.datalake.loadbalancer.enable}")
    private boolean datalakeLoadBalancerEnabled;

    @Value("${auth.mock.datalake.loadbalancer.enable}")
    private boolean azureDatalakeLoadBalancerEnabled;

    @Value("${auth.mock.environment.experience.deletion.enable}")
    private boolean enableExperienceDeletionByEnvironment;

    @Value("${auth.mock.endpointgateway.enable.azure}")
    private boolean azureEndpointGatewayEnabled;

    @Value("${auth.mock.endpointgateway.enable.gcp}")
    private boolean gcpEndpointGatewayEnabled;

    @Value("${auth.mock.datalake.backup.on.upgrade.enable}")
    private boolean datalakeBackupOnUpgrade;

    @Value("${auth.mock.datalake.backup.on.resize.enable}")
    private boolean datalakeBackupOnResize;

    @Value("${auth.mock.datalake.recovery.resize.enable}")
    private boolean datalakeResizeRecovery;

    @Value("${auth.mock.datalake.light.to.medium.migration.enable}")
    private boolean datalakeLightToMediumMigration;

    @Value("${auth.mock.cm.sync.command.poller.enable}")
    private boolean cmSyncCommandPollerEnabled;

    @Value("${auth.mock.conclusion.checker.send.user.event.enable}")
    private boolean conclusionCheckerSendUserEvent;

    @Value("${auth.mock.datahub.nodestatus.check.enable}")
    private boolean datahubNodestatusCheck;

    @Value("${auth.mock.diagnostics.vm.enable}")
    private boolean diagnosticsEnabled;

    @Value("${auth.mock.nodestatus.salt.ping.enable}")
    private boolean nodestatusSaltPingEnabled;

    @Value("${auth.mock.ephemeral.disks.for.temp.data.enable}")
    private boolean ephemeralDisksForTempDataEnabled;

    @Value("${auth.mock.datalake.multiaz.enable}")
    private boolean enableMultiAzDataLake;

    @Value("${auth.mock.freeipa.multiaz.enable}")
    private boolean enableMultiAzFreeIpa;

    @Value("${auth.mock.unbound.elimination.enable}")
    private boolean enableUnboundElimination;

    @Value("${auth.mock.targeted.upscale.enable}")
    private boolean enableTargetedUpscale;

    @Value("${auth.mock.aws.native.variant.migration.enable}")
    private boolean enableAwsVariantMigration;

    @Value("${auth.mock.e2e.test.only.enable}")
    private boolean enableE2ETestOnly;

    @Value("${auth.mock.datalake.select.instance.type.enable}")
    private boolean enableDatalakeSelectInstanceType;

    @Value("${auth.mock.freeipa.select.instance.type.enable}")
    private boolean enableFreeIpaSelectInstanceType;

    @Value("${auth.mock.datalake.zdu.osupgrade.enable}")
    private boolean enableDatalakeZduOSUpgrade;

    @Value("${auth.mock.workloadiam.sync.enable}")
    private boolean enableWorkloadIamSync;

    private String cbLicense;

    private AltusCredential telemetyPublisherCredential;

    private AltusCredential fluentCredential;

    private final Map<String, Set<String>> accountUsers = new ConcurrentHashMap<>();

    @Value("${auth.mock.event-generation.expiration-minutes:10}")
    private long eventGenerationExirationMinutes;

    private LoadingCache<String, GetEventGenerationIdsResponse> eventGenerationIdsCache;

    private GetActorWorkloadCredentialsResponse actorWorkloadCredentialsResponse;

    private WorkloadPasswordPolicy workloadPasswordPolicy;

    private Optional<SshPublicKey> sshPublicKey;

    @Value("${auth.mock.azure.disk.SSEWithCMK.enable}")
    private boolean enableAzureDiskSSEWithCMK;

    @Value("${auth.mock.azure.encryptionAtHost.enable}")
    private boolean enableAzureEncryptionAtHost;

    @Value("${auth.mock.aws.disk.EncryptionWithCMK.enable}")
    private boolean enableAWSDiskEncryptionWithCMK;

    @Value("${auth.mock.datahub.customconfigs.enable}")
    private boolean datahubCustomConfigsEnabled;

    @Value("${auth.mock.user.sync.credentials.update.optimization.enable}")
    private boolean userSyncCredentialsUpdateOptimizationEnabled;

    @Value("${auth.mock.endpointgateway.skip.validation}")
    private boolean endpointGatewaySkipValidation;

    @Value("${auth.mock.cm.ha.enable}")
    private boolean cmHAEnabled;

    @Value("${auth.mock.gcp.disk.EncryptionWithCMEK.enable}")
    private boolean enableGcpDiskEncryptionWithCMEK;

    @Value("${auth.mock.freeipa.batch.call.enable}")
    private boolean enableFmsFreeipaBatchCall;

    @Value("${auth.mock.ui.edp.progress.bar.enable}")
    private boolean edpProgressBarEnabled;

    @Value("${auth.mock.datahub.instancetypes.enable}")
    private boolean enableDistroxInstanceTypes;

    @Value("${auth.mock.freeipa.upgrade.enable}")
    private boolean enableFreeIpaUpgrade;

    @Value("${auth.mock.saas.enable}")
    private boolean enableSaas;

    @Value("${auth.mock.freeipa.usersync.thread.timeout.enable}")
    private boolean enableFreeipaUsersyncTimeout;

    @Value("${auth.mock.freeipa.delayedstopstart.enable}")
    private boolean enableFreeipaDelayedStopStart;

    @Value("${auth.mock.saas.sdx.integration.enable}")
    private boolean enableSdxSaasIntegration;

    @Value("${auth.mock.freeipa.recipes.enable}")
    private boolean enableFreeIpaRecipes;

    @Value("${auth.mock.compute.monitoring.enable}")
    private boolean enableComputeMonitoring;

    @Value("${auth.mock.salt.rotatepassword.enable}")
    private boolean enableSalRotatePassword;

    @PostConstruct
    public void init() {
        cbLicense = getLicense();
        telemetyPublisherCredential = getAltusCredential(databusTpCredentialFile, databusTpCredentialProfile);
        fluentCredential = getAltusCredential(databusFluentCredentialFile, databusFluentCredentialProfile);
        eventGenerationIdsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(eventGenerationExirationMinutes, TimeUnit.MINUTES)
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
        builder.setWorkloadPasswordMaxLifetime(PASSWORD_LIFETIME);
        workloadPasswordPolicy = builder.build();
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        LOGGER.info("Get user: {}", request.getUserIdOrCrn());
        String userIdOrCrn = request.getUserIdOrCrn();
        String[] splittedCrn = userIdOrCrn.split(":");
        String userName = splittedCrn[6];
        String accountId = splittedCrn[4];
        accountUsers.computeIfAbsent(accountId, key -> newSetFromMap(new ConcurrentHashMap<>())).add(userName);
        responseObserver.onNext(
                GetUserResponse.newBuilder()
                        .setUser(createUser(accountId, userName))
                        .build());
        responseObserver.onCompleted();
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
            }
            responseObserver.onNext(userBuilder.build());
        } else {
            String userIdOrCrn = request.getUserIdOrCrn(0);
            String[] splittedCrn = userIdOrCrn.split(":");
            String userName = splittedCrn[6];
            String accountId = splittedCrn[4];
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
        LOGGER.info("List workload administration groups for member: {}, accountid: {}", memberCrn, accountId);
        Set<String> groups = mockGroupManagementService.getOrCreateWorkloadGroups(accountId).stream().map(Group::getGroupName).collect(Collectors.toSet());
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
            String[] splittedCrn = machineUserIdOrCrn.split(":");
            String userName;
            String accountId = request.getAccountId();
            String crnString;
            if (splittedCrn.length > 1) {
                userName = splittedCrn[6];
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
        if (enableFreeIpaHaRepair) {
            builder.addEntitlements(createEntitlement(CDP_FREEIPA_HA_REPAIR));
        }
        if (enableFreeIpaRebuild) {
            builder.addEntitlements(createEntitlement(CDP_FREEIPA_REBUILD));
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
        if (runtimeUpgradeEnabled) {
            builder.addEntitlements(createEntitlement(CDP_RUNTIME_UPGRADE));
        }
        if (datahubRuntimeUpgradeEnabled) {
            builder.addEntitlements(createEntitlement(CDP_RUNTIME_UPGRADE_DATAHUB));
        }
        if (datahubOsUpgradeEnabled) {
            builder.addEntitlements(createEntitlement(CDP_OS_UPGRADE_DATAHUB));
        }
        if (ccmV2Enabled) {
            builder.addEntitlements(createEntitlement(CDP_CCM_V2));
        }
        if (ccmV2JumpgateEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CCM_V2_JUMPGATE));
        }
        if (ccmV2UseOneWayTls) {
            builder.addEntitlements(createEntitlement(CDP_CCM_V2_USE_ONE_WAY_TLS));
        }
        if (ccmV1UpgradeToV2JumpgateEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CCM_V1_TO_V2_JUMPGATE_UPGRADE));
        }
        if (ccmV2UpgradeToV2JumpgateEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CCM_V2_TO_V2_JUMPGATE_UPGRADE));
        }
        if (microDutySdxEnabled) {
            builder.addEntitlements(createEntitlement(CDP_MICRO_DUTY_SDX));
        }
        if (haUpgradeEnabled) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_HA_UPGRADE));
        }
        if (haRepairEnabled) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_HA_REPAIR));
        }
        if (enableAzureSingleResourceGroupDeployment) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_SINGLE_RESOURCE_GROUP));
        }
        if (enableAzureSingleResourceGroupDedicatedStorageAccount) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT));
        }
        if (enableAzureMarketplaceImages) {
            builder.addEntitlements(createEntitlement(CDP_AZURE_IMAGE_MARKETPLACE));
        }
        if (enableCloudIdentityMappinng) {
            builder.addEntitlements(createEntitlement(CDP_CLOUD_IDENTITY_MAPPING));
        }
        if (enableInternalRepositoryForUpgrade) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE));
        }
        if (enableHbaseCloudStorage) {
            builder.addEntitlements(createEntitlement(CDP_SDX_HBASE_CLOUD_STORAGE));
        }
        if (enableDataLakeEfs) {
            builder.addEntitlements(createEntitlement(CDP_DATA_LAKE_AWS_EFS));
        }
        if (enableDifferentDataHubVersionThanDataLake) {
            builder.addEntitlements(createEntitlement(CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE));
        }
        if (enableDatabaseWireEncryption) {
            builder.addEntitlements(createEntitlement(CDP_CB_DATABASE_WIRE_ENCRYPTION));
        }
        if (datalakeLoadBalancerEnabled) {
            builder.addEntitlements(createEntitlement(CDP_DATA_LAKE_LOAD_BALANCER));
        }
        if (azureDatalakeLoadBalancerEnabled) {
            builder.addEntitlements(createEntitlement(CDP_DATA_LAKE_LOAD_BALANCER_AZURE));
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
        if (datalakeBackupOnUpgrade) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_BACKUP_ON_UPGRADE));
        }
        if (datalakeBackupOnResize) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_BACKUP_ON_RESIZE));
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
        if (datahubNodestatusCheck) {
            builder.addEntitlements(createEntitlement(CDP_DATAHUB_NODESTATUS_CHECK));
        }
        if (nodestatusSaltPingEnabled) {
            builder.addEntitlements(createEntitlement(CDP_NODESTATUS_ENABLE_SALT_PING));
        }
        if (ephemeralDisksForTempDataEnabled) {
            builder.addEntitlements(createEntitlement(EPHEMERAL_DISKS_FOR_TEMP_DATA));
        }
        if (enableAzureDiskSSEWithCMK) {
            builder.addEntitlements(createEntitlement(CDP_CB_AZURE_DISK_SSE_WITH_CMK));
        }
        if (enableAzureEncryptionAtHost) {
            builder.addEntitlements(createEntitlement(CDP_CB_AZURE_ENCRYPTION_AT_HOST));
        }
        if (enableAWSDiskEncryptionWithCMK) {
            builder.addEntitlements(createEntitlement(CDP_CB_AWS_DISK_ENCRYPTION_WITH_CMK));
        }
        if (datahubCustomConfigsEnabled) {
            builder.addEntitlements(createEntitlement(CDP_DATAHUB_CUSTOM_CONFIGS));
        }
        if (userSyncCredentialsUpdateOptimizationEnabled) {
            builder.addEntitlements(createEntitlement(CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION));
        }
        if (endpointGatewaySkipValidation) {
            builder.addEntitlements(createEntitlement(CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION));
        }
        if (cmHAEnabled) {
            builder.addEntitlements(createEntitlement(CDP_CM_HA));
        }
        if (enableGcpDiskEncryptionWithCMEK) {
            builder.addEntitlements(createEntitlement(CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK));
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
        if (enableMultiAzDataLake) {
            builder.addEntitlements(createEntitlement(CDP_CB_AWS_NATIVE_DATALAKE));
        }
        if (enableMultiAzFreeIpa) {
            builder.addEntitlements(createEntitlement(CDP_CB_AWS_NATIVE_FREEIPA));
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
        if (enableFreeIpaUpgrade) {
            builder.addEntitlements(createEntitlement(CDP_FREEIPA_UPGRADE));
        }
        if (enableFreeipaUsersyncTimeout) {
            builder.addEntitlements(createEntitlement(CDP_FMS_USERSYNC_THREAD_TIMEOUT));
        }
        if (enableFreeipaDelayedStopStart) {
            builder.addEntitlements(createEntitlement(CDP_FMS_DELAYED_STOP_START));
        }
        if (enableFreeIpaRecipes) {
            builder.addEntitlements(createEntitlement(CDP_FMS_RECIPE));
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
        if (enableDatalakeSelectInstanceType) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_SELECT_INSTANCE_TYPE));
        }
        if (enableFreeIpaSelectInstanceType) {
            builder.addEntitlements(createEntitlement(CDP_FREEIPA_SELECT_INSTANCE_TYPE));
        }
        if (enableDatalakeZduOSUpgrade) {
            builder.addEntitlements(createEntitlement(CDP_DATALAKE_ZDU_OS_UPGRADE));
        }
        if (enableWorkloadIamSync) {
            builder.addEntitlements(createEntitlement(WORKLOAD_IAM_SYNC));
        }
        if (enableSdxSaasIntegration) {
            builder.addEntitlements(createEntitlement(CDP_SAAS_SDX_INTEGRATION));
        }
        if (enableComputeMonitoring) {
            builder.addEntitlements(createEntitlement(CDP_CENTRAL_COMPUTE_MONITORING));
        }
        if (enableSalRotatePassword) {
            builder.addEntitlements(createEntitlement(CDP_ROTATE_SALTUSER_PASSWORD));
        }
        responseObserver.onNext(
                GetAccountResponse.newBuilder()
                        .setAccount(builder
                                .setClouderaManagerLicenseKey(cbLicense)
                                .setWorkloadSubdomain(ACCOUNT_SUBDOMAIN)
                                .addEntitlements(createEntitlement(CDP_AZURE))
                                .addEntitlements(createEntitlement(CDP_GCP))
                                .addEntitlements(createEntitlement(CDP_CB_AWS_NATIVE))
                                .addEntitlements(createEntitlement(CDP_AUTOMATIC_USERSYNC_POLLER))
                                .addEntitlements(createEntitlement(CLOUDERA_INTERNAL_ACCOUNT))
                                .addEntitlements(createEntitlement(DATAHUB_AZURE_AUTOSCALING))
                                .addEntitlements(createEntitlement(CDP_AZURE_UAE_CENTRAL))
                                .addEntitlements(createEntitlement(DATAHUB_AWS_AUTOSCALING))
                                .addEntitlements(createEntitlement(DATAHUB_GCP_AUTOSCALING))
                                .addEntitlements(createEntitlement(DATAHUB_AWS_STOP_START_SCALING))
                                .addEntitlements(createEntitlement(DATAHUB_AZURE_STOP_START_SCALING))
                                .addEntitlements(createEntitlement(DATAHUB_GCP_STOP_START_SCALING))
                                .addEntitlements(createEntitlement(LOCAL_DEV))
                                .addEntitlements(createEntitlement(DATAHUB_FLOW_SCALING))
                                .addEntitlements(createEntitlement(CDP_SHOW_CLI))
                                .addEntitlements(createEntitlement(CDP_CP_CUSTOM_DL_TEMPLATE))
                                .addEntitlements(createEntitlement(OJDBC_TOKEN_DH))
                                .addEntitlements(createEntitlement(DATAHUB_STREAMING_SCALING))
                                .addEntitlements(createEntitlement(CDP_RAW_S3))
                                .addEntitlements(createEntitlement(CDP_CM_BULK_HOSTS_REMOVAL))
                                .setGlobalPasswordPolicy(workloadPasswordPolicy)
                                .setAccountId(accountId)
                                .setExternalAccountId("external-" + accountId)
                                .build())
                        .build());
        responseObserver.onCompleted();
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
    public void listTerms(ListTermsRequest request, StreamObserver<ListTermsResponse> responseObserver) {
        responseObserver.onNext(
                ListTermsResponse.newBuilder().build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void verifyInteractiveUserSessionToken(VerifyInteractiveUserSessionTokenRequest request,
            StreamObserver<VerifyInteractiveUserSessionTokenResponse> responseObserver) {
        LOGGER.trace("Verify interative user session token: {}", request.getSessionToken());
        String sessionToken = request.getSessionToken();
        Jwt token = decodeAndVerify(sessionToken, SIGNATURE_VERIFIER);
        AltusToken introspectResponse = jsonUtil.toObject(token.getClaims(), AltusToken.class);
        String userIdOrCrn = introspectResponse.getSub();
        String[] splittedCrn = userIdOrCrn.split(":");
        responseObserver.onNext(
                VerifyInteractiveUserSessionTokenResponse.newBuilder()
                        .setAccountId(splittedCrn[4])
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
                .setResourceAssignment(0, createResourceAssigment(request.getAssigneeCrn()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listResourceAssignees(ListResourceAssigneesRequest request,
            StreamObserver<ListResourceAssigneesResponse> responseObserver) {
        LOGGER.info("List resource assignees for resource: {}", request.getResourceCrn());
        responseObserver.onNext(ListResourceAssigneesResponse.newBuilder()
                .setResourceAssignee(0, createResourceAssignee(request.getResourceCrn()))
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
                ? telemetyPublisherCredential : fluentCredential;
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
        responseObserver.onNext(ListRolesResponse.newBuilder()
                .addRole(Role.newBuilder()
                        .setCrn("crn:altus:iam:us-west-1:altus:role:DbusUploader")
                        .build())
                .build());
        responseObserver.onCompleted();
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
        LOGGER.info("Get actor workload credentials: {}", request.getActorCrn());
        GetActorWorkloadCredentialsResponse.Builder builder = GetActorWorkloadCredentialsResponse.newBuilder(actorWorkloadCredentialsResponse);
        builder.setPasswordHashExpirationDate(System.currentTimeMillis() + PASSWORD_LIFETIME);
        if (sshPublicKey.isPresent()) {
            Crn actorCrn = Crn.safeFromString(request.getActorCrn());
            builder.addSshPublicKey(SshPublicKey.newBuilder(sshPublicKey.get())
                    .setCrn(mockCrnService.createCrn(actorCrn.getAccountId(), CrnResourceDescriptor.PUBLIC_KEY, UUID.randomUUID().toString()).toString())
                    .build());
        }

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
                        byte[] keyData = Base64.getDecoder().decode(publicKey.trim().split(" ")[1]);

                        try {
                            MessageDigest digest = MessageDigest.getInstance("SHA-256");
                            byte[] keyDigest = digest.digest(keyData);
                            String fingerprint = Base64.getEncoder().encodeToString(keyDigest);
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
        responseObserver.onNext(UserManagementProto.ListResourceRolesResponse.newBuilder()
                .addResourceRole(UserManagementProto.ResourceRole.newBuilder()
                        .setCrn("crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin")
                        .build())
                .addResourceRole(UserManagementProto.ResourceRole.newBuilder()
                        .setCrn("crn:altus:iam:us-west-1:altus:resourceRole:Owner")
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    private ResourceAssignee createResourceAssignee(String resourceCrn) {
        return ResourceAssignee.newBuilder()
                .setAssigneeCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                .setResourceRoleCrn("crn:altus:iam:us-west-1:altus:resourceRole:WorkspaceManager")
                .build();
    }

    private ResourceAssignment createResourceAssigment(String assigneeCrn) {
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
