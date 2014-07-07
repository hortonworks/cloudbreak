##Accounts

###Cloudbreak account

First and foremost in order to start launching Hadoop clusters you will need to create a Cloudbreak account. C
loudbreak supports registration, forgotten and reset password, and login features at API level.
All password are stored or sent are hashed - communication is always over a secure HTTPS channel. When you are deploying your own Cloudbreak instance we strongy suggest to configure an SSL certificate.
Users create and launch Hadoop clusters on their own namespace and security context. 

Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak **does not** store your Cloud provider account details (such as username, passord, keys, private SSL certificates, etc).
We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak *deployer* is purelly acting on behalf of the end user - without having access to the user's account. 

**How does this work**?

###Configuring the AWS EC2 account

Once you have logged in Cloudbreak you will have to link your AWS account with the Cloudbreak one. In order to do that you will need to do the following steps:

1. Log in AWS management console with the user account you'd like to use with Cloudbreak
2. Go to IAM and select Roles
  * Select Role for Cross-Account access 
    *  Allows IAM users from a 3rd party AWS account to access this account.
    
      **Account ID:** In case you are using our hosted solution you will need to pass SequenceIQ's account id: 755047402263

      **External ID:** provision-ambari (association link)
      
    * Custom policy 
      Use this policy [document](https://raw.githubusercontent.com/sequenceiq/cloudbreak/documentation/src/main/resources/iam-arn-custom.policy?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL2RvY3VtZW50YXRpb24vc3JjL21haW4vcmVzb3VyY2VzL2lhbS1hcm4tY3VzdG9tLnBvbGljeSIsImV4cGlyZXMiOjE0MDUzMzc2MjV9--cde4acae8c67317e6245598526be8cf680a08914) to configure the permission to start EC2 instances on the end user's behalf, and use SNS to receive notifications.
      
  * Select AWS Service Role
    * Amazon EC2
    *  Select Policy template - Read Only Access 
