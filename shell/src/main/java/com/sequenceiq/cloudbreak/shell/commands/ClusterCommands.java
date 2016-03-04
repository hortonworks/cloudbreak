package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.MarathonContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil;

@Component
public class ClusterCommands implements CommandMarker {

    @Inject
    private CloudbreakContext context;
    @Inject
    private MarathonContext marathonContext;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ResponseTransformer responseTransformer;
    @Inject
    private CloudbreakShellUtil cloudbreakShellUtil;
    @Inject
    private ExceptionTransformer exceptionTransformer;

    @CliAvailabilityIndicator(value = "cluster create")
    public boolean isClusterCreateCommandAvailable() {
        return context.isBlueprintAvailable()
                && ((context.isStackAvailable() && context.getActiveHostGroups().size() == context.getHostGroups().keySet().size())
                        || (context.isMarathonMode() && marathonContext.isSelectedMarathonStackAvailable()
                && context.getActiveHostGroups().size() == marathonContext.getHostgroups().size()));
    }

    @CliAvailabilityIndicator(value = { "cluster show", "cluster stop", "cluster start" })
    public boolean isClusterShowCommandAvailable() {
        return context.isStackAvailable() || (context.isMarathonMode() && marathonContext.isSelectedMarathonStackAvailable());
    }

    @CliAvailabilityIndicator({ "cluster node --ADD", "cluster node --REMOVE" })
    public boolean isClusterNodeCommandAvailable() {
        return context.isStackAvailable() || (context.isMarathonMode() && marathonContext.isSelectedMarathonStackAvailable());
    }

    @CliAvailabilityIndicator({ "cluster fileSystem --DASH", "cluster fileSystem --GCS", "cluster fileSystem --WASB" })
    public boolean isClusterFileSystemCommandAvailable() {
        return context.isStackAvailable() && !context.isMarathonMode();
    }

