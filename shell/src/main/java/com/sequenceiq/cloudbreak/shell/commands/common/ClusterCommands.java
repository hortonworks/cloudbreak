package com.sequenceiq.cloudbreak.shell.commands.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.MethodNotSupportedException;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.CustomContainerRequest;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.RDSDatabase;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseStackCommands;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;
import com.sequenceiq.cloudbreak.shell.model.MarathonHostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.NodeCountEntry;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.model.YarnHostgroupEntry;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil.WaitResult;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil.WaitResultStatus;

public class ClusterCommands implements BaseCommands {

    private ShellContext shellContext;

    private final CloudbreakShellUtil cloudbreakShellUtil;

    private final BaseStackCommands stackCommands;

    public ClusterCommands(ShellContext shellContext, CloudbreakShellUtil cloudbreakShellUtil, BaseStackCommands stackCommands) {
        this.shellContext = shellContext;
        this.cloudbreakShellUtil = cloudbreakShellUtil;
        this.stackCommands = stackCommands;
    }

    @CliAvailabilityIndicator("cluster create")
    public boolean createAvailable() {
        if (!shellContext.isBlueprintAvailable()) {
            return false;
        }

        if (shellContext.isMarathonMode()) {
            return shellContext.isSelectedMarathonStackAvailable()
                    && shellContext.getActiveHostGroups().size() == shellContext.getMarathonHostGroups().size();
        } else if (shellContext.isYarnMode()) {
            return shellContext.isSelectedYarnStackAvailable()
                    && shellContext.getActiveHostGroups().size() == shellContext.getYarnHostGroups().size();
        } else {
            return shellContext.getActiveHostGroups().size() == shellContext.getHostGroups().keySet().size();
        }
    }

