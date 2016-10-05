package com.sequenceiq.cloudbreak.shell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.event.ShellStatus;
import org.springframework.shell.event.ShellStatusListener;

import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak.shell")
public class CloudbreakShell implements CommandLineRunner, ShellStatusListener {

    public static final String DOLLAR = "$";
    public static final String SPACE = " ";
    public static final String EMPTY = "";
    public static final String FAILED = "FAILED";
    public static final String SUCCESS = "SUCCESS";

    @Inject
    private CommandLine commandLine;
    @Inject
    private JLineShellComponent shell;
    @Inject
    private ShellContext context;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ResponseTransformer responseTransformer;

    @Value("${sequenceiq.user:}")
    private String user;

    @Value("${sequenceiq.password:}")
    private String password;

    @Override
    public void run(String... arg) throws Exception {
        if ("".equals(user)) {
            System.out.println("Missing 'sequenceiq.user' parameter!");
            return;
        }
        if ("".equals(password)) {
            System.out.println("Missing 'sequenceiq.password' parameter!");
            return;
        }
        String[] shellCommandsToExecute = commandLine.getShellCommandsToExecute();
        if (shellCommandsToExecute != null) {
            init();
            for (String cmd : shellCommandsToExecute) {
                String replacedCommand = getReplacedString(cmd);
                CommandResult commandResult = shell.executeCommand(replacedCommand);
                if (!commandResult.isSuccess()) {
                    String message =
                            Optional.ofNullable(commandResult.getException()).map(Throwable::getMessage).orElse("Unknown error, maybe command not valid.");
                    System.out.println(String.format("%s: [%s] [REASON: %s]", replacedCommand, FAILED, message));
                    break;
                } else {
                    System.out.println(String.format("%s: [%s]", replacedCommand, SUCCESS));
                }
            }
        } else {
            shell.addShellStatusListener(this);
            shell.start();
            shell.promptLoop();
            shell.waitForComplete();
        }
    }

    private String getReplacedString(String cmd) {
        String result = cmd;
        if (result != null) {
            for (String split : cmd.split(SPACE)) {
                if (split.startsWith(DOLLAR)) {
                    result = result.replace(split, System.getenv(split.replace(DOLLAR, EMPTY)));
                }
            }
        }
        return result;
    }

    @Override
    public void onShellStatusChange(ShellStatus oldStatus, ShellStatus newStatus) {
        if (newStatus.getStatus() == ShellStatus.Status.STARTED) {
            try {
                init();
            } catch (Exception e) {
                System.out.println("Can't connect to Cloudbreak");
                e.printStackTrace();
                shell.executeCommand("quit");
            }
        }
    }

    private void init() {
        //cloudbreak.health();
        initResourceAccessibility();
        initPlatformVariants();
        if (!context.isCredentialAccessible()) {
            context.setHint(Hints.CREATE_CREDENTIAL);
        } else {
            context.setHint(Hints.SELECT_CREDENTIAL);
        }
    }

    public static void main(String[] args) {

        if ((args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) || args.length == 0) {
            System.out.println(
                    "\nCloudbreak Shell: Interactive command line tool for managing Cloudbreak.\n\n"
                            + "Usage:\n"
                            + "  java -jar cloudbreak-shell.jar                  : Starts Cloudbreak Shell in interactive mode.\n"
                            + "  java -jar cloudbreak-shell.jar --cmdfile=<FILE> : Cloudbreak Shell executes commands read from the file.\n\n"
                            + "Options:\n"
                            + "  --cloudbreak.address=http[s]://<HOSTNAME>[:PORT]  Address of the Cloudbreak Server\n"
                            + "  --identity.address=http[s]://<HOSTNAME>[:PORT]    Address of the SequenceIQ identity server (not a mandatory parameter)"
                            + " [default: cloudbreak.address + /identity].\n"
                            + "  --sequenceiq.user=<USER>                          Username of the SequenceIQ user.\n"
                            + "  --sequenceiq.password=<PASSWORD>                  Password of the SequenceIQ user.\n"
                            + "  --cert.validation=<boolean>                       Validate SSL certificates, shall be disabled for self signed certificates"
                            + " (not a mandatory parameter) [default: true]."
            );
        } else {
            if (!VersionedApplication.versionedApplication().showVersionInfo(args)) {
                try {
                    new SpringApplicationBuilder(CloudbreakShell.class).web(false).bannerMode(Banner.Mode.OFF).run(args);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Cloudbreak shell cannot be started.");
                }
            }
        }
    }

