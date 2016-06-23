package com.sequenceiq.cloudbreak.domain

class CbUsageFilterParameters private constructor(builder: CbUsageFilterParameters.Builder) {
    val account: String
    val owner: String
    val since: Long?
    val cloud: String
    val region: String
    val filterEndDate: Long?

    init {
        this.account = builder.account
        this.owner = builder.owner
        this.since = builder.since
        this.cloud = builder.cloud
        this.region = builder.region
        this.filterEndDate = builder.filterEndDate
    }

    class Builder {
        private var account: String? = null
        private var owner: String? = null
        private var since: Long? = null
        private var cloud: String? = null
        private var region: String? = null
        private var filterEndDate: Long? = null

        fun setAccount(account: String): Builder {
            this.account = account
            return this
        }

        fun setOwner(owner: String): Builder {
            this.owner = owner
            return this
        }

        fun setSince(since: Long?): Builder {
            this.since = since
            return this
        }

        fun setCloud(cloud: String): Builder {
            this.cloud = cloud
            return this
        }

        fun setRegion(region: String): Builder {
            this.region = region
            return this
        }

        fun setFilterEndDate(filterEndDate: Long?): Builder {
            this.filterEndDate = filterEndDate
            return this
        }

        fun build(): CbUsageFilterParameters {
            return CbUsageFilterParameters(this)
        }
    }
}
