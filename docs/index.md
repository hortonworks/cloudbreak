<<<<<<< HEAD
**This page has been moved. Please follow up with this link:** [http://sequenceiq.com/cloudbreak-docs/](http://sequenceiq.com/cloudbreak-docs/)
=======
Periscope
=========

*Periscope is a powerful, fast, thick and top-to-bottom right-hander, eastward from Sumbawa's famous west-coast. Timing is critical, as needs a number of elements to align before it shows its true colors.*

*Periscope is a heuristic Hadoop scheduler you associate with a QoS profile. Built on YARN schedulers, cloud and VM resource management API's it allows you to associate SLAs to applications and customers.*

##Overview

The purpose of Periscope is to bring QoS to a multi-tenant Hadoop cluster, while allowing to apply SLA policies to individual applications.
At [SequenceIQ](http://sequenceiq.com) working with multi-tenant Hadoop clusters for quite a while we have always seen the same frustration and fight for resource between users.
The **FairScheduler** was partially solving this problem - bringing in fairness based on the notion of [Dominant Resource Fairness](http://static.usenix.org/event/nsdi11/tech/full_papers/Ghodsi.pdf).
With the emergence of Hadoop 2 YARN and the **CapacityScheduler** we had the option to maximize throughput and utilization for a multi-tenant cluster in an operator-friendly manner.
The scheduler works around the concept of queues. These queues are typically setup by administrators to reflect the economics of the shared cluster.
While this is a pretty good abstraction and brings some level of SLA for predictable workloads, it often needs proper design ahead.
The queue hierarchy and resource allocation needs to be changed when new tenants and workloads are moved to the cluster.

Periscope was designed around the idea of `autoscaling` clusters - without any need to preconfigure queues, cluster nodes or apply capacity planning ahead.

##How it works

Periscope monitors the application progress, the number of YARN containers/resources and their allocation, queue depths, and the number of available cluster nodes and their health. 
Since we have switched to YARN a while ago (been among the first adopters) we have run an open source [monitoring project](https://github.com/sequenceiq/yarn-monitoring), based on R.
We have been collecting metrics from the YARN Timeline server, Hadoop Metrics2 and Ambari's Nagios/Ganglia - and profiling the applications and correlating with these metrics.
One of the key findings was that while low level metrics are good to understand the cluster health - they might not necessarily help on making decisions when applying different SLA policies on a multi-tenant cluster. 
Focusing on higher level building blocks as queue depth, YARN containers, etc actually brings in the same quality of service, while not being lost in low level details.

Periscope works with two types of Hadoop clusters: `static` and `dynamic`. Periscope does not require any pre-installation - the only thing it requires is to be `attached` to an Ambari server's REST API.

##Building blocks

###Clusters
A Hadoop cluster is a set of components and services launched in order to store, analyze and process unstructured data. Periscope can work with any Hadoop 2/ YARN cluster provisioned with Apache Ambari, and supports any YARN application.
As highlighted before, Periscope can apply SLA policies to `static` and `autoscaling` clusters. Due to flexibility supported by cloud based Hadoop deployments, we suggest to link Periscope with [Cloudbreak](http://sequenceiq.com/cloudbreak/) and apply policy based `autoscaling` to your cluster.

**Static clusters**
From Periscope point of view we consider a cluster `static` when the cluster capacity can't be increased horizontally.
This means that the hardware resources are already given - and the throughput can't be increased by adding new nodes.
Periscope introspects the job submission process, monitors the applications and applies the following SLAs:

  1. Application ordering - can guaranty that a higher priority application finishes before another one (supporting parallel or sequential execution)
  2. Moves running applications between priority queues
  3. *Attempts* to enforce time based SLA (execution time, finish by, finish between, recurring)
  4. *Attempts* to enforce guaranteed cluster capacity requests ( x % of the resources)
  5. Support for distributed (but not YARN ready) applications using Apache Slider
  6. Attach priorities to SLAs

_Note: not all of the features above are supported in the first `public beta` version. There are dependencies we contributed to Hadoop, Ambari and YARN and they will be included in the next releases (1.7 and 2.6)_

**Autoscaling clusters**
From Periscope point of view we consider a cluster `dynamic` when the cluster capacity can be increased horizontally.
This means that nodes can be added or removed on the fly - thus the cluster’s throughput can be increased or decreased based on the cluster load and scheduled applications.
Periscope works with [Cloudbreak](http://sequenceiq.com/cloudbreak/) to add or remove nodes from the cluster based on the SLA policies and thus continuously provide a high *quality of service* for the multi-tenand Hadoop cluster.
Just to refresh memories - [Cloudbreak](http://sequenceiq.com/products.html) is [SequenceIQ's](http://sequenceiq.com) open source, cloud agnostic Hadoop as a Service API.
Given the option of provisioning or decommissioning cluster nodes on the fly, Periscope allows you to use the following set of SLAs:

  1. Application ordering - can guaranty that a higher priority application finishes before another one (supporting parallel or sequential execution)
  2. Moves running applications between priority queues
  3. *Enforce* time based SLA (execution time, finish by, finish between, recurring) by increasing cluster capacity and throughput
  4. Smart decommissioning - avoids HDFS storms, keeps `payed` nodes alive till the last minute
  5. *Enforce* guaranteed cluster capacity requests ( x % of the resources)
  6. *Private* cluster requests - supports provisioning of short lived private clusters with the possibility to merge
  7. Support for distributed (but not YARN ready) applications using Apache Slider
  8. Attach priorities to SLAs
_Note: not all of the features above are supported in the first `public beta` version. There are dependencies we contributed to Hadoop, Ambari and YARN and they will be included in the next releases (1.7 and 2.6)_

###Alarms
An alarm watches a `metric` over a specified time period, and used by one or more action or scaling policy based on the value of the metric relative to a given threshold over a number of time periods. In case Periscope raises an alarm an action (e.g. sending an email) or a scaling policy is triggered. Alarms are based on metrics. The current supported `metrics` are: 
*`PENDING_CONTAINERS`- pending YARN containers

*`PENDING_APPLICATIONS` - pending/queued YARN applications

*`LOST_NODES` - cluster nodes lost

*`UNHEALTHY_NODES` - unhealthy cluster nodes

*`GLOBAL_RESOURCES` - global resources 

Measured `metrics` are compared with pre-configured values using operators. The `comparison operators` are: `LESS_THAN`, `GREATER_THAN`, `LESS_OR_EQUAL_THAN`, `GREATER_OR_EQUAL_THAN`, `EQUALS`.
In order to avoid reacting for sudden spikes in the system and apply policies only in case of a sustained system stress, `alarms` have to be sustained over a `period` of time.  The `period` specifies the time period in minutes during the alarm has to be sustained. 

Also a `threshold` can be configured, which specifies the variance applied by the operator for the selected `metric`.

###SLA Scaling Policies
Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application. 
When scaling policies are used, the capacity is automatically increased or decreased according to the conditions defined.
Periscope will do the heavy lifting and based on the alarms and the scaling policy linked to them it executes the associated policy.
By default a fully configured and running [Cloudbreak](https://cloudbreak.sequenceiq.com/) cluster contains no SLA policies. 
An SLA scaling policy can contain multiple alarms. While part of the scaling policies `scalingAdjustment` are applied, also to keep the cluster size within boundaries, a `minSize` and `maxSize` is attached to the cluster - thus a scaling policy - though trough an alarm is triggered - can never over or undersize a cluster.
In an SLA scaling policy the triggered rules are applied in order.
###Applications
A Hadoop YARN application is a packaged workload submitted to a cluster. An application requests resources from YARN Resource Manager. The resources are allocated as YARN containers. By default Periscope works with the Hadoop YARN Capacity Scheduler. Using the Capacity Scheduler applications are submitted in different priority queues. The queue configurations, their depth, associated resources, etc have to be designed ahead - and adapted in case of new tenants, applications or workloads are using the cluster.
At SequenceIQ, through our contributions to Apache YARN we facilitate moving applications between queues - and thus use the SLA policies attached to these queues. Even more, those SLA policies which were previously attached to Capacity Scheduler queues now can be attached to submitted jobs/applications. 
Also we facilitate changing the resources allocated to a running application - even though they were submitted and already running. 
_Note: not all of the features above are supported in the first `public beta` version. There are dependencies we contributed to Hadoop, Ambari and YARN and they will be included in the next releases (1.7 and 2.6)_

###Configuration

Periscope brings in the capability to reconfigure a running cluster - in particular resource properties heavily used. These properties currently are mostly related to the Capacity Scheduler configurations, but as we add functionality to Periscope this set of properties will constantly increase.

##QuickStart and installation

##Releases, future plans

##Contribution

So you are about to contribute to Periscope? Awesome! There are many different ways in which you can contribute. We strongly value your feedback, questions, bug reports, and feature requests.
Periscope consist of the following main projects:



###Periscope API

Available: <a href=https://periscope-api.sequenceiq.com>https://periscope-api.sequenceiq.com</a>

GitHub: <a href=https://github.com/sequenceiq/periscope>https://github.com/sequenceiq/periscope</a>


###Periscope documentation

Product documentation: <a href=http://sequenceiq.com/periscope>http://sequenceiq.com/periscope</a>

GitHub: <a href=https://github.com/sequenceiq/periscope/blob/master/docs/index.md>https://github.com/sequenceiq/periscope/blob/master/docs/index.md</a>


API documentation: <a href=http://docs.periscope.apiary.io>http://docs.periscope.apiary.io</a>

GitHub: <a href=https://github.com/sequenceiq/periscope/blob/master/apiary.apib>https://github.com/sequenceiq/periscope/blob/master/apiary.apib</a>

###Ways to contribute

* Use Periscope and Cloudbreak
* Submit a GitHub issue to the appropriate GitHub repository.
* Submit a new feature request (as a GitHub issue).
* Submit a code fix for a bug.
* Submit a unit test.
* Code review pending pull requests and bug fixes.
* Tell others about these projects.

###Contributing code

We are always thrilled to receive pull requests, and do our best to process them as fast as possible. Not sure if that typo is worth a pull request? Do it! We will appreciate it.
The Periscope projects are open source and developed/distributed under the [Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
If you wish to contribute to Periscope (which you're very welcome and encouraged to do so) then you must agree to release the rights of your source under this license.

####Creating issues

Any significant improvement should be documented as a GitHub issue before starting to work on it. Please use the appropriate labels - bug, enhancement, etc - this helps while creating the release notes for a version release.
Before submitting issues please check for duplicate or similar issues. If you are unclear about an issue please feel free to [contact us](https://groups.google.com/forum/#!forum/periscope).

####Discuss your design

We recommend discussing your plans on the [mailing list](https://groups.google.com/forum/#!forum/periscope) before starting to code - especially for more ambitious contributions. This gives other contributors a chance to point you in the right direction, give feedback on your design, and maybe point out if someone else is working on the same thing.

####Conventions

Please write clean code. Universally formatted code promotes ease of writing, reading, and maintenance.
* Do not use @author tags.

* New classes must match our dependency mechanism.

* Code must be formatted according to our [formatter](https://github.com/sequenceiq/periscope/blob/master/config/eclipse_formatter.xml).

* Code must be checked with our [checkstyle](https://github.com/sequenceiq/periscope/tree/master/config/checkstyle).

* Contributions must pass existing unit tests.

* The code changes must be accompanied by unit tests. In cases where unit tests are not possible or don’t make sense an explanation should be provided.

* New unit tests should be provided to demonstrate bugs and fixes (use Mockito whenever possible).

* The tests should be named *Test.java.

* Use slf4j instead of commons logging as the logging facade.


###Thank you
Huge thanks go to the contributors from the community who have been actively working with the SequenceIQ team. Kudos for that.

###Jobs
Do you like what we are doing? We are looking for exceptional software engineers to join our development team. Would you like to work with others in the open source community?
Please consider submitting your resume and applying for open positions at jobs@sequenceiq.com.


### Legal

*Brought to you courtesy of our legal counsel.*

Use and transfer of Cloudbreak may be subject to certain restrictions by the
United States and other governments.  
It is your responsibility to ensure that your use and/or transfer does not
violate applicable laws.

### Licensing
Cloudbreak is licensed under the Apache License, Version 2.0. See [LICENSE](http://www.apache.org/licenses/LICENSE-2.0.html) for full license text.
>>>>>>> a9d1b17... PERI-2 mkdocs generate
