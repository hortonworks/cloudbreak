package com.sequenceiq.cloudbreak.shell.commands.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RDSDatabase;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.MarathonHostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.NodeCountEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil;

public class ClusterCommands implements BaseCommands {

    private ShellContext shellContext;
    private CloudbreakShellUtil cloudbreakShellUtil;

    public ClusterCommands(ShellContext shellContext, CloudbreakShellUtil cloudbreakShellUtil) {
        this.shellContext = shellContext;
        this.cloudbreakShellUtil = cloudbreakShellUtil;
    }

    @CliAvailabilityIndicator(value = "cluster create")
    public boolean createAvailable() {
        return shellContext.isBlueprintAvailable()
                && ((shellContext.isStackAvailable()
                && shellContext.getActiveHostGroups().size() == shellContext.getHostGroups().keySet().size())
                || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable()
                && shellContext.getActiveHostGroups().size() == shellContext.getMarathonHostGroups().size()));
    }

    @CliCommand(value = "cluster create", help = "Create a new cluster based on a blueprint and optionally a recipe")
    public String createCluster(
            @CliOption(key = "userName", unspecifiedDefaultValue = "admin", help = "Username of the Ambari server") String userName,
            @CliOption(key = "password", unspecifiedDefaultValue = "admin", help = "Password of the Ambari server") String password,
            @CliOption(key = "description", help = "Description of the blueprint") String description,
            @CliOption(key = "ambariVersion", help = "Ambari version: 2.4.0.0-748") String ambariVersion,
            @CliOption(key = "ambariRepoBaseURL",
                    help = "Ambari repo base url: http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates") String ambariRepoBaseURL,
            @CliOption(key = "ambariRepoGpgKey",
                    help = "Ambari repo GPG key url") String ambariRepoGpgKey,
            @CliOption(key = "stack", help = "Stack definition name, like HDP") String stack,
            @CliOption(key = "version", help = "Stack definition version") String version,
            @CliOption(key = "os", help = "Stack OS to select package manager, default is RedHat") String os,
            @CliOption(key = "stackRepoId", help = "Stack repository id") String stackRepoId,
            @CliOption(key = "stackBaseURL", help = "Stack url") String stackBaseURL,
            @CliOption(key = "utilsRepoId", help = "Stack utils repoId") String utilsRepoId,
            @CliOption(key = "utilsBaseURL", help = "Stack utils URL") String utilsBaseURL,
            @CliOption(key = "verify", help = "Whether to verify the URLs or not") Boolean verify,
            @CliOption(key = "connectionURL", help = "JDBC connection URL (jdbc:<db-type>://<address>:<port>/<db>)") String connectionURL,
            @CliOption(key = "databaseType", help = "Type of the external database (MYSQL, POSTGRES)") RDSDatabase databaseType,
            @CliOption(key = "connectionUserName", help = "Username to use for the jdbc connection") String connectionUserName,
            @CliOption(key = "connectionPassword", help = "Password to use for the jdbc connection") String connectionPassword,
            @CliOption(key = "hdpVersion", help = "Compatible HDP version for the jdbc configuration") String hdpVersion,
            @CliOption(key = "validated", unspecifiedDefaultValue = "true", specifiedDefaultValue = "true",
                    help = "the jdbc config parameters will be validated") Boolean validated,
            @CliOption(key = "enableSecurity", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false",
                    help = "Kerberos security status") Boolean enableSecurity,
            @CliOption(key = "kerberosMasterKey", specifiedDefaultValue = "key", help = "Kerberos mater key") String kerberosMasterKey,
            @CliOption(key = "kerberosAdmin", specifiedDefaultValue = "admin", help = "Kerberos admin name") String kerberosAdmin,
            @CliOption(key = "kerberosPassword", specifiedDefaultValue = "admin", help = "Kerberos admin password") String kerberosPassword,
            @CliOption(key = "ldapRequired", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "Start and configure LDAP authentication support for Ambari hosts") Boolean ldapRequired,
            @CliOption(key = "configStrategy", help = "Config recommendation strategy") ConfigStrategy strategy,
            @CliOption(key = "enableShipyard", help = "Run shipyard in cluster") Boolean enableShipyard,
            @CliOption(key = "wait", help = "Wait for stack creation", specifiedDefaultValue = "false") Boolean wait) {
        try {
            Set<HostGroupJson> hostGroupList = new HashSet<>();
            Set<Map.Entry<String, NodeCountEntry>> entries = (Set<Map.Entry<String, NodeCountEntry>>) (shellContext.isMarathonMode()
                    ? shellContext.getMarathonHostGroups().entrySet() : shellContext.getHostGroups().entrySet());
            for (Map.Entry<String, NodeCountEntry> entry : entries) {
                HostGroupJson hostGroupJson = new HostGroupJson();
                hostGroupJson.setName(entry.getKey());

                ConstraintJson constraintJson = new ConstraintJson();

                constraintJson.setHostCount(entry.getValue().getNodeCount());
                if (shellContext.isMarathonMode()) {
                    constraintJson.setConstraintTemplateName(((MarathonHostgroupEntry) entry.getValue()).getConstraintName());
                } else {
                    hostGroupJson.setRecipeIds(((HostgroupEntry) entry.getValue()).getRecipeIdSet());
                    constraintJson.setInstanceGroupName(entry.getKey());
                }

                hostGroupJson.setConstraint(constraintJson);
                hostGroupList.add(hostGroupJson);
            }

            wait = wait == null ? false : wait;
            ClusterRequest clusterRequest = new ClusterRequest();
            clusterRequest.setEnableShipyard(enableShipyard == null ? false : enableShipyard);
            clusterRequest.setName(shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackName() : shellContext.getStackName());
            clusterRequest.setDescription(description);
            clusterRequest.setUserName(userName);
            clusterRequest.setPassword(password);
            clusterRequest.setBlueprintId(Long.valueOf(shellContext.getBlueprintId()));
            clusterRequest.setEmailNeeded(false);
            clusterRequest.setEnableSecurity(enableSecurity);
            clusterRequest.setHostGroups(hostGroupList);

            if (strategy != null) {
                clusterRequest.setConfigStrategy(strategy);
            }

            if (!shellContext.isMarathonMode()) {
                FileSystemRequest fileSystemRequest = new FileSystemRequest();
                fileSystemRequest.setName(shellContext.getStackName());
                fileSystemRequest.setDefaultFs(shellContext.getDefaultFileSystem() == null ? true : shellContext.getDefaultFileSystem());
                fileSystemRequest.setType(shellContext.getFileSystemType());

                if (shellContext.getDefaultFileSystem() == null && shellContext.getFileSystemType() == null) {
                    fileSystemRequest = null;
                }
                clusterRequest.setFileSystem(fileSystemRequest);
            }
            clusterRequest.setKerberosAdmin(kerberosAdmin);
            clusterRequest.setKerberosMasterKey(kerberosMasterKey);
            clusterRequest.setKerberosPassword(kerberosPassword);
            clusterRequest.setLdapRequired(ldapRequired);
            if (shellContext.getSssdConfigId() != null) {
                clusterRequest.setSssdConfigId(Long.valueOf(shellContext.getSssdConfigId()));
            }
            if (shellContext.getRdsConfigId() != null) {
                if (connectionURL != null || connectionUserName != null || connectionPassword != null || databaseType != null || hdpVersion != null) {
                    return "--connectionURL, --databaseType, --connectionUserName, --connectionPassword switches "
                            + "cannot be used if an RDS config is already selected with 'rdsconfig select'";
                }
                clusterRequest.setRdsConfigId(Long.valueOf(shellContext.getRdsConfigId()));
            }
            String ldapConfigId = shellContext.getLdapConfigId();
            if (ldapConfigId != null) {
                clusterRequest.setLdapConfigId(Long.valueOf(ldapConfigId));
            }
            clusterRequest.setValidateBlueprint(false);

            if (ambariVersion != null || ambariRepoBaseURL != null || ambariRepoGpgKey != null) {
                if (ambariVersion == null || ambariRepoBaseURL == null || ambariRepoGpgKey == null) {
                    return "ambariVersion, ambariRepoBaseURL and ambariRepoGpgKey must be set.";
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
                RDSConfigJson rdsConfigJson = new RDSConfigJson();
                rdsConfigJson.setName(clusterRequest.getName());
                rdsConfigJson.setConnectionURL(connectionURL);
                rdsConfigJson.setDatabaseType(databaseType);
                rdsConfigJson.setConnectionUserName(connectionUserName);
                rdsConfigJson.setConnectionPassword(connectionPassword);
                rdsConfigJson.setHdpVersion(hdpVersion);
                rdsConfigJson.setValidated(validated);
                clusterRequest.setRdsConfigJson(rdsConfigJson);
            } else if (connectionURL != null || connectionUserName != null || connectionPassword != null || databaseType != null || hdpVersion != null) {
                return "connectionURL, databaseType, connectionUserName, connectionPassword and hdpVersion must be all set.";
            }

            String stackId = shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackId().toString() : shellContext.getStackId();
            shellContext.cloudbreakClient().clusterEndpoint().post(Long.valueOf(stackId), clusterRequest);
            shellContext.setHint(Hints.NONE);
            shellContext.resetFileSystemConfiguration();
            shellContext.resetAmbariDatabaseDetailsJson();
            if (wait) {
                CloudbreakShellUtil.WaitResult waitResult =
                        cloudbreakShellUtil.waitAndCheckClusterStatus(Long.valueOf(stackId), Status.AVAILABLE.name());
                if (CloudbreakShellUtil.WaitResultStatus.FAILED.equals(waitResult.getWaitResultStatus())) {
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

    @CliAvailabilityIndicator(value = {"cluster stop", "cluster start"})
    public boolean startStopAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable());
    }

    @CliCommand(value = "cluster stop", help = "Stop your cluster")
    public String stop() {
        try {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.STOPPED);
            String stackId = shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackId().toString() : shellContext.getStackId();
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
            String stackId = shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackId().toString() : shellContext.getStackId();
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

    @CliAvailabilityIndicator(value = {"cluster delete"})
    public boolean deleteAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable());
    }

    @CliCommand(value = "cluster delete", help = "Delete the cluster by stack id")
    public String delete() {
        try {
            String stackId = shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackId().toString() : shellContext.getStackId();
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

    @CliAvailabilityIndicator(value = {"cluster show", "cluster show --id", "cluster show --name"})
    @Override
    public boolean showAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable());
    }

    @CliCommand(value = "cluster show", help = "Shows the cluster by stack id")
    public String show() {
        try {
            String stackId = shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackId().toString() : shellContext.getStackId();
            ClusterResponse clusterResponse = shellContext.cloudbreakClient().clusterEndpoint().get(Long.valueOf(stackId));
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(clusterResponse), "FIELD", "VALUE");
        } catch (IndexOutOfBoundsException ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("There was no cluster for this account.");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public String show(Long id, String name) throws Exception {
        throw new MethodNotSupportedException("Cluster show command not available");
    }

    @Override
    public String showById(Long id) throws Exception {
        return show(id, null);
    }

    @Override
    public String showByName(String name) throws Exception {
        return show(null, name);
    }

    @CliAvailabilityIndicator(value = {"cluster node --ADD", "cluster node --REMOVE"})
    public boolean nodeAvailable() {
        return shellContext.isStackAvailable() || (shellContext.isMarathonMode() && shellContext.isSelectedMarathonStackAvailable());
    }

    @CliAvailabilityIndicator(value = {"cluster fileSystem"})
    public boolean fileSystemAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode();
    }

    @CliCommand(value = "cluster node --ADD", help = "Add new nodes to the cluster")
    public String addNode(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be added to the cluster") Integer adjustment) {
        try {
            if (adjustment < 1) {
                return "The adjustment value in case of node addition should be at least 1.";
            }
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setScalingAdjustment(adjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            String stackId = shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackId().toString() : shellContext.getStackId();
            cloudbreakShellUtil.checkResponse("upscaleCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(Long.valueOf(stackId), updateClusterJson));
            return "Cluster upscale started with stack id: " + shellContext.getStackId();
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster node --REMOVE", help = "Remove nodes from the cluster")
    public String removeNode(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "The number of the nodes to be removed from the cluster.") Integer adjustment,
            @CliOption(key = "withStackDownScale", help = "Do the downscale with the stack together") Boolean withStackDownScale) {
        try {
            if (adjustment > -1) {
                return "The adjustment value in case of node removal should be negative.";
            }
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setScalingAdjustment(adjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(withStackDownScale == null ? false : withStackDownScale);
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            String stackId = shellContext.isMarathonMode() ? shellContext.getSelectedMarathonStackId().toString() : shellContext.getStackId();
            cloudbreakShellUtil.checkResponse("downscaleCluster",
                    shellContext.cloudbreakClient().clusterEndpoint().put(Long.valueOf(stackId), updateClusterJson));
            return "Cluster downscale started with stack id: " + shellContext.getStackId();
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = "cluster sync")
    public boolean syncAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode();
    }

    @CliCommand(value = "cluster sync", help = "Sync the cluster")
    public String sync() {
        try {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.SYNC);
            shellContext.cloudbreakClient().clusterEndpoint().put(Long.valueOf(shellContext.getStackId()), updateClusterJson);
            return "Cluster is syncing";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

}
