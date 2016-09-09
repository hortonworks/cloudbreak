package com.sequenceiq.cloudbreak.shell.commands;

import org.springframework.shell.core.CommandMarker;

import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public interface BaseCommands extends CommandMarker {

    boolean selectAvailable();

    String select(Long id, String name) throws Exception;

    String selectById(Long id) throws Exception;

    String selectByName(String name) throws Exception;

    boolean showAvailable();

    String show(Long id, String name) throws Exception;

    String showById(Long id) throws Exception;

    String showByName(String name) throws Exception;

    boolean deleteAvailable();

    String delete(Long id, String name) throws Exception;

    String deleteById(Long id) throws Exception;

    String deleteByName(String name) throws Exception;

    boolean listAvailable();

    String list() throws Exception;

    ShellContext shellContext();
}
