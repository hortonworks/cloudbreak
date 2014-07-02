##Technology

Cloudbreak is built on the foundation of cloud providers APIs, Apache Ambari, Docker containers, Serf and dnsmasq.

###Apache Ambari

The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/documentation/docs/images/ambari-overview.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL2RvY3VtZW50YXRpb24vZG9jcy9pbWFnZXMvYW1iYXJpLW92ZXJ2aWV3LnBuZyIsImV4cGlyZXMiOjE0MDQ5MTA5Mzd9--8de37800a298e93934f4e2bd0f9052882d0f91ac)

Ambari enables System Administrators to:

1. Provision a Hadoop Cluster
  * Ambari provides a step-by-step wizard for installing Hadoop services across any number of hosts.
  * Ambari handles configuration of Hadoop services for the cluster.

2. Manage a Hadoop Cluster
  * Ambari provides central management for starting, stopping, and reconfiguring Hadoop services across the entire cluster.

3. Monitor a Hadoop Cluster
  * Ambari provides a dashboard for monitoring health and status of the Hadoop cluster.
  * Ambari leverages Ganglia for metrics collection.
  * Ambari leverages Nagios for system alerting and will send emails when your attention is needed (e.g., a node goes down, remaining disk space is low, etc).

Ambari enables to integrate Hadoop provisioning, management, and monitoring capabilities into applications with the Ambari REST APIs.
Ambari Blueprints are a declarative definition of a cluster. With a Blueprint, you can specify a Stack, the Component layout and the Configurations to materialize a Hadoop cluster instance (via a REST API) without having to use the Ambari Cluster Install Wizard.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/documentation/docs/images/ambari-create-cluster.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL2RvY3VtZW50YXRpb24vZG9jcy9pbWFnZXMvYW1iYXJpLWNyZWF0ZS1jbHVzdGVyLnBuZyIsImV4cGlyZXMiOjE0MDQ5MTEwNTR9--fb259e79df39ef4a0841f3f5e93f1640f1fd553d)

###Docker

Docker is an open platform for developers and sysadmins to build, ship, and run distributed applications. Consisting of Docker Engine, a portable, lightweight runtime and packaging tool, and Docker Hub, a cloud service for sharing applications and automating workflows, Docker enables apps to be quickly assembled from components and eliminates the friction between development, QA, and production environments. As a result, IT can ship faster and run the same app, unchanged, on laptops, data center VMs, and any cloud.

The main features of Docker are:

1. Lightweight, portable
2. Build once, run anywhere
3. VM - without the overhead of a VM
  * Each virtualized application includes not only the application and the necessary binaries and libraries, but also an entire guest operating system
  * The Docker Engine container comprises just the application and its dependencies. It runs as an isolated process in userspace on the host operating system, sharing the kernel with other containers.
  * ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/documentation/docs/images/vm.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL2RvY3VtZW50YXRpb24vZG9jcy9pbWFnZXMvdm0ucG5nIiwiZXhwaXJlcyI6MTQwNDkwOTg1NX0%3D--7d68aec8961722b08f985962c5ad56db62cee220)

4. Containers are isolated
5. It can be automated and scripted


###Serf

Serf is a tool for cluster membership, failure detection, and orchestration that is decentralized, fault-tolerant and highly available. Serf runs on every major platform: Linux, Mac OS X, and Windows. It is extremely lightweight.
Serf uses an efficient gossip protocol to solve three major problems:

  * Membership: Serf maintains cluster membership lists and is able to execute custom handler scripts when that membership changes. For example, Serf can maintain the list of Hadoop servers of a cluster and notify the members when nodes comes online or goes offline.

  * Failure detection and recovery: Serf automatically detects failed nodes within seconds, notifies the rest of the cluster, and executes handler scripts allowing you to handle these events. Serf will attempt to recover failed nodes by reconnecting to them periodically.
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/documentation/docs/images/serf-gossip.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL2RvY3VtZW50YXRpb24vZG9jcy9pbWFnZXMvc2VyZi1nb3NzaXAucG5nIiwiZXhwaXJlcyI6MTQwNDkxMTI3Nn0%3D--07a33c4fb45079236acee058388ac2ff090a3ae2)
  * Custom event propagation: Serf can broadcast custom events and queries to the cluster. These can be used to trigger deploys, propagate configuration, etc. Events are simply fire-and-forget broadcast, and Serf makes a best effort to deliver messages in the face of offline nodes or network partitions. Queries provide a simple realtime request/response mechanism.
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/documentation/docs/images/serf-gossip.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL2RvY3VtZW50YXRpb24vZG9jcy9pbWFnZXMvc2VyZi1nb3NzaXAucG5nIiwiZXhwaXJlcyI6MTQwNDkxMTQzMH0%3D--66a9bdbcae60924b991947428d182a65ade70887)
