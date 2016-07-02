package com.sequenceiq.cloudbreak.shell.commands.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.MethodNotSupportedException;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.TopologyRequest;
import com.sequenceiq.cloudbreak.api.model.TopologyResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;


public class BasePlatformCommands implements BaseCommands, PlatformCommands {
    private static final String CREATE_SUCCESS_MSG = "Platform created with id: '%d' and name: '%s'";

    private ShellContext shellContext;

    public BasePlatformCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "platform list")
    public boolean listAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "platform list", help = "Shows the currently available platform")
    public String list() {
        try {
            Set<TopologyResponse> publics = shellContext.cloudbreakClient().topologyEndpoint().getPublics();
            Map<String, String> map = shellContext.responseTransformer().transformToMap(publics, "id", "name");
            return shellContext.outputTransformer().render(map, "ID", "INFO");
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliAvailabilityIndicator(value = "platform show")
    public boolean showAvailable() {
        return !shellContext.isMarathonMode();
    }

    @Override
    public String show(Long id, String name) {
        try {
            if (id != null) {
                TopologyResponse topologyResponse = shellContext.cloudbreakClient().topologyEndpoint().get(id);
                return shellContext.outputTransformer()
                        .render(shellContext.responseTransformer()
                                .transformObjectToStringMap(topologyResponse), "FIELD", "VALUE");
            } else if (name != null) {
                TopologyResponse topologyResponse = selectByName(shellContext.cloudbreakClient().topologyEndpoint().getPublics(), name);
                if (topologyResponse != null) {
                    return shellContext.outputTransformer()
                            .render(shellContext.responseTransformer()
                                    .transformObjectToStringMap(topologyResponse), "FIELD", "VALUE");
                }
            }
            return "No platform specified.";
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "platform show --id", help = "Show the platform by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "platform show --name", help = "Show the platform by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @Override
    public boolean selectAvailable() {
        return false;
    }

    @Override
    public String select(Long id, String name) throws Exception {
        throw new MethodNotSupportedException("Platform select command not available.");
    }

    @Override
    public String selectById(Long id) throws Exception {
        return select(id, null);
    }

    @Override
    public String selectByName(String name) throws Exception {
        return select(null, name);
    }

    @CliAvailabilityIndicator(value = "platform delete")
    @Override
    public boolean deleteAvailable() {
        return !shellContext.isMarathonMode();
    }

    @Override
    public String delete(Long id, String name) {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().topologyEndpoint().delete(id, false);
                return String.format("Platform has been deleted, id: %s", id);
            } else if (name != null) {
                Long idForName = getIdForName(shellContext.cloudbreakClient().topologyEndpoint().getPublics(), name);
                if (idForName != null) {
                    shellContext.cloudbreakClient().topologyEndpoint().delete(idForName, false);
                    return String.format("Platform has been deleted, name: %s", name);
                }
            }
            return "No platform specified.";
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "platform delete --id", help = "Delete the platform by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "platform delete --name", help = "Delete the platform by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public boolean createPlatformAvailable(String platform) {
        return true;
    }

    public String create(String name, String description, String cloudPlatform, Map<String, String> mapping) {
        try {
            TopologyRequest req = new TopologyRequest();
            req.setCloudPlatform(cloudPlatform);
            req.setName(name);
            req.setDescription(description);
            req.setNodes(mapping);
            IdJson id = shellContext.cloudbreakClient().topologyEndpoint().postPublic(req);
            shellContext.setHint(Hints.CREATE_CREDENTIAL_WITH_TOPOLOGY);
            return String.format(CREATE_SUCCESS_MSG, id.getId(), name);
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }

    }

    @Override
    public Map<String, String> convertMappingFile(File file, String url) {
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
                throw shellContext.exceptionTransformer().transformToRuntimeException(e);
            } finally {
                IOUtils.closeQuietly(bf);
            }
        }
        return result;
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private BufferedReader getReader(File file, String url) {
        try {
            if (file != null) {
                return IOUtils.toBufferedReader(new FileReader(file));
            }
            return IOUtils.toBufferedReader(new InputStreamReader((new URL(url)).openStream()));
        } catch (IOException e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
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
