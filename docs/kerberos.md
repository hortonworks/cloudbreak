#Kerberos security

Cloudbreak supports Kerberos security for Ambari internal communication. To activate Kerberos with Cloudbreak you should enable security option and fill the

 * `kerberos master key`
 * `kerberos admin`
 * `kerberos password`

fields too on web interface or shell during cluster creation. To run a job on the cluster, you can use one of the default Hadoop users, like `ambari-qa`, as usual.

**Note** Current implementation of Kerberos security doesn't contain Active Directory support or any other third party user authentication method. If you want to use custom user, you have to create users manually with the same name on all Ambari containers on each node.