    @CliCommand(value = "cluster fileSystem --DASH", help = "Set Windows Azure Blob Storage filesystem with DASH on cluster")
    public String setAzureRmFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default filesystem") Boolean defaultFileSystem,
            @CliOption(key = "accountName", mandatory = true, help = "accountName of the DASH service") String accountName,
            @CliOption(key = "accountKey", mandatory = true, help = "access key of the DASH service") String accountKey) {
        context.setDefaultFileSystem(defaultFileSystem);
        context.setFileSystemType(FileSystemType.DASH);
        Map<String, Object> props = new HashMap<>();
        props.put("accountName", accountName);
        props.put("accountKey", accountKey);
        context.setFileSystemParameters(props);
        return "Windows Azure Blob Storage with DASH configured as the filesystem";
    }

    @CliCommand(value = "cluster fileSystem --WASB", help = "Set Windows Azure Blob Storage filesystem on cluster")
    public String setWasbFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default filesystem") Boolean defaultFileSystem,
            @CliOption(key = "accountName", mandatory = true, help = "name of the storage account") String accountName,
            @CliOption(key = "accountKey", mandatory = true, help = "primary access key to the storage account") String accountKey) {
        context.setDefaultFileSystem(defaultFileSystem);
        context.setFileSystemType(FileSystemType.WASB);
        Map<String, Object> props = new HashMap<>();
        props.put("accountName", accountName);
        props.put("accountKey", accountKey);
        context.setFileSystemParameters(props);
        return "Windows Azure Blob Storage filesystem configured";
    }


    @CliCommand(value = "cluster fileSystem --GCS", help = "Set GCS fileSystem on cluster")
    public String setGcsFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default fileSystem") Boolean defaultFileSystem,
            @CliOption(key = "projectId", mandatory = true, help = "projectId of the GCS") String projectId,
            @CliOption(key = "serviceAccountEmail", mandatory = true, help = "serviceAccountEmail of the GCS") String serviceAccountEmail,
            @CliOption(key = "privateKeyEncoded", mandatory = true, help = "privateKeyEncoded of the GCS") String privateKeyEncoded,
            @CliOption(key = "defaultBucketName", mandatory = true, help = "defaultBucketName of the GCS") String defaultBucketName) {
        context.setDefaultFileSystem(defaultFileSystem);
        context.setFileSystemType(FileSystemType.GCS);
        Map<String, Object> props = new HashMap<>();
        props.put("projectId", projectId);
        props.put("serviceAccountEmail", serviceAccountEmail);
        props.put("privateKeyEncoded", privateKeyEncoded);
        props.put("defaultBucketName", defaultBucketName);
        context.setFileSystemParameters(props);
        return "GCS filesystem configured";
    }

    @CliCommand(value = "cluster show", help = "Shows the cluster by stack id")
    public String configCluster() {
        try {
            String stackId = context.isMarathonMode() ? marathonContext.getSelectedMarathonStackId().toString() : context.getStackId();
            ClusterResponse clusterResponse = cloudbreakClient.clusterEndpoint().get(Long.valueOf(stackId));
            return renderSingleMap(responseTransformer.transformObjectToStringMap(clusterResponse), "FIELD", "VALUE");
        } catch (IndexOutOfBoundsException ex) {
            throw exceptionTransformer.transformToRuntimeException("There was no cluster for this account.");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster node --ADD", help = "Add new nodes to the cluster")
    public String addNodeToCluster(
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
            String stackId = context.isMarathonMode() ? marathonContext.getSelectedMarathonStackId().toString() : context.getStackId();
            cloudbreakClient.clusterEndpoint().put(Long.valueOf(stackId), updateClusterJson);
            return context.getStackId();
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster node --REMOVE", help = "Remove nodes from the cluster")
    public String removeNodeToCluster(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "The number of the nodes to be removed from the cluster.") Integer adjustment,
            @CliOption(key = "withStackDownScale", mandatory = false, help = "Do the downscale with the stack together") Boolean withStackDownScale) {
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
            String stackId = context.isMarathonMode() ? marathonContext.getSelectedMarathonStackId().toString() : context.getStackId();
            cloudbreakClient.clusterEndpoint().put(Long.valueOf(stackId), updateClusterJson);
            return context.getStackId();
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster create", help = "Create a new cluster based on a blueprint and optionally a recipe")
    public String createCluster(
            @CliOption(key = "userName", mandatory = false, unspecifiedDefaultValue = "admin", help = "Username of the Ambari server") String userName,
            @CliOption(key = "password", mandatory = false, unspecifiedDefaultValue = "admin", help = "Password of the Ambari server") String password,
            @CliOption(key = "description", mandatory = false, help = "Description of the blueprint") String description,
            @CliOption(key = "stack", mandatory = false, help = "Stack definition name, like HDP") String stack,
            @CliOption(key = "version", mandatory = false, help = "Stack definition version") String version,
            @CliOption(key = "os", mandatory = false, help = "Stack OS to select package manager, default is RedHat") String os,
            @CliOption(key = "stackRepoId", mandatory = false, help = "Stack repository id") String stackRepoId,
            @CliOption(key = "stackBaseURL", mandatory = false, help = "Stack url") String stackBaseURL,
            @CliOption(key = "utilsRepoId", mandatory = false, help = "Stack utils repoId") String utilsRepoId,
            @CliOption(key = "utilsBaseURL", mandatory = false, help = "Stack utils URL") String utilsBaseURL,
            @CliOption(key = "verify", mandatory = false, help = "Whether to verify the URLs or not") Boolean verify,
            @CliOption(key = "enableSecurity", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false",
                    help = "Kerberos security status") Boolean enableSecurity,
            @CliOption(key = "kerberosMasterKey", mandatory = false, specifiedDefaultValue = "key", help = "Kerberos mater key") String kerberosMasterKey,
            @CliOption(key = "kerberosAdmin", mandatory = false, specifiedDefaultValue = "admin", help = "Kerberos admin name") String kerberosAdmin,
            @CliOption(key = "kerberosPassword", mandatory = false, specifiedDefaultValue = "admin", help = "Kerberos admin password") String kerberosPassword,
            @CliOption(key = "ldapRequired", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "Start and configure LDAP authentication support for Ambari hosts") Boolean ldapRequired,
            @CliOption(key = "configStrategy", mandatory = false, help = "Config recommendation strategy") ConfigStrategy strategy,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") Boolean wait) {
        try {
            Set<HostGroupJson> hostGroupList = new HashSet<>();
            Set<Map.Entry<String, HostgroupEntry>> entries = context.isMarathonMode()
                    ? marathonContext.getHostgroups().entrySet() : context.getHostGroups().entrySet();
            for (Map.Entry<String, HostgroupEntry> entry : entries) {
                HostGroupJson hostGroupJson = new HostGroupJson();
                hostGroupJson.setRecipeIds(entry.getValue().getRecipeIdSet());
                hostGroupJson.setName(entry.getKey());

                ConstraintJson constraintJson = new ConstraintJson();

                constraintJson.setHostCount(entry.getValue().getNodeCount());
                if (context.isMarathonMode()) {
                    constraintJson.setConstraintTemplateName(entry.getValue().getConstraintName());
                } else {
                    constraintJson.setInstanceGroupName(entry.getKey());
                }

                hostGroupJson.setConstraint(constraintJson);
                hostGroupJson.setRecipeIds(entry.getValue().getRecipeIdSet());
                hostGroupList.add(hostGroupJson);
            }

            wait = wait == null ? false : wait;
            ClusterRequest clusterRequest = new ClusterRequest();
            clusterRequest.setName(context.isMarathonMode() ? marathonContext.getSelectedMarathonStackName() : context.getStackName());
            clusterRequest.setDescription(description);
            clusterRequest.setUserName(userName);
            clusterRequest.setPassword(password);
            clusterRequest.setBlueprintId(Long.valueOf(context.getBlueprintId()));
            clusterRequest.setEmailNeeded(false);
            clusterRequest.setEnableSecurity(enableSecurity);
            clusterRequest.setHostGroups(hostGroupList);

            if (strategy != null) {
                clusterRequest.setConfigStrategy(strategy);
            }

            if (!context.isMarathonMode()) {
                FileSystemRequest fileSystemRequest = new FileSystemRequest();
                fileSystemRequest.setName(context.getStackName());
                fileSystemRequest.setDefaultFs(context.getDefaultFileSystem() == null ? true : context.getDefaultFileSystem());
                fileSystemRequest.setType(context.getFileSystemType());

                if (context.getDefaultFileSystem() == null && context.getFileSystemType() == null) {
                    fileSystemRequest = null;
                }
                clusterRequest.setFileSystem(fileSystemRequest);
            }
            clusterRequest.setKerberosAdmin(kerberosAdmin);
            clusterRequest.setKerberosMasterKey(kerberosMasterKey);
            clusterRequest.setKerberosPassword(kerberosPassword);
            clusterRequest.setLdapRequired(ldapRequired);
            if (context.getSssdConfigId() != null) {
                clusterRequest.setSssdConfigId(Long.valueOf(context.getSssdConfigId()));
            }
            clusterRequest.setValidateBlueprint(false);

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

            String stackId = context.isMarathonMode() ? marathonContext.getSelectedMarathonStackId().toString() : context.getStackId();
            cloudbreakClient.clusterEndpoint().post(Long.valueOf(stackId), clusterRequest);
            context.setHint(Hints.NONE);
            context.resetFileSystemConfiguration();
            if (wait) {
                CloudbreakShellUtil.WaitResult waitResult =
                        cloudbreakShellUtil.waitAndCheckClusterStatus(Long.valueOf(stackId), Status.AVAILABLE.name());
                if (CloudbreakShellUtil.WaitResult.FAILED.equals(waitResult)) {
                    throw exceptionTransformer.transformToRuntimeException("Cluster creation failed on stack with id: " + stackId);
                } else {
                    return "Cluster creation finished";
                }
            }
            return "Cluster creation started";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster stop", help = "Stop your cluster")
    public String stopCluster() {
        try {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.STOPPED);
            String stackId = context.isMarathonMode() ? marathonContext.getSelectedMarathonStackId().toString() : context.getStackId();
            cloudbreakClient.clusterEndpoint().put(Long.valueOf(stackId), updateClusterJson);
            return "Cluster is stopping";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "cluster start", help = "Start your cluster")
    public String startCluster() {
        try {
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setStatus(StatusRequest.STARTED);
            String stackId = context.isMarathonMode() ? marathonContext.getSelectedMarathonStackId().toString() : context.getStackId();
            cloudbreakClient.stackEndpoint().put(Long.valueOf(stackId), updateStackJson);
            return "Cluster is starting";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }
}
