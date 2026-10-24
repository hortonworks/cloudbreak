package com.sequenceiq.mock.spi.controller;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.spi.TagStoreService;

@RestController
@RequestMapping("/{mock_uuid}/tags")
public class TagController {

    @Inject
    private TagStoreService tagStoreService;

    @GetMapping
    public Map<String, String> get(@PathVariable("mock_uuid") String mockuuid) {
        return tagStoreService.get(mockuuid);
    }

    @PostMapping
    public void update(@PathVariable("mock_uuid") String mockuuid, @RequestBody Map<String, String> tags) {
        tagStoreService.update(mockuuid, tags);
    }

    @PostMapping("/delete")
    public void delete(@PathVariable("mock_uuid") String mockuuid, @RequestBody Set<String> tagKeys) {
        tagStoreService.delete(mockuuid, tagKeys);
    }
}
