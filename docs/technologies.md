##Technology

Cloudbreak is built on the foundation of cloud providers APIs, Apache Ambari, Docker containers, Serf and dnsmasq.

###Apache Ambari

The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs.

Ambari enables System Administrators to:

1. Provision a Hadoop Cluster
..* Ambari provides a step-by-step wizard for installing Hadoop services across any number of hosts.
..* Ambari handles configuration of Hadoop services for the cluster.

2. Manage a Hadoop Cluster
..* Ambari provides central management for starting, stopping, and reconfiguring Hadoop services across the entire cluster.

3. Monitor a Hadoop Cluster
..* Ambari provides a dashboard for monitoring health and status of the Hadoop cluster.
..* Ambari leverages Ganglia for metrics collection.
..* Ambari leverages Nagios for system alerting and will send emails when your attention is needed (e.g., a node goes down, remaining disk space is low, etc).

Ambari enables to integrate Hadoop provisioning, management, and monitoring capabilities into applications with the Ambari REST APIs.

### Docker

Docker is an open platform for developers and sysadmins to build, ship, and run distributed applications. Consisting of Docker Engine, a portable, lightweight runtime and packaging tool, and Docker Hub, a cloud service for sharing applications and automating workflows, Docker enables apps to be quickly assembled from components and eliminates the friction between development, QA, and production environments. As a result, IT can ship faster and run the same app, unchanged, on laptops, data center VMs, and any cloud.

The main features of Docker are:

1. Lightweight, portable
2. Build once, run anywhere
3. VM - without the overhead of a VM
4. Containers are isolated
5. It can be automated and scripted
