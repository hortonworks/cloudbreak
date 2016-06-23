package com.sequenceiq.cloudbreak.cloud.task

interface PollTask<T> : FetchTask<T>, Check<T>