    private void initResourceAccessibility() {
        initCredentialAccessibility();
        initBlueprintAccessibility();
        initStackAccessibility();
        initRecipeAccessibility();
        initSssdConfigAccessibility();
        initRdsConfigAccessibility();
        Set<NetworkJson> publics = cloudbreakClient.networkEndpoint().getPublics();
        for (NetworkJson network : publics) {
            context.putNetwork(Long.valueOf(network.getId()), network.getCloudPlatform());
        }
        Set<SecurityGroupJson> securityGroups = cloudbreakClient.securityGroupEndpoint().getPublics();
        for (SecurityGroupJson securityGroup : securityGroups) {
            context.putSecurityGroup(securityGroup.getId(), securityGroup.getName());
        }
        Set<RDSConfigResponse> rdsConfigResponses = cloudbreakClient.rdsConfigEndpoint().getPublics();
        for (RDSConfigResponse rdsConfig: rdsConfigResponses) {
            context.putRdsConfig(Long.valueOf(rdsConfig.getId()), rdsConfig.getName());
        }
    }

    private void initCredentialAccessibility() {
        if (!cloudbreakClient.credentialEndpoint().getPublics().isEmpty()) {
            context.setCredentialAccessible();
        }
    }

    private void initBlueprintAccessibility() {
        if (!cloudbreakClient.blueprintEndpoint().getPublics().isEmpty()) {
            context.setBlueprintAccessible();
        }
    }

    private void initStackAccessibility() {
        if (!cloudbreakClient.stackEndpoint().getPublics().isEmpty()) {
            context.setStackAccessible();
        }
    }

    private void initRecipeAccessibility() {
        if (!cloudbreakClient.recipeEndpoint().getPublics().isEmpty()) {
            context.setRecipeAccessible();
        }
    }

    private void initSssdConfigAccessibility() {
        if (!cloudbreakClient.sssdConfigEndpoint().getPublics().isEmpty()) {
            context.setSssdConfigAccessible();
        }
    }

    private void initRdsConfigAccessibility() {
        if (!cloudbreakClient.rdsConfigEndpoint().getPublics().isEmpty()) {
            context.setRdsConfigAccessible();
        }
    }

    private void initPlatformVariants() {
        Map<String, Collection<String>> platformToVariants = Collections.EMPTY_MAP;
        Map<String, Collection<String>> regions = Collections.EMPTY_MAP;
        Map<String, Collection<String>> volumeTypes = Collections.EMPTY_MAP;
        Map<String, Map<String, Collection<String>>> availabilityZones = Collections.EMPTY_MAP;
        Map<String, List<Map<String, String>>> instanceTypes = new HashMap<>();
        Map<String, Collection<String>> orchestrators = new HashMap<>();
        try {
            platformToVariants = cloudbreakClient.connectorEndpoint().getPlatformVariants().getPlatformToVariants();
            regions = cloudbreakClient.connectorEndpoint().getRegions().getRegions();
            availabilityZones = cloudbreakClient.connectorEndpoint().getRegions().getAvailabilityZones();
            volumeTypes = cloudbreakClient.connectorEndpoint().getDisktypes().getDiskTypes();
            orchestrators = cloudbreakClient.connectorEndpoint().getOrchestratortypes().getOrchestrators();
            Map<String, Collection<VmTypeJson>> virtualMachines = cloudbreakClient.connectorEndpoint().getVmTypes(true).getVirtualMachines();
            for (Map.Entry<String, Collection<VmTypeJson>> vmCloud : virtualMachines.entrySet()) {
                List<Map<String, String>> tmp = new ArrayList<>();
                for (VmTypeJson vmTypeJson : vmCloud.getValue()) {
                    Map<String, String> map = responseTransformer.transformObjectToStringMap(vmTypeJson);
                    tmp.add(map);
                }
                instanceTypes.put(vmCloud.getKey(), tmp);
            }
        } catch (Exception e) {
            System.out.println("Error during retrieving platform variants");
        } finally {
            context.setPlatformToVariantsMap(platformToVariants);
            context.setRegions(regions);
            context.setAvailabilityZones(availabilityZones);
            context.setVolumeTypes(volumeTypes);
            context.setInstanceTypes(instanceTypes);
            context.setOrchestrators(orchestrators);
        }
    }

}
