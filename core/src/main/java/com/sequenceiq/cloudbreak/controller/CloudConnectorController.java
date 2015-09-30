package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.controller.json.JsonEntity;
import com.sequenceiq.cloudbreak.controller.json.PlatformDisksJson;
import com.sequenceiq.cloudbreak.controller.json.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.controller.json.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.controller.json.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.service.stack.CloudConnectorParameterService;

@Controller
public class CloudConnectorController {

    @Inject
    private CloudConnectorParameterService cloudConnectorParameterService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @RequestMapping(value = "/connectors", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, JsonEntity>> getPlatforms() {
        PlatformVariants pv = cloudConnectorParameterService.getPlatformVariants();
        PlatformDisks diskTypes = cloudConnectorParameterService.getDiskTypes();
        PlatformVirtualMachines vmtypes = cloudConnectorParameterService.getVmtypes();
        PlatformRegions regions = cloudConnectorParameterService.getRegions();

        Map<String, JsonEntity> map = new HashMap<>();

        map.put("variants", conversionService.convert(pv, PlatformVariantsJson.class));
        map.put("disks", conversionService.convert(diskTypes, PlatformDisksJson.class));
        map.put("virtualMachines", conversionService.convert(vmtypes, PlatformVirtualMachinesJson.class));
        map.put("regions", conversionService.convert(regions, PlatformRegionsJson.class));

        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/variants", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<PlatformVariantsJson> getPlatformVariants() {
        PlatformVariants pv = cloudConnectorParameterService.getPlatformVariants();
        return new ResponseEntity<>(conversionService.convert(pv, PlatformVariantsJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/variants/{type}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Collection<String>> getPlatformVariantByType(@PathVariable String type) {
        PlatformVariants pv = cloudConnectorParameterService.getPlatformVariants();
        Collection<String> strings = pv.getPlatformToVariants().get(type.toUpperCase());
        return new ResponseEntity<>(strings == null ? new ArrayList<String>() : strings, HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/disktypes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<PlatformDisksJson> getDisktypes() {
        PlatformDisks diskTypes = cloudConnectorParameterService.getDiskTypes();
        return new ResponseEntity<>(conversionService.convert(diskTypes, PlatformDisksJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/disktypes/{type}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, String>> getDisktypeByType(@PathVariable String type) {
        PlatformDisks diskTypes = cloudConnectorParameterService.getDiskTypes();
        Map<String, String> strings = diskTypes.getDiskTypes().get(type.toUpperCase());
        return new ResponseEntity<>(strings == null ? new HashMap<String, String>() : strings, HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/vmtypes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<PlatformVirtualMachinesJson> getVmTypes() {
        PlatformVirtualMachines vmtypes = cloudConnectorParameterService.getVmtypes();
        return new ResponseEntity<>(conversionService.convert(vmtypes, PlatformVirtualMachinesJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/vmtypes/{type}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, String>> getVmTypeByType(@PathVariable String type) {
        PlatformVirtualMachines vmtypes = cloudConnectorParameterService.getVmtypes();
        Map<String, String> stringStringMap = vmtypes.getVirtualMachines().get(type.toUpperCase());
        return new ResponseEntity<>(stringStringMap == null ? new HashMap<String, String>() : stringStringMap, HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/regions", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<PlatformRegionsJson> getRegions() {
        PlatformRegions pv = cloudConnectorParameterService.getRegions();
        return new ResponseEntity<>(conversionService.convert(pv, PlatformRegionsJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/regions/r/{type}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, String>> getRegionRByType(@PathVariable String type) {
        PlatformRegions pv = cloudConnectorParameterService.getRegions();
        Map<String, String> stringStringMap = pv.getRegions().get(type.toUpperCase());
        return new ResponseEntity<>(stringStringMap == null ? new HashMap<String, String>() : stringStringMap, HttpStatus.OK);
    }

    @RequestMapping(value = "/connectors/regions/av/{type}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, List<String>>> getRegionAvByType(@PathVariable String type) {
        PlatformRegions pv = cloudConnectorParameterService.getRegions();
        Map<String, List<String>> stringStringMap = pv.getAvailabiltyZones().get(type.toUpperCase());
        return new ResponseEntity<>(stringStringMap == null ? new HashMap<String, List<String>>() : stringStringMap, HttpStatus.OK);
    }

}
