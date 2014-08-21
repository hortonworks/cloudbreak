<<<<<<< HEAD
**This page has been moved. Please follow up with this link:** [http://sequenceiq.com/cloudbreak-docs/](http://sequenceiq.com/cloudbreak-docs/)
=======
Periscope
=========

*Periscope is a powerful, fast, thick and top-to-bottom right-hander, eastward from Sumbawa's famous west-coast. Timing is critical, as needs a number of elements to align before it shows its true colors.*

*Periscope brings QoS and autoscaling to Hadoop YARN. Built on cloud resource management and YARN schedulers, allows to associate SLA policies to applications.*


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

##Technology

###Apache YARN

Since the emergence of Hadoop 2 and the YARN based architecture we have a platform where we can run multiple applications (of different types) not constrained only to MapReduce.

The idea of YARN is to have a global ResourceManager (RM) and per-application ApplicationMaster (AM). The ResourceManager and per-node slave, the NodeManager (NM), form the data-computation framework. The ResourceManager is the ultimate authority that arbitrates resources among all the applications in the system. The per-application ApplicationMaster is, in effect, a framework specific library and is tasked with negotiating resources from the ResourceManager and working with the NodeManager(s) to execute and monitor the tasks.


Different applications or different MapReduce job profiles have different resource needs, however since Hadoop 2 is a multi tenant platform the different users could have different access patterns or need for cluster capacity. In Hadoop 2.0 this is achieved through YARN schedulers — to allocate resources to various applications subject to constraints of capacities and queues. The Scheduler is responsible for allocating resources to the various running applications subject to familiar constraints of capacities, queues etc. The Scheduler is pure scheduler in the sense that it performs no monitoring or tracking of status for the application.

Periscope is using the Capacity Scheduler to apply SLA policies to applications.

###Apache Ambari

The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs.

![](https://raw.githubusercontent.com/sequenceiq/periscope/master/docs/images/ambari-overview.png)

Ambari enables System Administrators to:

1. Provision a Hadoop Cluster
  * Ambari provides a step-by-step wizard for installing Hadoop services across any number of hosts.
  * Ambari handles configuration of Hadoop services for the cluster.

2. Manage a Hadoop Cluster
  * Ambari provides central management for starting, stopping, and reconfiguring Hadoop services across the entire cluster.

3. Monitor a Hadoop Cluster
  * Ambari provides a dashboard for monitoring health and status of the Hadoop cluster.
  * Ambari leverages Ganglia for metrics collection.
  * Ambari leverages Nagios for system alerting and will send emails when your attention is needed (e.g. a node goes down, remaining disk space is low, etc).

Ambari enables to integrate Hadoop provisioning, management and monitoring capabilities into applications with the Ambari REST APIs.
Ambari Blueprints are a declarative definition of a cluster. With a Blueprint, you can specify a Stack, the Component layout and the Configurations to materialise a Hadoop cluster instance (via a REST API) without having to use the Ambari Cluster Install Wizard.

![](https://raw.githubusercontent.com/sequenceiq/periscope/master/docs/images/ambari-create-cluster.png)

###Cloudbreak

Cloudbreak is SequenceIQ's RESTful Hadoop as a Service API. Once it is deployed in your favourite servlet container exposes a REST API allowing to span up Hadoop clusters of arbitrary sizes on your selected cloud provider. Provisioning Hadoop has never been easier. Cloudbreak is built on the foundation of cloud providers API (Amazon AWS, Microsoft Azure, Google Cloud Compute...), Apache Ambari, Docker containers, Serf and dnsmasq.

Fir further documentation please check the [Cloudbreak documentation](http://sequenceiq.com/cloudbreak).

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
An SLA scaling policy can contain multiple alarms. As an alarm is triggered a  `scalingAdjustment` is applied, however to keep the cluster size within boundaries a `minSize` and `maxSize` is attached to the cluster - thus a scaling policy can never over or undersize a cluster. Also in order to avoid stressing the cluster we have introduced a `cooldown` period (minutes) - though an alarm is raised and there is an associated scaling policy, the system will not apply the policy within the configured timeframe. In an SLA scaling policy the triggered rules are applied in order.

###Applications
A Hadoop YARN application is a packaged workload submitted to a cluster. An application requests resources from YARN Resource Manager. The resources are allocated as YARN containers. By default Periscope works with the Hadoop YARN Capacity Scheduler. Using the Capacity Scheduler applications are submitted in different priority queues. The queue configurations, their depth, associated resources, etc have to be designed ahead - and adapted in case of new tenants, applications or workloads are using the cluster.
At SequenceIQ, through our contributions to Apache YARN we facilitate moving applications between queues - and thus use the SLA policies attached to these queues. Even more, those SLA policies which were previously attached to Capacity Scheduler queues now can be attached to submitted jobs/applications.
Also we facilitate changing the resources allocated to a running application - even though they were submitted and already running.
_Note: not all of the features above are supported in the first `public beta` version. There are dependencies we contributed to Hadoop, Ambari and YARN and they will be included in the next releases (1.7 and 2.6)_

###Configuration

Periscope brings in the capability to reconfigure a running cluster - in particular resource properties heavily used. These properties currently are mostly related to the Capacity Scheduler configurations, but as we add functionality to Periscope this set of properties will constantly increase.

##QuickStart and installation

The easiest way to start using Periscope is to use our hosted solution. We host, maintain and support [Periscope API](https://periscope-api.sequenceiq.com) for you.

Periscope requires an Apache Ambari endpoint of your Hadoop cluster to start to apply your SLA policies. We suggest to start with [Cloudbreak](http://sequenceiq.com/cloudbreak/#quickstart-and-installation). Create a hosted free [Cloudbreak](https://cloudbreak.sequenceiq.com/) account and start experimenting.


##Releases, future plans

Quite a while ago we have been thinking about `autoscaling` Hadoop clusters. First of all - being a cost aware young startup - we always had to manually manage our cloud based VM instances, doing what exactly Periscope does. Having short and long running Hadoop jobs on different clusters, and maintaining in parallel different clusters it was a very error prone and tedious job. While Amazon for instance gives a quite good API (remember we always use a CLI as an alternative for UI) but this still wasn’t easy when you have 10+ clusters of different sizes. On the other hand we have started to use different cloud providers as well - Microsoft’s Azure and Google’s Cloud Compute.
This diversity started to eat into too much DevOps time - and we decided to automate everything and create Periscope.

###  Public Beta - 0.1

The `first public beta` does support autoscaling clusters on **Amazon AWS** and **Microsoft Azure** - and we will bring in the other Cloudbreak providers as we add them. Once our contributions in Apache Hadoop, YARN and Ambari will be released (patches are accepted and in trunk - target versions are *2.6.0 and 1.7.0* - Periscope will start supporting the `static` cluster features such as application SLA policies.

The currently supported Hadoop is the Hortonworks Data Platform - the 100% open source Hadoop distribution and the respective component versions are:

CentOS - 6.5 Hortonworks Data Platform - 2.1 Apache Hadoop - 2.4.0 Apache Tez - 0.4 Apache Pig - 0.12.1 Apache Hive & HCatalog - 0.13.0 Apache HBase - 0.98.0 Apache Phoenix - 4.0.0 Apache Accumulo - 1.5.1 Apache Storm - 0.9.1 Apache Mahout - 0.9.0 Apache Solr - 4.7.2 Apache Falcon - 0.5.0 Apache Sqoop - 1.4.4 Apache Flume - 1.4.0 Apache Ambari - 1.6.1 Apache Oozie - 4.0.0 Apache Zookeeper - 3.4.5 Apache Knox - 0.4.0 Docker - 1.1 Serf - 0.5.0 dnsmasq - 2.7

###Future releases

While this is already a good achievement - bringing autoscaling to Hadoop - we don’t stop here. Periscope will be a centralized place to manage your cluster through SLA policies, check your cluster metrics and logs and correlate them with events/cluster heath. The analytics and visualization capabilities will allow you for a deeper understanding of your jobs, the nature of resources consumed and ultimately leverage the features provided by cloud providers. For instance a CPU heavy job can always launch purpose build (compute optimized) instance types.

As we have already mentioned we are running a YARN monitoring project based on R - based on the experience and what we have learnt the end goal is to built a high level heuristic model which maintains a healthy cluster, without the need of predefined SLA policy rules.


##Contribution

So you are about to contribute to Periscope? Awesome! There are many different ways in which you can contribute. We strongly value your feedback, questions, bug reports, and feature requests.
Periscope consist of the following main projects:

###Periscope code

Available: <a href=https://github.com/sequenceiq/periscope>https://github.com/sequenceiq/periscope</a>

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
