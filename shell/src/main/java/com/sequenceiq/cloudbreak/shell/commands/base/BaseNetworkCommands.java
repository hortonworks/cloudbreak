package com.sequenceiq.cloudbreak.shell.commands.base;

import static com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource;

import java.util.Map;
import java.util.Set;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BaseNetworkCommands implements BaseCommands, NetworkCommands {
    private static final String CREATE_SUCCESS_MSG = "Network created and selected successfully, with id: '%s' and name: '%s'";

    private ShellContext shellContext;

    public BaseNetworkCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = { "network delete --id", "network delete --name" })
    @Override
    public boolean deleteAvailable() {
        return !shellContext.getNetworksByProvider().isEmpty() && !shellContext.isMarathonMode();
    }

    @CliCommand(value = "network delete --id", help = "Delete the network by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "network delete --name", help = "Delete the network by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        try {
            Long networkId = id == null ? null : id;
            String networkName = name == null ? null : name;
            if (networkId != null) {
                shellContext.cloudbreakClient().networkEndpoint().delete(networkId);
                refreshNetworksInContext();
                return String.format("Network deleted with %s id", networkId);
            } else if (networkName != null) {
                shellContext.cloudbreakClient().networkEndpoint().deletePublic(networkName);
                refreshNetworksInContext();
                return String.format("Network deleted with %s name", networkName);
            }
            return "No network specified.";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "network select --id", "network select --name" })
    @Override
    public boolean selectAvailable() {
        return !shellContext.getNetworksByProvider().isEmpty() && !shellContext.isMarathonMode();
    }

    @CliCommand(value = "network select --id", help = "Delete the network by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "network select --name", help = "Delete the network by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @Override
    public String select(Long idOfNetwork, String networkName) {
        try {
            String msg = "Network not found.";
            if (idOfNetwork != null && shellContext.getNetworksByProvider().containsKey(idOfNetwork)) {
                String provider = shellContext.getNetworksByProvider().get(idOfNetwork);
                createHintAndAddNetworkToContext(idOfNetwork, provider);
                msg = "Network is selected with id: " + idOfNetwork;
            } else if (networkName != null) {
                NetworkResponse aPublic = shellContext.cloudbreakClient().networkEndpoint().getPublic(networkName);
                if (aPublic != null) {
                    createHintAndAddNetworkToContext(Long.valueOf(aPublic.getId()), aPublic.getCloudPlatform());
                    msg = "Network is selected with name: " + networkName;
                }
            }
            return msg;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = "network list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "network list", help = "Shows the currently available networks configurations")
    @Override
    public String list() throws Exception {
        try {
            Set<NetworkResponse> publics = shellContext.cloudbreakClient().networkEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "network show --id", "network show --name" })
    @Override
    public boolean showAvailable() {
        return !shellContext.getNetworksByProvider().isEmpty() && !shellContext.isMarathonMode();
    }

    @CliCommand(value = "network show --id", help = "Show the network by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "network show --name", help = "Show the network by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @Override
    public String show(Long id, String name) throws Exception {
        try {
            if (id != null) {
                NetworkResponse networkResponse = shellContext.cloudbreakClient().networkEndpoint().get(id);
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(networkResponse), "FIELD", "VALUE");
            } else if (name != null) {
                NetworkResponse aPublic = shellContext.cloudbreakClient().networkEndpoint().getPublic(name);
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE");
            }
            return "Network could not be found!";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public boolean createNetworkAvailable(String platform) {
        return !shellContext.isMarathonMode();
    }

    @Override
    public String create(String name, String subnet, Boolean publicInAccount, String description, Long platformId, Map<String, Object> parameters,
            String platform) {
        try {
            NetworkRequest networkRequest = new NetworkRequest();
            networkRequest.setName(name);
            networkRequest.setDescription(description);
            networkRequest.setCloudPlatform(platform);
            networkRequest.setParameters(parameters);
            networkRequest.setSubnetCIDR(subnet);
            if (platformId != null) {
                checkTopologyForResource(shellContext.cloudbreakClient().topologyEndpoint().getPublics(), platformId, platform);
            }
            networkRequest.setTopologyId(platformId);

            Long id;
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().networkEndpoint().postPublic(networkRequest).getId();
            } else {
                id = shellContext.cloudbreakClient().networkEndpoint().postPrivate(networkRequest).getId();
            }
            createHintAndAddNetworkToContext(id, platform);
            return String.format(CREATE_SUCCESS_MSG, id, name);
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private void createHintAndAddNetworkToContext(Long id, String provider) {
        shellContext.setHint(Hints.CREATE_STACK);
        shellContext.putNetwork(id, provider);
        shellContext.setActiveNetworkId(id);
    }

    private void refreshNetworksInContext() {
        shellContext.getNetworksByProvider().clear();
        Set<NetworkResponse> publics = shellContext.cloudbreakClient().networkEndpoint().getPublics();
        for (NetworkResponse network : publics) {
            shellContext.putNetwork(Long.valueOf(network.getId()), network.getCloudPlatform());
        }
        if (!shellContext.getNetworksByProvider().containsKey(shellContext.getActiveNetworkId())) {
            shellContext.setActiveNetworkId(null);
        }
    }

}
