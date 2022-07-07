package com.sequenceiq.mock.controller;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.CmProfile;
import com.sequenceiq.mock.service.ResponseModifierService;
import com.sequenceiq.mock.spi.MockResponse;

@RestController
//@RequestMapping()
public class ConfigureController {

    @Inject
    private ResponseModifierService responseModifierService;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @PostMapping("/configure")
    public void configure(@RequestBody MockResponse mockResponse) {
        responseModifierService.addResponse(mockResponse);
    }

    @PostMapping("/configure/clear")
    public void configureClear(@RequestBody MockResponse mockResponse) {
        responseModifierService.clearResponse(mockResponse);
    }

    @GetMapping("/{mockUuid}/profile/{profile}/{times}")
    public void configure(@PathVariable("mockUuid") String mockUuid, @PathVariable("profile") String profile, @PathVariable("times") int times) {
        if (!clouderaManagerStoreService.exists(mockUuid)) {
            clouderaManagerStoreService.start(mockUuid);
        }
        List<CmProfile> activeProfiles = clouderaManagerStoreService.read(mockUuid).getActiveProfiles();
        Optional<CmProfile> cmProfileOpt = activeProfiles.stream().filter(p -> p.getProfile().equals(profile)).findFirst();
        if (cmProfileOpt.isPresent()) {
            CmProfile cmProfile = cmProfileOpt.get();
            if (times == 0) {
                activeProfiles.remove(cmProfile);
            } else {
                cmProfile.setTimes(times);
            }
        } else {
            activeProfiles.add(new CmProfile(profile, times));
        }
    }

    @PutMapping("/{mockUuid}/cmversion")
    public void setCmVersion(@PathVariable("mockUuid") String mockUuid, @RequestParam("version") String version) {
        clouderaManagerStoreService.read(mockUuid).getClusterTemplate().setCmVersion(version);
    }
}
