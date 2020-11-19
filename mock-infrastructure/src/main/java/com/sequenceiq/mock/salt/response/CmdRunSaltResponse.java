package com.sequenceiq.mock.salt.response;

import static com.sequenceiq.mock.HostNameUtil.responseFromJsonFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class CmdRunSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, String body) throws Exception {
        Pattern pattern = Pattern.compile("&tgt=([\\w\\.-]+)&");
        Matcher matcher = pattern.matcher(body);
        String host = matcher.find() ? matcher.group(1) : "";
        String jsonFile;
        if (body.contains("arg=%28cd+%2Fsrv%2Fsalt%2Fdisk%3BCLOUD_PLATFORM%3D%27MOCK%27+ATTACHED_VOLUME_NAME_LIST%3D%27%27+"
                + "ATTACHED_VOLUME_SERIAL_LIST%3D%27%27+.%2Ffind-device-and-format.sh%29")) {
            jsonFile = String.format(responseFromJsonFile("saltapi/cmd_run_format_response.json"), host);
        } else if (body.contains("arg=cat+%2Fetc%2Ffstab")) {
            jsonFile = String.format(responseFromJsonFile("saltapi/cmd_run_mount_response.json"), host);
        } else {
            jsonFile = responseFromJsonFile("saltapi/cmd_run_empty_response.json");
        }
        return JsonUtil.readValue(jsonFile, ApplyResponse.class);
    }

    @Override
    public String cmd() {
        return "cmd.run";
    }
}
