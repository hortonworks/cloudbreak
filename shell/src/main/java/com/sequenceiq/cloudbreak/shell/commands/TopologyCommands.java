package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.TopologyRequest;
import com.sequenceiq.cloudbreak.api.model.TopologyResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;


@Component
public class TopologyCommands implements CommandMarker {
    private static final String CREATE_SUCCESS_MSG = "Topology created and selected successfully, with id: '%s'";

    @Inject
    private CloudbreakContext context;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ResponseTransformer responseTransformer;
    @Inject
    private ExceptionTransformer exceptionTransformer;

    @CliAvailabilityIndicator(value = "topology list")
    public boolean isTopologyListCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "topology show")
    public boolean isTopologyShowCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "topology delete")
    public boolean isTopologyDeleteCommandAvailable() {
        return true;
    }


    @CliCommand(value = "topology create --AWS", help = "Create a new AWS topology configuration")
    public String createAwsTopology(
            @CliOption(key = "name", mandatory = true, help = "Name of the topology") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the topology") String description
    ) {
        try {
            return createTopology(name, description, "AWS", Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "topology create --AZURE", help = "Create a new AWS topology configuration")
    public String createAzureTopology(
            @CliOption(key = "name", mandatory = true, help = "Name of the topology") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the topology") String description
    ) {
        try {
            return createTopology(name, description, "AZURE_RM", Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "topology create --GCP", help = "Create a new AWS topology configuration")
    public String createGcpTopology(
            @CliOption(key = "name", mandatory = true, help = "Name of the topology") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the topology") String description
    ) {
        try {
            return createTopology(name, description, "GCP", Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "topology create --OPENSTACK", help = "Create a new AWS topology configuration")
    public String createOpenstackTopology(
            @CliOption(key = "name", mandatory = true, help = "Name of the topology") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the topology") String description,
            @CliOption(key = "url", mandatory = false, help = "URL of the topology mapping file to download from") String url,
            @CliOption(key = "file", mandatory = false, help = "File which contains the topology mapping") File file
    ) {
        try {
            return createTopology(name, description, "OPENSTACK", convertMappingFile(file, url));
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    private Map<String, String> convertMappingFile(File file, String url) {
        Map<String, String> result = Maps.newHashMap();
        if (file != null || url != null) {
            BufferedReader bf = null;
            try {
                bf = getReader(file, url);
                String line;
                while ((line = bf.readLine()) != null) {
                    String[] mapping = line.split("\\s+");
                    if (mapping.length != 2) {
                        continue;
                    }
                    result.put(mapping[0], mapping[1]);
                }
            } catch (IOException e) {
                throw exceptionTransformer.transformToRuntimeException(e);
            } finally {
                IOUtils.closeQuietly(bf);
            }
        }
        return result;
    }

    private BufferedReader getReader(File file, String url) {
        try {
            if (file != null) {
                return IOUtils.toBufferedReader(new FileReader(file));
            }
            return IOUtils.toBufferedReader(new InputStreamReader((new URL(url)).openStream()));
        } catch (IOException e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    private String createTopology(String name, String description, String cloudPlatform, Map<String, String> mapping) {
        try {
            TopologyRequest req = new TopologyRequest();
            req.setCloudPlatform(cloudPlatform);
            req.setName(name);
            req.setDescription(description);
            req.setNodes(mapping);
            IdJson id = cloudbreakClient.topologyEndpoint().postPublic(req);
            context.setHint(Hints.CREATE_CREDENTIAL_WITH_TOPOLOGY);
            return String.format(CREATE_SUCCESS_MSG, id.getId());
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }

    }

    @CliCommand(value = "topology list", help = "Shows the currently available cloud topology")
    public String listTopology() {
        try {
            Set<TopologyResponse> publics = cloudbreakClient.topologyEndpoint().getPublics();
            return renderSingleMap(responseTransformer.transformToMap(publics, "id", "name"), true, "ID", "INFO");
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "topology show", help = "Shows the topology by its id or name")
    public Object showTopology(
            @CliOption(key = "id", mandatory = false, help = "Id of the topology") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the topology") String name) {
        try {
            if (id != null) {
                return renderSingleMap(
                        responseTransformer.transformObjectToStringMap(
                                cloudbreakClient.topologyEndpoint().get(Long.valueOf(id))), "FIELD", "VALUE");
            } else if (name != null) {
                TopologyResponse aPublic = selectByName(cloudbreakClient.topologyEndpoint().getPublics(), name);
                if (aPublic != null) {
                    return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            return "No topology specified.";
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "topology delete", help = "Shows the topology by its id or name")
    public Object deleteTopology(
            @CliOption(key = "id", mandatory = false, help = "Id of the topology") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the topology") String name) {
        try {
            if (id != null) {
                cloudbreakClient.topologyEndpoint().delete(Long.valueOf(id), false);
                return String.format("Topology has been deleted, id: %s", id);
            } else if (name != null) {
                Long idForName = getIdForName(cloudbreakClient.topologyEndpoint().getPublics(), name);
                if (idForName != null) {
                    cloudbreakClient.topologyEndpoint().delete(idForName, false);
                    return String.format("Topology has been deleted, name: %s", name);
                }
            }
            return "No topology specified.";
        } catch (Exception e) {
            throw exceptionTransformer.transformToRuntimeException(e);
        }
    }

    private Long getIdForName(Set<TopologyResponse> publics, String name) {
        TopologyResponse t = selectByName(publics, name);
        if (t != null) {
            return t.getId();
        }
        return null;
    }

    private TopologyResponse selectByName(Set<TopologyResponse> publics, String name) {
        if (publics != null) {
            for (TopologyResponse res : publics) {
                if (res.getName().equals(name)) {
                    return res;
                }
            }
        }
        return null;
    }

}
