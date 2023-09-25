package com.sequenceiq.cloudbreak.rotation.secret.custom;

import java.util.Optional;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class CustomJobRotationContext extends RotationContext {

    private Optional<Runnable> rotationJob;

    private Optional<Runnable> rollbackJob;

    private Optional<Runnable> finalizeJob;

    private Optional<Runnable> preValidateJob;

    private Optional<Runnable> postValidateJob;

    CustomJobRotationContext(String resourceCrn, Optional<Runnable> rotationJob, Optional<Runnable> rollbackJob, Optional<Runnable> finalizeJob,
            Optional<Runnable> preValidateJob, Optional<Runnable> postValidateJob) {
        super(resourceCrn);
        this.rotationJob = rotationJob;
        this.rollbackJob = rollbackJob;
        this.finalizeJob = finalizeJob;
        this.postValidateJob = postValidateJob;
        this.preValidateJob = preValidateJob;
    }

    public Optional<Runnable> getRotationJob() {
        return rotationJob;
    }

    public Optional<Runnable> getRollbackJob() {
        return rollbackJob;
    }

    public Optional<Runnable> getFinalizeJob() {
        return finalizeJob;
    }

    public Optional<Runnable> getPreValidateJob() {
        return preValidateJob;
    }

    public Optional<Runnable> getPostValidateJob() {
        return postValidateJob;
    }

    public static CustomJobRotationContextBuilder builder() {
        return new CustomJobRotationContextBuilder();
    }

    public static class CustomJobRotationContextBuilder {

        private String resourceCrn;

        private Optional<Runnable> rotationJob = Optional.empty();

        private Optional<Runnable> rollbackJob = Optional.empty();

        private Optional<Runnable> finalizeJob = Optional.empty();

        private Optional<Runnable> preValidateJob = Optional.empty();

        private Optional<Runnable> postValidateJob = Optional.empty();

        public CustomJobRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public CustomJobRotationContextBuilder withRotationJob(Runnable rotationJob) {
            this.rotationJob = Optional.ofNullable(rotationJob);
            return this;
        }

        public CustomJobRotationContextBuilder withRollbackJob(Runnable rollbackJob) {
            this.rollbackJob = Optional.ofNullable(rollbackJob);
            return this;
        }

        public CustomJobRotationContextBuilder withFinalizeJob(Runnable finalizeJob) {
            this.finalizeJob = Optional.ofNullable(finalizeJob);
            return this;
        }

        public CustomJobRotationContextBuilder withPreValidateJob(Runnable preValidateJob) {
            this.preValidateJob = Optional.ofNullable(preValidateJob);
            return this;
        }

        public CustomJobRotationContextBuilder withPostValidateJob(Runnable postValidateJob) {
            this.postValidateJob = Optional.ofNullable(postValidateJob);
            return this;
        }

        public CustomJobRotationContext build() {
            return new CustomJobRotationContext(resourceCrn, rotationJob, rollbackJob, finalizeJob, preValidateJob, postValidateJob);
        }
    }
}
