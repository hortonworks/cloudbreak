package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.RenderResult;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class KernelTuningStateRenderTest {

    private static final String STATE_PATH = "salt-common/salt/kernel-tuning/init.sls";

    private static final String SETTINGS_IMPORT_PATTERN = "(?m)^\\{%-? from .*$\\R";

    private static final String CONF_EXISTS_CALL = "salt['file.file_exists'](min_free_conf)";

    private static final String CONF = "/etc/sysctl.d/00-00-cdp-min-free-kbytes.conf";

    private static final String APPLY = "apply_min_free_kbytes";

    @Test
    void enabledShouldWriteTargetAndReconcileOnlyThisKeyOnEveryRun() throws IOException {
        Map<String, Object> states = render(kernelTuning(true, true, true, 1280000), false);

        assertTrue(fileContents(states).contains("vm.min_free_kbytes = 1280000"), "Unexpected file contents: " + fileContents(states));
        assertApplyReconcilesOnlyThisKey(states);
    }

    @Test
    void disabledShouldWriteRestoreValueWhenTunedBefore() throws IOException {
        Map<String, Object> states = render(kernelTuning(true, true, false, 67584), true);

        assertTrue(fileContents(states).contains("vm.min_free_kbytes = 67584"), "Unexpected file contents: " + fileContents(states));
        assertApplyReconcilesOnlyThisKey(states);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("noStateCases")
    void shouldRenderNoStates(String name, Map<String, Object> kernelTuning, boolean confExists) throws IOException {
        assertTrue(render(kernelTuning, confExists).isEmpty());
    }

    private static Stream<Arguments> noStateCases() {
        return Stream.of(
                Arguments.of("disabled, never tuned", kernelTuning(true, true, false, 67584), false),
                Arguments.of("unsupported OS", kernelTuning(false, true, true, 1280000), false),
                Arguments.of("no pillar", kernelTuning(true, false, false, 0), true),
                Arguments.of("no restore value", kernelTuning(true, true, false, 0), true));
    }

    private void assertApplyReconcilesOnlyThisKey(Map<String, Object> states) {
        Map<String, Object> apply = stateArgs(states, APPLY, "cmd.run");
        assertEquals("/usr/lib/systemd/systemd-sysctl --prefix=vm.min_free_kbytes", apply.get("name"));
        assertTrue(String.valueOf(apply.get("unless")).contains("sysctl -n vm.min_free_kbytes"), "Unexpected unless: " + apply.get("unless"));
        assertTrue(String.valueOf(apply.get("unless")).contains("systemd-sysctl --cat-config"), "Unexpected unless: " + apply.get("unless"));
        assertEquals(List.of(Map.of("file", CONF)), apply.get("require"));
        // onchanges would skip the retry after a failed apply, the live value must be reconciled on every run instead
        assertFalse(apply.containsKey("onchanges"), "The apply must not depend on file changes: " + apply);
    }

    private String fileContents(Map<String, Object> states) {
        return String.valueOf(stateArgs(states, CONF, "file.managed").get("contents"));
    }

    private Map<String, Object> stateArgs(Map<String, Object> states, String id, String function) {
        Map<String, Object> state = (Map<String, Object>) states.get(id);
        Map<String, Object> args = new HashMap<>();
        ((List<Map<String, Object>>) state.get(function)).forEach(args::putAll);
        return args;
    }

    private Map<String, Object> render(Map<String, Object> kernelTuning, boolean confExists) throws IOException {
        // the settings.sls import and the file check resolve pillars, grains and files through the salt runtime, they are provided directly instead
        String template = FileReaderUtils.readFileFromClasspath(STATE_PATH)
                .replaceAll(SETTINGS_IMPORT_PATTERN, "")
                .replace(CONF_EXISTS_CALL, "conf_exists");
        RenderResult renderResult = new Jinjava().renderForResult(template, Map.of("kernel_tuning", kernelTuning, "conf_exists", confExists));
        assertTrue(renderResult.getErrors().isEmpty(), "Jinja rendering failed for " + STATE_PATH + ": " + renderResult.getErrors());
        Map<String, Object> states = new Yaml().load(renderResult.getOutput());
        return states == null ? Map.of() : states;
    }

    private static Map<String, Object> kernelTuning(boolean supported, boolean configured, boolean enabled, long valueKb) {
        return Map.of(
                "supported", supported,
                "minFreeKbytes", Map.of(
                        "configured", configured,
                        "enabled", enabled,
                        "valueKb", valueKb));
    }
}
