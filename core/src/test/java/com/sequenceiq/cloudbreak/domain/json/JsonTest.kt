package com.sequenceiq.cloudbreak.domain.json

import java.io.IOException
import java.util.HashMap

import org.junit.Assert
import org.junit.Test

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.model.Image

class JsonTest {

    @Test
    @Throws(JsonProcessingException::class)
    fun testMembers() {
        val userData = HashMap<InstanceGroupType, String>()
        userData.put(InstanceGroupType.CORE, "CORE")
        val image = Image("cb-centos66-amb200-2015-05-25", userData, null, null)
        val json = Json(image)
        Assert.assertEquals("{\"imageName\":\"cb-centos66-amb200-2015-05-25\",\"userdata\":{\"CORE\":\"CORE\"},\"hdpRepo\":null,\"hdpVersion\":null}",
                json.value)
    }

    // The reason for this to check whether serialisetion-deserialisation-serialisation results the same json
    @Test
    @Throws(IOException::class)
    fun testMultipleSerialisation() {
        val userData = HashMap<InstanceGroupType, String>()
        userData.put(InstanceGroupType.CORE, "CORE")
        val image = Image("cb-centos66-amb200-2015-05-25", userData, null, null)
        var json = Json(image)
        val expected = json.value
        val covertedAgain = json.get<Image>(Image::class.java)
        json = Json(covertedAgain)
        Assert.assertEquals(expected, json.value)
    }
}
