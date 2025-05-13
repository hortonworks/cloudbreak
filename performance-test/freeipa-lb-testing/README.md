# LDAP performance testing with Apache Jmeter
In this section I would like to show you how you can run ldapsearch queries for a FreeIPA server trough load balancer.
## 1. Prerequisites
### Apache Jmeter
For mac the easiest way to install Jmeter is installing with brew
```
brew install jmeter
```
Install Jmeter  on linux:
```
wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xvzf apache-jmeter-5.6.3.tgz
sudo mv apache-jmeter-5.6.3 /opt/jmeter
```
On both operating system you can launch the Apache Jmeter with the `jmeter` command from command line.

### Cloud provider CLI
For the LDAP tests Azure or AWS cli is **required** on the machine where the test is running.

### Prerequisites for the test cluster
To be able to reach the LDAP server on the FreeIPA we need some extra network configuration

- Add the **389** LDAP service port to the application.yaml in the freeipa application like this:
    ```
    targets:
      53: TCP_UDP
      88: TCP_UDP
      389: TCP
      636: TCP
      749: TCP
      4444: UDP
    ```
    Or add this environment variable to the freeipa application
    ```
    freeipa.loadbalancer.targets.389=TCP
    ```
- Open the 389 port on the security group of the load balancer

## 2. Run a Jmeter test plan
To run a Jmeter test plan you need to open a JMX file based on witch cloud provider would like to test. The JMX files are located in the `performance-test/freeipa-lb-testing` directory.
- After you opened the test suite you need to set the value of the following variables:
    - **LB_ADDRESS**: The load balancer IP address
    - **LDAP_PW**: The password of the user
      
      Run the following commands on a FreeIPA instance to get the password:
      ```
      sudo -i
      source activate_salt_env
      echo $(salt -L "$(hostname -f)" pillar.get freeipa:password| tail -n 1 | awk '{print $1}')
      ```
    - **INSTANCE_ID_0 and INSTANCE_ID_1**: The ids of the FreeIPA instances which you want to stop during the test


- After you set everything up you can run the test plan by clicking on the green play button in the top left corner of the Jmeter window.
- You can see the results of the test in the **View Results Tree** tab.
- You can also run the test plan from the command line with the following command:
  ```
  jmeter -n -t performance-test/freeipa-lb-testing/ldap_test_aws.jmx -l /results/file.jtl
  ```