    @CliCommand(value = "cluster create", help = "Create a new cluster based on a blueprint and optionally a recipe")
    public String createCluster(
            @CliOption(key = "userName", unspecifiedDefaultValue = "admin", help = "Username of the Ambari server") String userName,
            @CliOption(key = "password", unspecifiedDefaultValue = "admin", help = "Password of the Ambari server") String password,
            @CliOption(key = "description", help = "Description of the cluster") String description,
            @CliOption(key = "ambariVersion", help = "Ambari version (e.g. 2.4.0.0-748)") String ambariVersion,
            @CliOption(key = "ambariRepoBaseURL",
                    help = "Ambari repo base url: http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates") String ambariRepoBaseURL,
            @CliOption(key = "ambariRepoGpgKey",
                    help = "Ambari repo GPG key url") String ambariRepoGpgKey,
            @CliOption(key = "stack", help = "Stack definition name (e.g. HDP)") String stack,
            @CliOption(key = "version", help = "Stack definition version (e.g. 2.6)") String version,
            @CliOption(key = "os", help = "Stack OS to select package manager, default is RedHat") String os,
            @CliOption(key = "stackRepoId", help = "Stack repository id (e.g. HDP-2.6)") String stackRepoId,
            @CliOption(key = "stackBaseURL", help = "Stack URL (e.g. http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.0.3)")
                    String stackBaseURL,
            @CliOption(key = "utilsRepoId", help = "Stack utils repoId (e.g. HDP-UTILS-1.1.0.21)") String utilsRepoId,
            @CliOption(key = "utilsBaseURL", help = "Stack utils URL (e.g. http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7)")
                    String utilsBaseURL,
            @CliOption(key = "verify", help = "verify the repository URL-s") Boolean verify,
            @CliOption(key = "connectionURL", help = "JDBC connection URL for HIVE metastore (jdbc:<db-type>://<address>:<port>/<db>)") String connectionURL,
            @CliOption(key = "databaseType", help = "Type of the external database for HIVE metastore (MYSQL, POSTGRES)") RDSDatabase databaseType,
            @CliOption(key = "connectionUserName", help = "Username to use for the jdbc connection for HIVE metastore") String connectionUserName,
            @CliOption(key = "connectionPassword", help = "Password to use for the jdbc connection for HIVE metastore") String connectionPassword,
            @CliOption(key = "hdpVersion", help = "Compatible HDP version for the jdbc configuration for HIVE metastore") String hdpVersion,
            @CliOption(key = "validated", unspecifiedDefaultValue = "true", specifiedDefaultValue = "true",
                    help = "validate the HIVE metastore jdbc config parameters") Boolean validated,
            @CliOption(key = "enableSecurity", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false",
                    help = "Kerberos security status") Boolean enableSecurity,
            @CliOption(key = "kerberosMasterKey", specifiedDefaultValue = "key", help = "Kerberos master key") String kerberosMasterKey,
            @CliOption(key = "kerberosAdmin", specifiedDefaultValue = "admin", help = "Kerberos admin name") String kerberosAdmin,
            @CliOption(key = "kerberosPassword", specifiedDefaultValue = "admin", help = "Kerberos admin password") String kerberosPassword,
            @CliOption(key = "kerberosUrl", help = "Kerberos url (e.g. 10.0.0.4)") String kerberosUrl,
            @CliOption(key = "kerberosRealm", help = "Kerberos realm (e.g. custom.com)") String kerberosRealm,
            @CliOption(key = "kerberosTcpAllowed", help = "Enable TCP for Kerberos", specifiedDefaultValue = "true",
                    unspecifiedDefaultValue = "false")
                    Boolean kerberosTcpAllowed,
            @CliOption(key = "kerberosPrincipal", help = "Kerberos principal (e.g. admin/admin)") String kerberosPrincipal,
            @CliOption(key = "kerberosLdapUrl", help = "Kerberos ldap url (e.g. ldaps://acme.com)") String kerberosLdapUrl,
            @CliOption(key = "kerberosContainerDn", help = "Kerberos container dn (e.g. ou=ambaritest,dc=WWW,dc=ACME,dc=COM)")
                    String kerberosContainerDn,
            @CliOption(key = "configStrategy", help = "Config recommendation strategy") ConfigStrategy strategy,
            @CliOption(key = "enableKnoxGateway", help = "Enable Knox Gateway",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean enableKnoxGateway,
            @CliOption(key = "ambariServerImage", help = "Name of the ambari server image in case of BYOS orchestrator")
                    String ambariServerImage,
            @CliOption(key = "ambariAgentImage", help = "Name of the ambari agent image in case of BYOS orchestrator")
                    String ambariAgentImage,
            @CliOption(key = "ambariDbImage", help = "Name of the ambari db image in case of BYOS orchestrator")
                    String ambariDbImage,
            @CliOption(key = "customQueue", help = "Name of the custom queue for yarn orchestrator",
                    unspecifiedDefaultValue = "default", specifiedDefaultValue = "default") String customQueue,
            @CliOption(key = "executorType", help = "Executor type of yarn") String executorType,
            @CliOption(key = "wait", help = "Wait for stack creation", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true") Long timeout) {
        try {
            Set<HostGroupRequest> hostGroupList = new HashSet<>();
            Set<? extends Entry<String, ? extends NodeCountEntry>> entries;
            if (shellContext.isMarathonMode()) {
                entries = shellContext.getMarathonHostGroups().entrySet();
            } else if (shellContext.isYarnMode()) {
                entries = shellContext.getYarnHostGroups().entrySet();
            } else {
                entries = shellContext.getHostGroups().entrySet();
            }
            for (Entry<String, ? extends NodeCountEntry> entry : entries) {
                HostGroupRequest hostGroupBase = new HostGroupRequest();
                hostGroupBase.setName(entry.getKey());

                ConstraintJson constraintJson = new ConstraintJson();

                constraintJson.setHostCount(entry.getValue().getNodeCount());
                if (shellContext.isMarathonMode()) {
                    constraintJson.setConstraintTemplateName(((MarathonHostgroupEntry) entry.getValue()).getConstraintName());
                } else if (shellContext.isYarnMode()) {
                    constraintJson.setConstraintTemplateName(((YarnHostgroupEntry) entry.getValue()).getConstraintName());
                } else {
                    hostGroupBase.setRecipeIds(((HostgroupEntry) entry.getValue()).getRecipeIdSet());
                    hostGroupBase.setRecoveryMode(((HostgroupEntry) entry.getValue()).getRecoveryMode());
                    constraintJson.setInstanceGroupName(entry.getKey());
                }

                hostGroupBase.setConstraint(constraintJson);
                hostGroupList.add(hostGroupBase);
            }

            ClusterRequest clusterRequest = new ClusterRequest();
            if (shellContext.isMarathonMode() || shellContext.isYarnMode()) {
                CustomContainerRequest customContainerRequest = new CustomContainerRequest();
                Map<String, String> images = new HashMap<>();
                if (!Strings.isNullOrEmpty(ambariServerImage)) {
                    images.put("ambari-server", ambariServerImage);
                }
                if (!Strings.isNullOrEmpty(ambariAgentImage)) {
                    images.put("ambari-agent", ambariAgentImage);
                }
                if (!Strings.isNullOrEmpty(ambariDbImage)) {
                    images.put("ambari_db", ambariDbImage);
                }
                if (!images.isEmpty()) {
                    customContainerRequest.setDefinitions(images);
                    clusterRequest.setCustomContainer(customContainerRequest);
                }
            }
            if (shellContext.isMarathonMode()) {
                clusterRequest.setName(shellContext.getSelectedMarathonStackName());
            } else if (shellContext.isYarnMode()) {
                clusterRequest.setName(shellContext.getSelectedYarnStackName());
            } else {
                clusterRequest.setName(shellContext.getStackName());
            }
            clusterRequest.setDescription(description);
            clusterRequest.setUserName(userName);
            clusterRequest.setPassword(password);
            clusterRequest.setBlueprintId(Long.valueOf(shellContext.getBlueprintId()));
            clusterRequest.setEmailNeeded(false);
            clusterRequest.setEnableSecurity(enableSecurity);
            clusterRequest.setHostGroups(hostGroupList);
            clusterRequest.setBlueprintInputs(new HashSet<>());
            clusterRequest.setCustomQueue(customQueue);
            clusterRequest.setExecutorType(executorType == null ? ExecutorType.DEFAULT : ExecutorType.CONTAINER);

            if (enableKnoxGateway) {
                // Check if Knox is configured in selected blueprint
                List<InstanceGroupEntry> gatewayIgList = shellContext.getInstanceGroups().values()
                        .stream()
                        .filter(e -> "GATEWAY".equals(e.getType())).collect(Collectors.toList());
                List<String> gatewayIgNameList = new ArrayList<>();

                for (Entry<String, InstanceGroupEntry> entry : shellContext.getInstanceGroups().entrySet()) {
                    for (InstanceGroupEntry gatewayIg : gatewayIgList) {
                        if (Objects.equals(gatewayIg, entry.getValue())) {
                            gatewayIgNameList.add(entry.getKey());
                        }
                    }
                }
                Map<String, List<String>> componentMap = getComponentMap(shellContext.getBlueprintText());

                for (String gatewayIgName : gatewayIgNameList) {
                    if (componentMap.get(gatewayIgName).contains("KNOX_GATEWAY")) {
                        throw shellContext.exceptionTransformer().transformToRuntimeException(
                                "Please select another blueprint! Knox gateway is enabled but it is present in the blueprint's gateway hostgroup as well.");
                    }
                }
            }

            if (enableKnoxGateway) {
                // Check if Knox is configured in selected blueprint
                List<InstanceGroupEntry> gatewayIgList = shellContext.getInstanceGroups().values()
                        .stream()
                        .filter(e -> "GATEWAY".equals(e.getType())).collect(Collectors.toList());
                List<String> gatewayIgNameList = new ArrayList<>();

                for (Entry<String, InstanceGroupEntry> entry : shellContext.getInstanceGroups().entrySet()) {
                    for (InstanceGroupEntry gatewayIg : gatewayIgList) {
                        if (Objects.equals(gatewayIg, entry.getValue())) {
                            gatewayIgNameList.add(entry.getKey());
                        }
                    }
                }
                Map<String, List<String>> componentMap = getComponentMap(shellContext.getBlueprintText());

                for (String gatewayIgName : gatewayIgNameList) {
                    if (componentMap.get(gatewayIgName).contains("KNOX_GATEWAY")) {
                        throw shellContext.exceptionTransformer().transformToRuntimeException(
                                "Please select another blueprint! Knox gateway is enabled but it is present in the blueprint's gateway hostgroup as well.");
                    }
                }
            }

            GatewayJson gateway = new GatewayJson();
            gateway.setEnableGateway(enableKnoxGateway);
            gateway.setExposedServices(ImmutableList.of(ExposedService.ALL.name()));
            clusterRequest.setGateway(gateway);

            if (strategy != null) {
                clusterRequest.setConfigStrategy(strategy);
            }

            if (!shellContext.isMarathonMode() && !shellContext.isYarnMode()) {
                FileSystemRequest fileSystemRequest = new FileSystemRequest();
                fileSystemRequest.setName(shellContext.getStackName());
                fileSystemRequest.setDefaultFs(shellContext.getDefaultFileSystem() == null ? true : shellContext.getDefaultFileSystem());
                fileSystemRequest.setType(shellContext.getFileSystemType());

                Map<String, String> fileSystemParameters = new HashMap<>();
                for (String key : shellContext.getFileSystemParameters().keySet()) {
                    fileSystemParameters.put(key, (String) shellContext.getFileSystemParameters().get(key));
                }
                fileSystemRequest.setProperties(fileSystemParameters);

                if (shellContext.getDefaultFileSystem() == null && shellContext.getFileSystemType() == null) {
                    fileSystemRequest = null;
                }
                clusterRequest.setFileSystem(fileSystemRequest);
            }
            KerberosRequest kerberosRequest = new KerberosRequest();
            if (!Strings.isNullOrEmpty(kerberosAdmin)) {
                kerberosRequest.setAdmin(kerberosAdmin);
            }
            if (!Strings.isNullOrEmpty(kerberosPassword)) {
                kerberosRequest.setPassword(kerberosPassword);
            }
            if (!Strings.isNullOrEmpty(kerberosMasterKey)) {
                kerberosRequest.setMasterKey(kerberosMasterKey);
            }
            kerberosRequest.setTcpAllowed(kerberosTcpAllowed == null ? false : kerberosTcpAllowed);
            if (!Strings.isNullOrEmpty(kerberosRealm)) {
                kerberosRequest.setRealm(kerberosRealm);
            }
            if (!Strings.isNullOrEmpty(kerberosUrl)) {
                kerberosRequest.setUrl(kerberosUrl);
            }
            if (!Strings.isNullOrEmpty(kerberosPrincipal)) {
                kerberosRequest.setPrincipal(kerberosPrincipal);
            }
            if (!Strings.isNullOrEmpty(kerberosLdapUrl)) {
                kerberosRequest.setLdapUrl(kerberosLdapUrl);
            }
            if (!Strings.isNullOrEmpty(kerberosContainerDn)) {
                kerberosRequest.setContainerDn(kerberosContainerDn);
            }
            clusterRequest.setKerberos(kerberosRequest);
            if (shellContext.getRdsConfigId() != null) {
                if (connectionURL != null || connectionUserName != null || connectionPassword != null || databaseType != null || hdpVersion != null) {
                    throw shellContext.exceptionTransformer().transformToRuntimeException(
                            "--connectionURL, --databaseType, --connectionUserName, --connectionPassword switches "
                            + "cannot be used if an RDS config is already selected with 'rdsconfig select'");
                }
                Set<Long> rdsConfigs = new HashSet<>();
                rdsConfigs.add(Long.valueOf(shellContext.getRdsConfigId()));
                clusterRequest.setRdsConfigIds(rdsConfigs);
            }
            String ldapConfigId = shellContext.getLdapConfigId();
            if (ldapConfigId != null) {
                clusterRequest.setLdapConfigId(Long.valueOf(ldapConfigId));
            }
            clusterRequest.setValidateBlueprint(false);

            if (ambariVersion != null || ambariRepoBaseURL != null || ambariRepoGpgKey != null) {
                if (ambariVersion == null || ambariRepoBaseURL == null || ambariRepoGpgKey == null) {
                    throw shellContext.exceptionTransformer().transformToRuntimeException("ambariVersion, ambariRepoBaseURL and ambariRepoGpgKey must be set");
                }
                AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
                ambariRepoDetailsJson.setVersion(ambariVersion);
                ambariRepoDetailsJson.setBaseUrl(ambariRepoBaseURL);
                ambariRepoDetailsJson.setGpgKeyUrl(ambariRepoGpgKey);
                clusterRequest.setAmbariRepoDetailsJson(ambariRepoDetailsJson);
            }

            if (shellContext.getAmbariDatabaseDetailsJson() != null) {
                clusterRequest.setAmbariDatabaseDetails(shellContext.getAmbariDatabaseDetailsJson());
            }

            AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
            ambariStackDetailsJson.setOs(os);
            ambariStackDetailsJson.setStack(stack);
            ambariStackDetailsJson.setStackBaseURL(stackBaseURL);
            ambariStackDetailsJson.setStackRepoId(stackRepoId);
            ambariStackDetailsJson.setUtilsBaseURL(utilsBaseURL);
            ambariStackDetailsJson.setUtilsRepoId(utilsRepoId);
            ambariStackDetailsJson.setVerify(verify);
            ambariStackDetailsJson.setVersion(version);

            if (os == null && stack == null && stackBaseURL == null && stackRepoId == null && utilsBaseURL == null
                    && utilsRepoId == null && verify == null && version == null) {
                ambariStackDetailsJson = null;
            }
            clusterRequest.setAmbariStackDetails(ambariStackDetailsJson);

            if (connectionURL != null && connectionUserName != null && connectionPassword != null && databaseType != null && hdpVersion != null) {
                RDSConfigRequest rdsConfigRequest = new RDSConfigRequest();
                rdsConfigRequest.setName(clusterRequest.getName());
                rdsConfigRequest.setConnectionURL(connectionURL);
                rdsConfigRequest.setDatabaseType(databaseType);
                rdsConfigRequest.setConnectionUserName(connectionUserName);
                rdsConfigRequest.setConnectionPassword(connectionPassword);
                rdsConfigRequest.setHdpVersion(hdpVersion);
                rdsConfigRequest.setValidated(validated);
                Set<RDSConfigRequest> rdsConfigJsons = new HashSet<>();
                rdsConfigJsons.add(rdsConfigRequest);
                clusterRequest.setRdsConfigJsons(rdsConfigJsons);
            } else if (connectionURL != null || connectionUserName != null || connectionPassword != null || databaseType != null || hdpVersion != null) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(
                        "connectionURL, databaseType, connectionUserName, connectionPassword and hdpVersion must be all set");
            }

            String stackId;
            if (shellContext.isMarathonMode()) {
                stackId = shellContext.getSelectedMarathonStackId().toString();
            } else if (shellContext.isYarnMode()) {
                stackId = shellContext.getSelectedYarnStackId().toString();
            } else {
                stackId = shellContext.getStackId();
            }
            shellContext.cloudbreakClient().clusterEndpoint().post(Long.valueOf(stackId), clusterRequest);
            shellContext.setHint(Hints.NONE);
            shellContext.resetFileSystemConfiguration();
            shellContext.resetAmbariDatabaseDetailsJson();
            if (wait) {
                WaitResult waitResult =
                        cloudbreakShellUtil.waitAndCheckClusterStatus(Long.valueOf(stackId), Status.AVAILABLE.name(), timeout);
                if (WaitResultStatus.FAILED.equals(waitResult.getWaitResultStatus())) {
                    throw shellContext.exceptionTransformer().transformToRuntimeException(
                            String.format("Cluster creation failed on stack with id: '%s': '%s'", stackId, waitResult.getReason()));
                } else {
                    return "Cluster creation finished";
                }
            }
            return "Cluster creation started";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({"cluster stop", "cluster start"})
    public boolean startStopAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable())
                || (shellContext.isYarnMode() && shellContext.isSelectedYarnStackAvailable());
    }

    @CliCommand(value = "cluster stop", help = "Stop your cluster")
    public String stop() {
        try {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.STOPPED);
            String stackId;
            if (shellContext.isMarathonMode()) {
                stackId = shellContext.getSelectedMarathonStackId().toString();
            } else if (shellContext.isYarnMode()) {
                stackId = shellContext.getSelectedYarnStackId().toString();
            } else {
                stackId = shellContext.getStackId();
            }
            cloudbreakShellUtil.checkResponse("stopCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(Long.valueOf(stackId), updateClusterJson));
            return "Cluster is stopping";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster start", help = "Start your cluster")
    public String start() {
        try {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.STARTED);
            String stackId;
            if (shellContext.isMarathonMode()) {
                stackId = shellContext.getSelectedMarathonStackId().toString();
            } else if (shellContext.isYarnMode()) {
                stackId = shellContext.getSelectedYarnStackId().toString();
            } else {
                stackId = shellContext.getStackId();
            }
            cloudbreakShellUtil.checkResponse("startCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(Long.valueOf(stackId), updateClusterJson));
            return "Cluster is starting";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        throw new MethodNotSupportedException("Cluster delete command not available");
    }

    @CliAvailabilityIndicator("cluster delete")
    @Override
    public boolean deleteAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable())
                || (shellContext.isYarnMode() && shellContext.isSelectedYarnStackAvailable());
    }

    @CliCommand(value = "cluster delete", help = "Delete the cluster by stack id")
    public String delete() {
        try {
            String stackId;
            if (shellContext.isMarathonMode()) {
                stackId = shellContext.getSelectedMarathonStackId().toString();
            } else if (shellContext.isYarnMode()) {
                stackId = shellContext.getSelectedYarnStackId().toString();
            } else {
                stackId = shellContext.getStackId();
            }
            shellContext.cloudbreakClient().clusterEndpoint().delete(Long.valueOf(stackId));
            return "Cluster deletion started with stack id: " + stackId;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public String deleteById(Long id) throws Exception {
        return delete(id, null);
    }

    @Override
    public String deleteByName(String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public boolean selectAvailable() {
        return false;
    }

    @Override
    public String select(Long id, String name) throws Exception {
        throw new MethodNotSupportedException("Cluster select command not available");
    }

    @Override
    public String selectById(Long id) throws Exception {
        return select(id, null);
    }

    @Override
    public String selectByName(String name) throws Exception {
        return select(null, name);
    }

    @Override
    public boolean listAvailable() {
        return false;
    }

    @Override
    public String list() throws Exception {
        throw new MethodNotSupportedException("Cluster list command not available");
    }

    @CliAvailabilityIndicator({"cluster show", "cluster show --id", "cluster show --name"})
    @Override
    public boolean showAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable())
                || (shellContext.isYarnMode() && shellContext.isSelectedYarnStackAvailable());
    }

    @CliCommand(value = "cluster show", help = "Shows the cluster by stack id")
    public String show(@CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) {
        try {
            outPutType = outPutType == null ? OutPutType.RAW : outPutType;
            String stackId;
            if (shellContext.isMarathonMode()) {
                stackId = shellContext.getSelectedMarathonStackId().toString();
            } else if (shellContext.isYarnMode()) {
                stackId = shellContext.getSelectedYarnStackId().toString();
            } else {
                stackId = shellContext.getStackId();
            }
            ClusterResponse clusterResponse = shellContext.cloudbreakClient().clusterEndpoint().get(Long.valueOf(stackId));
            return shellContext.outputTransformer().render(outPutType,
                    shellContext.responseTransformer().transformObjectToStringMap(clusterResponse), "FIELD", "VALUE");
        } catch (IndexOutOfBoundsException ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("There was no cluster for this account");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public String show(Long id, String name, OutPutType outPutType) throws Exception {
        throw new MethodNotSupportedException("Cluster show command not available");
    }

    @Override
    public String showById(Long id, OutPutType outPutType) throws Exception {
        return show(id, null, outPutType);
    }

    @Override
    public String showByName(String name, OutPutType outPutType) throws Exception {
        return show(null, name, outPutType);
    }

    @CliAvailabilityIndicator({"cluster node --ADD", "cluster node --REMOVE"})
    public boolean nodeAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable())
                || (shellContext.isYarnMode() && shellContext.isSelectedYarnStackAvailable());
    }

    @CliAvailabilityIndicator("cluster fileSystem")
    public boolean fileSystemAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "cluster node --ADD", help = "Add new nodes to the cluster")
    public String addNode(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be added to the cluster") Integer adjustment,
            @CliOption(key = "wait", help = "Wait until the operation completes",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true") Long timeout) {
        try {
            if (adjustment < 1) {
                throw shellContext.exceptionTransformer().transformToRuntimeException("The adjustment value in case of node addition should be at least 1");
            }
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setScalingAdjustment(adjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            String stackIdStr;
            if (shellContext.isMarathonMode()) {
                stackIdStr = shellContext.getSelectedMarathonStackId().toString();
            } else if (shellContext.isYarnMode()) {
                stackIdStr = shellContext.getSelectedYarnStackId().toString();
            } else {
                stackIdStr = shellContext.getStackId();
            }
            Long stackId = Long.valueOf(stackIdStr);
            cloudbreakShellUtil.checkResponse("upscaleCluster", shellContext.cloudbreakClient().clusterEndpoint().put(stackId, updateClusterJson));
            if (!wait) {
                return "Cluster upscale started with stack id: " + stackIdStr;
            }
            stackCommands.waitUntilClusterAvailable(stackId, "Cluster upscale failed: ", timeout);
            return "Cluster upscale finished with stack id " + stackIdStr;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster node --REMOVE", help = "Remove nodes from the cluster")
    public String removeNode(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "The number of the nodes to be removed from the cluster.") Integer adjustment,
            @CliOption(key = "withStackDownScale", help = "Do the downscale with the stack together",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean withStackDownScale,
            @CliOption(key = "wait", help = "Wait until the operation completes",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true") Long timeout) {
        try {
            if (adjustment > -1) {
                throw shellContext.exceptionTransformer().transformToRuntimeException("The adjustment value in case of node removal should be negative");
            }
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setScalingAdjustment(adjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(withStackDownScale);
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            String stackIdStr;
            if (shellContext.isMarathonMode()) {
                stackIdStr = shellContext.getSelectedMarathonStackId().toString();
            } else if (shellContext.isYarnMode()) {
                stackIdStr = shellContext.getSelectedYarnStackId().toString();
            } else {
                stackIdStr = shellContext.getStackId();
            }
            Long stackId = Long.valueOf(stackIdStr);
            cloudbreakShellUtil.checkResponse("downscaleCluster", shellContext.cloudbreakClient().clusterEndpoint().put(stackId, updateClusterJson));

            if (!wait) {
                return "Cluster downscale started with stack id: " + stackIdStr;
            }

            stackCommands.waitUntilClusterAvailable(stackId, "Cluster downscale failed: ", timeout);
            if (!withStackDownScale) {
                return "Cluster downscale finished with stack id: " + stackIdStr;
            }

            stackCommands.waitUntilStackAvailable(stackId, "Stack downscale failed: ", timeout);
            return "Cluster and stack downscale finished with stack id " + stackIdStr;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator("cluster sync")
    public boolean syncAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "cluster sync", help = "Sync the cluster")
    public String sync() {
        try {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.SYNC);
            cloudbreakShellUtil.checkResponse("syncCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(Long.valueOf(shellContext.getStackId()), updateClusterJson));
            return "Cluster is syncing";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator("cluster upgrade")
    public boolean upgradeAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand("cluster upgrade")
    public String upgradeCluster(
            @CliOption(key = "baseUrl", mandatory = true, help = "Base URL of the Ambari repo") String baseUrl,
            @CliOption(key = "gpgKeyUrl", mandatory = true, help = "GPG key of the Ambari repo") String gpgKeyUrl,
            @CliOption(key = "version", mandatory = true, help = "Ambari version") String version) {
        AmbariRepoDetailsJson request = new AmbariRepoDetailsJson();
        request.setVersion(version);
        request.setBaseUrl(baseUrl);
        request.setGpgKeyUrl(gpgKeyUrl);
        String stackId = shellContext.getStackId();
        if (stackId == null) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("No stack selected");
        }
        cloudbreakShellUtil.checkResponse("ambariUpgrade", shellContext.cloudbreakClient().clusterEndpoint().upgradeCluster(Long.valueOf(stackId), request));
        return "Upgrade request successfully sent";
    }

    private Map<String, List<String>> getComponentMap(String json) {
        Map<String, List<String>> map = new HashMap<>();
        try {
            JsonNode hostGroups = shellContext.objectMapper().readTree(json.getBytes()).get("host_groups");
            for (JsonNode hostGroup : hostGroups) {
                List<String> components = new ArrayList<>();
                JsonNode componentsNodes = hostGroup.get("components");
                for (JsonNode componentsNode : componentsNodes) {
                    components.add(componentsNode.get("name").asText());
                }
                map.put(hostGroup.get("name").asText(), components);
            }
        } catch (IOException e) {
            map = new HashMap<>();
        }
        return map;
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

}
