package com.sequenceiq.cloudbreak.domain.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.InstanceGroupType;

class JsonTest {

    @Test
    void testMembers() {
        Map<InstanceGroupType, String> userData = new EnumMap<>(InstanceGroupType.class);
        userData.put(InstanceGroupType.CORE, "CORE");
        Image image = Image.builder()
                .withImageName("cb-centos66-amb200-2015-05-25")
                .withUserdata(userData)
                .withOs("redhat6")
                .withOsType("redhat6")
                .withArchitecture("arm64")
                .withImageCatalogName("default")
                .withImageCatalogUrl("")
                .withImageId("default-id")
                .withPackageVersions(Collections.emptyMap())
                .withDate("2019-10-24")
                .withCreated(1571884856L)
                .withTags(Map.of("release-version", "7.3.1.500"))
                .build();
        Json json = new Json(image);
        assertEquals("{\"imageName\":\"cb-centos66-amb200-2015-05-25\",\"userdata\":{\"CORE\":\"CORE\"},\"os\":\"redhat6\",\"osType\":\"redhat6\","
                + "\"architecture\":\"arm64\",\"imageCatalogUrl\":\"\",\"imageCatalogName\":\"default\",\"imageId\":\"default-id\","
                + "\"packageVersions\":{},\"date\":\"2019-10-24\",\"created\":1571884856,\"tags\":{\"release-version\":\"7.3.1.500\"}}", json.getValue());
    }

    // The reason for this to check whether serialisetion-deserialisation-serialisation results the same json
    @Test
    void testMultipleSerialisation() throws IOException {
        Map<InstanceGroupType, String> userData = new EnumMap<>(InstanceGroupType.class);
        userData.put(InstanceGroupType.CORE, "CORE");
        Image image = Image.builder()
                .withImageName("cb-centos66-amb200-2015-05-25")
                .withUserdata(userData)
                .withOs("redhat6")
                .withOsType("redhat6")
                .withImageCatalogName("default")
                .withImageId("default-id")
                .build();
        Json json = new Json(image);
        String expected = json.getValue();
        Image covertedAgain = json.get(Image.class);
        json = new Json(covertedAgain);
        assertEquals(expected, json.getValue());
    }

    @Test
    void testMultipleSerialisationWithOtherConstructorOfImage() throws IOException {
        Map<InstanceGroupType, String> userData = new EnumMap<>(InstanceGroupType.class);
        userData.put(InstanceGroupType.CORE, "CORE");
        Image image = Image.builder()
                .withImageName("cb-centos66-amb200-2015-05-25")
                .withUserdata(userData)
                .withOs("redhat6")
                .withOsType("redhat6")
                .withImageCatalogName("default")
                .withImageId("default-id")
                .build();
        Json json = new Json(image);
        String expected = json.getValue();
        Image covertedAgain = json.get(Image.class);
        json = new Json(covertedAgain);
        assertEquals(expected, json.getValue());
    }
}
