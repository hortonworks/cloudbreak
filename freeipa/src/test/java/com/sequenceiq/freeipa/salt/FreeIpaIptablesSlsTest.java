package com.sequenceiq.freeipa.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.RenderResult;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class FreeIpaIptablesSlsTest {

    private static final String SLS_PATH = "freeipa-salt/salt/freeipa/iptables.sls";

    private static final String NFTABLES_CONF_PATH = "freeipa-salt/salt/freeipa/templates/freeipa-nftables.conf";

    private static final String OS_GRAIN_PATTERN = "(?m)^\\{%- set os = .*$";

    private static final String OS_MAJOR_RELEASE_GRAIN_PATTERN = "(?m)^\\{%- set osMajorRelease = .*$";

    @Test
    void rhel9ManagesNftablesConfigFileAndEnablesService() throws IOException {
        Map<String, Object> states = render("RedHat", 9);

        Map<String, Object> file = stateArguments(states, "deploy_freeipa_nftables_conf", "file.managed");
        assertEquals("/etc/nftables.d/freeipa.conf", file.get("name"));
        assertEquals("salt://freeipa/templates/freeipa-nftables.conf", file.get("source"));

        Map<String, Object> include = stateArguments(states, "include_freeipa_nftables_conf", "file.append");
        assertEquals("/etc/sysconfig/nftables.conf", include.get("name"));
        assertTrue(((String) include.get("text")).contains("include \"/etc/nftables.d/freeipa.conf\""),
                "The nftables service config should include the FreeIPA rules: " + include.get("text"));

        Map<String, Object> service = stateArguments(states, "nftables", "service.running");
        assertEquals(true, service.get("enable"));
        assertTrue(((List<Map<String, Object>>) service.get("require")).contains(Map.of("file", "include_freeipa_nftables_conf")),
                "The nftables service should require the include to be in place: " + service.get("require"));
        assertTrue(((List<Map<String, Object>>) service.get("watch")).contains(Map.of("file", "deploy_freeipa_nftables_conf")),
                "The nftables service should reload when the ruleset changes: " + service.get("watch"));
    }

    @ParameterizedTest
    @ValueSource(ints = {8443, 8080})
    void rhel9RulesetAcceptsLoopbackBeforeDroppingThePort(int port) throws IOException {
        String ruleset = FileReaderUtils.readFileFromClasspath(NFTABLES_CONF_PATH);

        int acceptIndex = ruleset.indexOf("iifname \"lo\" tcp dport { 8080, 8443 } accept");
        int dropIndex = ruleset.indexOf("ct state new tcp dport { 8080, 8443 } drop");

        assertTrue(acceptIndex >= 0, "The ruleset should accept loopback traffic to 8080/8443: " + ruleset);
        assertTrue(dropIndex >= 0, "The ruleset should drop new connections to 8080/8443: " + ruleset);
        assertTrue(ruleset.contains(String.valueOf(port)), "The ruleset should cover port " + port + ": " + ruleset);
        assertTrue(acceptIndex < dropIndex,
                "The loopback accept rule must come before the drop rule, otherwise loopback traffic is dropped: " + ruleset);
    }

    @Test
    void rhel9RendersNoImperativeNftablesStates() throws IOException {
        Map<String, Object> states = render("RedHat", 9);

        states.forEach((stateId, functions) -> ((Map<String, Object>) functions).keySet().forEach(function ->
                assertFalse(((String) function).startsWith("nftables."),
                        "The RHEL 9 branch should manage the ruleset via a config file, not imperative nftables states: " + stateId)));
    }

    @ParameterizedTest
    @ValueSource(ints = {8443, 8080})
    void rhel8AcceptRulesAreRestrictedToLoopback(int port) throws IOException {
        Map<String, Object> state = stateArguments(render("RedHat", 8), "accept_tcp_" + port + "_on_lo", "iptables.insert");

        assertEquals("lo", state.get("i"));
        assertEquals(port, state.get("dport"));
        assertEquals("ACCEPT", state.get("jump"));
        assertTrue(((String) state.get("unless")).contains(port + " -j ACCEPT"));
    }

    @Test
    void nonRedHatRendersNoStates() throws IOException {
        assertTrue(render("Ubuntu", 22).isEmpty());
    }

    private Map<String, Object> render(String os, int osMajorRelease) throws IOException {
        // the grains are resolved by the salt runtime, they are provided as literals instead
        String template = FileReaderUtils.readFileFromClasspath(SLS_PATH)
                .replaceAll(OS_GRAIN_PATTERN, "{%- set os = '" + os + "' %}")
                .replaceAll(OS_MAJOR_RELEASE_GRAIN_PATTERN, "{%- set osMajorRelease = " + osMajorRelease + " %}");

        RenderResult renderResult = new Jinjava().renderForResult(template, Map.of());

        assertTrue(renderResult.getErrors().isEmpty(), "Jinja rendering failed for " + SLS_PATH + ": " + renderResult.getErrors());
        Map<String, Object> states = new Yaml().load(renderResult.getOutput());
        return states == null ? Map.of() : states;
    }

    private Map<String, Object> stateArguments(Map<String, Object> states, String stateId, String function) {
        Map<String, List<Map<String, Object>>> functions = (Map<String, List<Map<String, Object>>>) states.get(stateId);
        assertNotNull(functions, "State " + stateId + " is missing: " + states.keySet());
        List<Map<String, Object>> arguments = functions.get(function);
        assertNotNull(arguments, "State " + stateId + " should use " + function + ": " + functions.keySet());
        Map<String, Object> result = new HashMap<>();
        arguments.forEach(result::putAll);
        return result;
    }
}
