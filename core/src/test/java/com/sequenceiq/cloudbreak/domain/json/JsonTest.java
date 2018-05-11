package com.sequenceiq.cloudbreak.domain.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.model.Image;

public class JsonTest {

    @Test
    public void testMembers() throws JsonProcessingException {
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "CORE");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id");
        Json json = new Json(image);
        Assert.assertEquals("{\"imageName\":\"cb-centos66-amb200-2015-05-25\",\"userdata\":{\"CORE\":\"CORE\"},\"os\":\"redhat6\",\"osType\":\"redhat6\","
                        + "\"imageCatalogUrl\":\"\",\"imageCatalogName\":\"default\",\"imageId\":\"default-id\"}",
                json.getValue());
    }

    // The reason for this to check whether serialisetion-deserialisation-serialisation results the same json
    @Test
    public void testMultipleSerialisation() throws IOException {
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "CORE");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id");
        Json json = new Json(image);
        String expected = json.getValue();
        Image covertedAgain = json.get(Image.class);
        json = new Json(covertedAgain);
        Assert.assertEquals(expected, json.getValue());
    }

    @Test
    public void testMultipleSerialisationWithOtherConstructorOfImage() throws IOException {
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "CORE");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id");
        Json json = new Json(image);
        String expected = json.getValue();
        Image covertedAgain = json.get(Image.class);
        json = new Json(covertedAgain);
        Assert.assertEquals(expected, json.getValue());
    }
}
