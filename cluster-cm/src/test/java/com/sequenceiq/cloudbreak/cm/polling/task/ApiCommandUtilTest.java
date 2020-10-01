package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiServiceRef;

public class ApiCommandUtilTest {

    @Test
    public void getResultMessageFromChildrenFirstLevel() {
        ApiCommandList commandList = getApiCommandList("name0", "message0", "serviceName0");
        List<String> actual = ApiCommandUtil.getFailedCommandMessages(commandList);

        Assertions.assertEquals(actual.size(), 1);
        Assertions.assertEquals(actual.get(0), "name0(serviceName0): message0");
    }

    @Test
    public void getResultMessageFromChildrenRecursively() {
        ApiCommandList level1 = getApiCommandList("name0", "message0", "serviceName0");
        ApiCommandList level2 = getApiCommandList("name1", "message1", "serviceName1");
        level1.getItems().get(0).setChildren(level2);
        List<String> actual = ApiCommandUtil.getFailedCommandMessages(level1);

        Assertions.assertEquals(actual.size(), 2);
        Assertions.assertEquals(actual.get(0), "name0(serviceName0): message0");
        Assertions.assertEquals(actual.get(1), "name1(serviceName1): message1");
    }

    @Test
    public void getResultMessageFromChildrenFiveLevel() {
        ApiCommandList level1 = getApiCommandList("name0", "message0", "serviceName0");
        ApiCommandList level2 = getApiCommandList("name1", "message1", "serviceName1");
        ApiCommandList level3 = getApiCommandList("name2", "message2", "serviceName2");
        ApiCommandList level4 = getApiCommandList("name3", "message3", "serviceName3");
        ApiCommandList level5 = getApiCommandList("name4", "message4", "serviceName4");
        level1.getItems().get(0).setChildren(level2);
        level2.getItems().get(0).setChildren(level3);
        level3.getItems().get(0).setChildren(level4);
        level4.getItems().get(0).setChildren(level5);
        List<String> actual = ApiCommandUtil.getFailedCommandMessages(level1);

        Assertions.assertEquals(actual.size(), 5);
        Assertions.assertEquals(actual.get(0), "name0(serviceName0): message0");
        Assertions.assertEquals(actual.get(1), "name1(serviceName1): message1");
        Assertions.assertEquals(actual.get(2), "name2(serviceName2): message2");
        Assertions.assertEquals(actual.get(3), "name3(serviceName3): message3");
        Assertions.assertEquals(actual.get(4), "name4(serviceName4): message4");
    }

    @Test
    public void getResultMessageFromChildrenTwoChildren() {
        ApiCommandList level1 = getApiCommandList("name0", "message0", "serviceName0");
        ApiCommand level1Ch1 = getFailedApiCommand("name1", "message1", "serviceName1");
        ApiCommand level1Ch2 = getFailedApiCommand("name2", "message2", "serviceName2");
        level1.addItemsItem(level1Ch1);
        level1.addItemsItem(level1Ch2);
        List<String> actual = ApiCommandUtil.getFailedCommandMessages(level1);

        Assertions.assertEquals(actual.size(), 3);
        Assertions.assertEquals(actual.get(0), "name0(serviceName0): message0");
        Assertions.assertEquals(actual.get(1), "name1(serviceName1): message1");
        Assertions.assertEquals(actual.get(2), "name2(serviceName2): message2");
    }

    @Test
    public void getOnlyFailedCommandResultMessages() {
        ApiCommandList level1 = getApiCommandList("name0", "message0", "serviceName0");
        ApiCommand level1Ch1 = getFailedApiCommand("name1", "message1", "serviceName1");
        ApiCommand level1Ch2 = getSuccessfulApiCommand("name2", "message2", "serviceName2");
        ApiCommand level1Ch3 = getFailedApiCommand("name3", "message3", "serviceName3");
        level1.addItemsItem(level1Ch1);
        level1.addItemsItem(level1Ch2);
        level1.addItemsItem(level1Ch3);
        List<String> actual = ApiCommandUtil.getFailedCommandMessages(level1);

        Assertions.assertEquals(actual.size(), 3);
        Assertions.assertEquals(actual.get(0), "name0(serviceName0): message0");
        Assertions.assertEquals(actual.get(1), "name1(serviceName1): message1");
        Assertions.assertEquals(actual.get(2), "name3(serviceName3): message3");
    }

    @Test
    public void getResultMessageFromChildrenWhenItemsNull() {
        ApiCommandList commandList = new ApiCommandList();
        List<String> actual = ApiCommandUtil.getFailedCommandMessages(commandList);

        Assertions.assertEquals(actual.size(), 0);
    }

    @Test
    public void getResultMessageFromChildrenWhenItemsEmpty() {
        ApiCommandList commandList = new ApiCommandList();
        commandList.setItems(Collections.emptyList());
        List<String> actual = ApiCommandUtil.getFailedCommandMessages(commandList);

        Assertions.assertEquals(actual.size(), 0);
    }

    private ApiCommandList getApiCommandList(String name, String message, String serviceName) {
        ApiCommandList commandList = new ApiCommandList();
        ApiCommand apiCommand = getFailedApiCommand(name, message, serviceName);
        commandList.addItemsItem(apiCommand);
        return commandList;
    }

    private ApiCommand getFailedApiCommand(String name, String message, String serviceName) {
        return getApiCommand(name, message, serviceName, false);
    }

    private ApiCommand getSuccessfulApiCommand(String name, String message, String serviceName) {
        return getApiCommand(name, message, serviceName, true);
    }

    private ApiCommand getApiCommand(String name, String message, String serviceName, boolean success) {
        return new ApiCommand()
                .success(success)
                .active(false)
                .name(name)
                .resultMessage(message)
                .serviceRef(new ApiServiceRef().serviceName(serviceName));
    }

}