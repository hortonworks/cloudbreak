##Releases

When we have started to work on Cloudbreak the idea was to `democratize` the usage of Hadoop in the cloud and VMs. For us this was a necesity as we often had to deal with different Hadoop versions, distributions and cloud providers. 

Also we needed to find a way to speed up the process of adding new cloud providers, and be able to `ship` Hadoop between clouds without re-writing and re-engineering our code base each and every time - welcome **Docker**.

All the Hadoop ecosystem related code, configuration and serivces are inside Docker containers - and these containers are launched on VMs of cloud providers or physical hardware - the end result is the same: a **resilient and dynamic** Hadoop cluster.

We needed to find a unified way to provision, manage and configurure Hadoon clusters - welcome **Apache Ambari**.

###Current release - 0.1
The first public beta version of Cloudbreak supports Hadoop on Amazon's EC2 and Microsoft's Azure cloud providers. The currently supported Hadoop is the Hortonworks Data Platform - the 100% open source Hadoop distribution.

Versions:

CentOS - 6.5
Hortonworks Data Platform - 2.1
Apache Hadoop - 2.4.0
Apache Tez - 0.4
Apache Pig - 0.12.1
Apache Hive & HCatalog - 0.13.0
Apache HBase - 0.98.0
Apache Phoenix - 4.0.0
Apache Accumulo - 1.5.1
Apache Storm - 0.9.1
Apache Mahout - 0.9.0
Apache Solr - 4.7.2
Apache Falcon - 0.5.0
Apache Sqoop - 1.4.4
Apache Flume - 1.4.0
Apache Ambari - 1.6.1
Apache Oozie - 4.0.0
Apache Zookeeper - 3.4.5
Apache Knox - 0.4.0
Docker - 1.1
Serf - 0.5.0
dnsmasq - 2.7

###Future releases

**Hadoop distributions**

There is a great effort by the community and SequenceIQ to bring [Apache Bigtop](http://bigtop.apache.org/) - the Apache Hadoop distribution - under the umbrella of Apache Ambari. Once this effort is finished, Cloudbreak will support Apache Bigtop as a Hadoop distribution as well.

In the meantime we have started an internal R&D project to bring Cloudera's CDH distribution under Apache Ambari - in case you would like to collaborate in this task with us or sounds interesting to you don't hesitate to let us know.

**Cloud providers**

While we have just released the first public beta version of Cloudbreak, we have already started working on other cloud providers - namely Google Cloud Compute and Digital Ocean. 
We have received many requests from people to integrate Cloudbreak wih 3d party hypervisors and cloud providers - as IBM's Softlayer. In case you'd like to have your favorite cloud provider listed don't hesitate to contact us or use our SDK and process to add yours.

Enjoy Cloudbreak - the Hadoop as a Service API which brings you a Hadoop ecosystem in matters on minutes. You are literarily one click or two REST calls away from a fully functional, distributed Hadoop cluster.




