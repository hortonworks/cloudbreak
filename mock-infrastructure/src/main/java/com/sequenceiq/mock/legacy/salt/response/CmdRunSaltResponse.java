package com.sequenceiq.mock.legacy.salt.response;

import static com.sequenceiq.mock.legacy.service.HostNameUtil.responseFromJsonFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.mock.legacy.salt.SaltResponse;

@Component
public class CmdRunSaltResponse implements SaltResponse {

    @Override
    public Object run(String body) throws Exception {
        Pattern pattern = Pattern.compile("&tgt=([\\w\\.-]+)&");
        Matcher matcher = pattern.matcher(body);
        String host = matcher.find() ? matcher.group(1) : "";

        if (body.contains("arg=%28cd+%2Fsrv%2Fsalt%2Fdisk%3BCLOUD_PLATFORM%3D%27MOCK%27+ATTACHED_VOLUME_NAME_LIST%3D%27%27+"
                + "ATTACHED_VOLUME_SERIAL_LIST%3D%27%27+.%2Ffind-device-and-format.sh%29")) {
            return String.format(responseFromJsonFile("saltapi/cmd_run_format_response.json"), host);
        } else if (body.contains("arg=cat+%2Fetc%2Ffstab")) {
            return String.format(responseFromJsonFile("saltapi/cmd_run_mount_response.json"), host);
        } else {
            return responseFromJsonFile("saltapi/cmd_run_empty_response.json");
        }
    }

    @Override
    public String cmd() {
        return "cmd.run";
    }
}
