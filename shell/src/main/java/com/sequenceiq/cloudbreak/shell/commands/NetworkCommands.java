package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.cloudbreak.shell.completion.NetworkId;
import com.sequenceiq.cloudbreak.shell.completion.NetworkName;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class NetworkCommands implements CommandMarker {
    private static final String CREATE_SUCCESS_MSG = "Network created and selected successfully, with id: '%s'";

    @Autowired
    private CloudbreakContext context;
    @Autowired
    private CloudbreakClient cloudbreakClient;
    @Autowired
    private ResponseTransformer responseTransformer;

    @CliAvailabilityIndicator({ "network list", "network create --AWS", "network create --AZURE", "network create --GCP", "network create --OPENSTACK" })
    public boolean areNetworkCommandsAvailable() {
        return true;
    }

    @CliAvailabilityIndicator({ "network delete", "network select", "network show" })
    public boolean isNetworkDeleteCommandAvailable() {
        return !context.getNetworksByProvider().isEmpty();
    }

    @CliCommand(value = "network list", help = "Shows the currently available networks configurations")
    public String listNetworks() {
        try {
            Set<NetworkJson> publics = cloudbreakClient.networkEndpoint().getPublics();
            return renderSingleMap(responseTransformer.transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @CliCommand(value = "network create --AWS", help = "Create a new AWS network configuration")
    public String createAwsNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "vpcID", mandatory = false, help = "The ID of the virtual private cloud (VPC)") String vpcId,
            @CliOption(key = "internetGatewayID", mandatory = false,
                    help = "The ID of the internet gateway that is attached to the VPC (configured via 'vpcID' option)") String internetGatewayId,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description
    ) {
        try {
            NetworkJson networkJson = new NetworkJson();
            networkJson.setName(name);
            networkJson.setDescription(description);
            networkJson.setPublicInAccount(publicInAccount == null ? false : publicInAccount);
            networkJson.setCloudPlatform("AWS");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("vpcID", vpcId);
            parameters.put("internetGatewayID", internetGatewayId);

            networkJson.setParameters(parameters);
            networkJson.setSubnetCIDR(subnet);

            IdJson id;
            if (publicInAccount == null || !publicInAccount) {
                id = cloudbreakClient.networkEndpoint().postPrivate(networkJson);
            } else {
                id = cloudbreakClient.networkEndpoint().postPublic(networkJson);
            }
            createHintAndAddNetworkToContext(id.getId().toString(), "AWS");
            return String.format(CREATE_SUCCESS_MSG, id);
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "network create --AZURE", help = "Create a new AZURE network configuration")
    public String createAzureNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "addressPrefix", mandatory = true, help = "The address prefix of the Azure virtual network in CIDR format") String addressPrefix,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description
    ) {
        try {
            NetworkJson networkJson = new NetworkJson();
            networkJson.setName(name);
            networkJson.setDescription(description);
            networkJson.setPublicInAccount(publicInAccount == null ? false : publicInAccount);
            networkJson.setCloudPlatform("AWS");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("addressPrefix", addressPrefix);

            networkJson.setParameters(parameters);
            networkJson.setSubnetCIDR(subnet);

            IdJson id;
            if (publicInAccount == null || !publicInAccount) {
                id = cloudbreakClient.networkEndpoint().postPrivate(networkJson);
            } else {
                id = cloudbreakClient.networkEndpoint().postPublic(networkJson);
            }
            createHintAndAddNetworkToContext(id.getId().toString(), "AZURE_RM");
            return String.format(CREATE_SUCCESS_MSG, id.getId());
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "network create --GCP", help = "Create a new GCP network configuration")
    public String createGcpNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description
    ) {
        try {
            NetworkJson networkJson = new NetworkJson();
            networkJson.setName(name);
            networkJson.setDescription(description);
            networkJson.setPublicInAccount(publicInAccount == null ? false : publicInAccount);
            networkJson.setCloudPlatform("AWS");

            Map<String, Object> parameters = new HashMap<>();

            networkJson.setParameters(parameters);
            networkJson.setSubnetCIDR(subnet);

            IdJson id;
            if (publicInAccount == null || !publicInAccount) {
                id = cloudbreakClient.networkEndpoint().postPrivate(networkJson);
            } else {
                id = cloudbreakClient.networkEndpoint().postPublic(networkJson);
            }
            createHintAndAddNetworkToContext(id.getId().toString(), "GCP");
            return String.format(CREATE_SUCCESS_MSG, id.getId());
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "network create --OPENSTACK", help = "Create a new OpenStack network configuration")
    public String createOpenStackNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "publicNetID", mandatory = true, help = "ID of the available and desired OpenStack public network") String publicNetID,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description
    ) {
        try {
            NetworkJson networkJson = new NetworkJson();
            networkJson.setName(name);
            networkJson.setDescription(description);
            networkJson.setPublicInAccount(publicInAccount == null ? false : publicInAccount);
            networkJson.setCloudPlatform("AWS");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("publicNetID", publicNetID);

            networkJson.setParameters(parameters);
            networkJson.setSubnetCIDR(subnet);

            IdJson id;
            if (publicInAccount == null || !publicInAccount) {
                id = cloudbreakClient.networkEndpoint().postPrivate(networkJson);
            } else {
                id = cloudbreakClient.networkEndpoint().postPublic(networkJson);
            }
            createHintAndAddNetworkToContext(id.getId().toString(), "OPENSTACK");
            return String.format(CREATE_SUCCESS_MSG, id.getId());
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "network select", help = "Select network by the given id or name")
    public String selectNetwork(
            @CliOption(key = "id", mandatory = false, help = "Id of the network") NetworkId idOfNetwork,
            @CliOption(key = "name", mandatory = false, help = "Name of the network") NetworkName networkName) {
        try {
            String msg = "Network could not be found.";
            String id = idOfNetwork == null ? null : idOfNetwork.getName();
            String name = networkName == null ? null : networkName.getName();
            if (id != null && context.getNetworksByProvider().containsKey(id)) {
                String provider = context.getNetworksByProvider().get(id);
                createHintAndAddNetworkToContext(id, provider);
                msg = "Network is selected with id: " + id;
            } else if (name != null) {
                NetworkJson aPublic = cloudbreakClient.networkEndpoint().getPublic(name);
                if (aPublic != null) {
                    createHintAndAddNetworkToContext(aPublic.getId(), name);
                    msg = "Network is selected with name: " + name;
                }
            }
            return msg;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "network delete", help = "Delete network by the given id or name")
    public Object deleteNetwork(
            @CliOption(key = "id", mandatory = false, help = "Id of the network") NetworkId networkId,
            @CliOption(key = "name", mandatory = false, help = "Name of the network") NetworkName networkName) {
        try {
            String id = networkId == null ? null : networkId.getName();
            String name = networkName == null ? null : networkName.getName();
            if (id != null) {
                cloudbreakClient.networkEndpoint().delete(Long.valueOf(id));
                refreshNetworksInContext();
                return String.format("Network deleted with %s id", id);
            } else if (name != null) {
                cloudbreakClient.networkEndpoint().deletePublic(name);
                refreshNetworksInContext();
                return String.format("Network deleted with %s name", name);
            }
            return "No network specified.";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "network show", help = "Shows the network by its id or name")
    public Object showNetwork(
            @CliOption(key = "id", mandatory = false, help = "Id of the network") NetworkId networkId,
            @CliOption(key = "name", mandatory = false, help = "Name of the network") NetworkName networkName) {
        try {
            String id = networkId == null ? null : networkId.getName();
            String name = networkName == null ? null : networkName.getName();
            if (id != null) {
                NetworkJson networkJson = cloudbreakClient.networkEndpoint().get(Long.valueOf(id));
                return renderSingleMap(responseTransformer.transformObjectToStringMap(networkJson), "FIELD", "VALUE");
            } else if (name != null) {
                NetworkJson aPublic = cloudbreakClient.networkEndpoint().getPublic(name);
                return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
            }
            return "Network could not be found!";
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private Map<String, String> getNetworkFieldsAsSingleStringMap(Map<String, Object> networkFields) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> networkField : networkFields.entrySet()) {
            if ("parameters".equals(networkField.getKey())) {
                for (Map.Entry<String, Object> parameter : ((Map<String, Object>) networkField.getValue()).entrySet()) {
                    if (parameter.getValue() != null) {
                        result.put(parameter.getKey(), parameter.getValue().toString());
                    }
                }
            } else {
                result.put(networkField.getKey(), networkField.getValue().toString());
            }
        }
        return result;
    }

    private void createHintAndAddNetworkToContext(String id, String provider) throws Exception {
        context.setHint(Hints.SELECT_SECURITY_GROUP);
        context.putNetwork(id, provider);
        context.setActiveNetworkId(id);
    }

    private void refreshNetworksInContext() throws Exception {
        context.getNetworksByProvider().clear();
        Set<NetworkJson> publics = cloudbreakClient.networkEndpoint().getPublics();
        for (NetworkJson network : publics) {
            context.putNetwork(network.getId(), network.getCloudPlatform());
        }
        if (!context.getNetworksByProvider().containsKey(context.getActiveNetworkId())) {
            context.setActiveNetworkId(null);
        }
    }
}
