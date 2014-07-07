##Accounts

###Cloudbreak account

First and foremost in order to start launching Hadoop clusters you will need to create a Cloudbreak account. C
loudbreak supports registration, forgotten and reset password, and login features at API level.
All password are stored or sent are hashed - communication is always over a secure HTTPS channel. When you are deploying your own Cloudbreak instance we strongy suggest to configure an SSL certificate.
Users create and launch Hadoop clusters on their own namespace and security context. 

Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak **does not** store your Cloud provider account details (such as username, passord, keys, private SSL certificates, etc).
We work around the concept of 
