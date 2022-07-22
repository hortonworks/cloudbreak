package com.sequenceiq.datalake.job.salt;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerJobService;

@Service
public class SdxSaltStatusCheckerJobService extends SaltStatusCheckerJobService<SdxSaltStatusCheckerJobAdapter> {
}
