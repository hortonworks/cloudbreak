*Uluwatu is situated at the southern end of Bali and is the island most famous and consistent wave. The spot offers several waves which are working with different swells and tides.*

*Uluwatu is a web UI for Cloudbreak - a cloud agnostic Hadoop as a Service API.*

##[Cloudbreak UI](https://cloudbreak.sequenceiq.com/)

When we have started to work on Cloudbreak, our main goal was to create an easy to use, cloud and Hadoop distribution agnostic Hadoop as a Service API. Though we always like to automate everything and approach things with a very DevOps mindset, as a side project we have created a UI for Cloudbreak as well.
The goal of the UI is to ease to process and allow you to create a Hadoop cluster on your favourite cloud provider in `one-click`.

The UI is built on the foundation of the Cloudbreak REST API. You can access the UI [here](https://cloudbreak.sequenceiq.com/).

###Manage credentials
Using manage credentials you can link your cloud account with the Cloudbreak account.

**Amazon AWS**

`Name:` name of your credential

`Description:` short description of your linked credential

`Role ARN:` the role string - you can find it at the summary tab of the IAM role

`SSH public key:` if you specify an SSH public key you can use your private key later to log into your launched instances

**Azure**

`Name:` name of your credential

`Description:` short description of your linked credential

`Subscription Id:` your Azure subscription id - see Accounts

`File password:` your generated JKS file password - see Accounts

`SSH public key:` if you specify an SSH public key you can use your private key later to log into your launched instances (The key generation process is described in the Configuring the Microsoft Azure account section)


###Manage templates

Using manage templates you can create infrastructure templates.

**Amazon AWS**

`Name:` name of your template

`Description:` short description of your template

`AMI:` the AMI which contains the Docker containers

`SSH location:` allowed inbound SSH access. Use 0.0.0.0/0 as default

`Region:` AWS region where you'd like to launch your cluster

`Instance type:` the Amazon instance type to be used - we suggest to use at least small or medium instances

**Azure**

`Name:` name of your template

`Description:` short description of your template

`Location:` Azure datacenter location where you'd like to launch your cluster

`Image name:` The Azure base image used

`Instance type:` the Azure instance type to be used - we suggest to use at least small or medium instances

`Password:` if you specify a password you can use it later to log into you launched instances

###Manage blueprints
Blueprints are your declarative definition of a Hadoop cluster.

`Name:` name of your blueprint

`Description:` short description of your blueprint

`Source URL:` you can add a blueprint by pointing to a URL. As an example you can use this [blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/multi-node-hdfs-yarn).

`Manual copy:` you can copy paste your blueprint in this text area

_Note: Apache Ambari community and SequenceIQ is working on an auto-hostgroup assignment algorithm; in the meantime please follow our conventions and check the default blueprints as examples, or ask us to support you._

_1. When you are creating a Single node blueprint the name of the default host group has to be `master`._
_2. When you are creating a Multi node blueprint, all the worker node components (a.k.a. Slaves) will have to be grouped in host groups named `slave_*`. Replace * with the number of Slave hostgroups._

_The default rule is that for multi node clusters there must be at least as many hosts as the number of host groups. Each NOT slave host groups (master, gateway, etc) will be launched with a cardinality of 1 (1 node per master, gateway, etc hosts), and all the rest of the nodes are equally distributed among Slave nodes (if there are multiple slave host groups)._

###Create cluster
Using the create cluster functionality you will create a cloud Stack and a Hadoop Cluster. In order to create a cluster you will have to select a credential first.
_Note: Cloudbreak can maintain multiple cloud credentials (even for the same provider)._

`Cluster name:` your cluster name

`Cluster size:` the number of nodes in your Hadoop cluster

`Template:` your cloud infrastructure template to be used

`Blueprint:` your Hadoop cluster blueprint

Once you have launched the cluster creation you can track the progress either on Cloudbreak UI or your cloud provider management UI.


_Note: Because Azure does not directly support third party public images we will have to copy the used image from VM Depot into your storage account. The steps below need to be finished once and only once before any stack is created for every affinity group:_

_1. Get the VM image - http://vmdepot.msopentech.com/Vhd/Show?vhdId=42480&version=43564_

_2. Copy the VHD blob from above (community images) into your storage account_

_3. Create a VM image from the copied VHD blob._

_This process will take 20 minutes so be patient - but this step will have do be done once and only once._

##Technical details

Uluwatu is a small [node.js](http://nodejs.org/) webapp with an [Angular.js](https://angularjs.org/) frontend. The main logic is on the client side, the node backend has only the following reposibilities:

- provides an HTTP server that serves the static HTML/JS/CSS content
- proxies every request coming from the Angular side to Cloudbreak therefore eliminating the need for [CORS](http://en.wikipedia.org/wiki/Cross-origin_resource_sharing)
- obtains an OAuth2 token to Cloudbreak by handling the authorization code flow

###Running Uluwatu locally

If you'd like to run Uluwatu on your local machine, you should have npm and node.js installed. After checking out the git repository, run `npm install` in Uluwatu's directory and set these environment variables:

- ULU_CLOUDBREAK_ADDRESS: the address of the Cloudbreak backend (format: `http[s]://[host]:[port]`)
- ULU_IDENTITY_ADDRESS: the address of the identity server - you'll either need to [run your own](http://blog.sequenceiq.com/blog/2014/10/16/using-uaa-as-an-identity-server/) UAA server properly configured, or you can use our own identity server deployed to our QA environment. If you'd like to connect to our QA server please contact us for connection details. (format: `http[s]://[host]:[port]`)
- ULU_OAUTH_CLIENT_ID: the `client_id` of the Uluwatu app configured in the UAA server
- ULU_OAUTH_CLIENT_SECRET: the `client_secret` of the Uluwatu app configured in the UAA server
- ULU_OAUTH_REDIRECT_URI: the `redirect_url` of the Uluwatu app configured in the UAA server - when running Uluwatu locally in a dev environment it should be something like `http://localhost:3000/authorize`
- ULU_SULTANS_ADDRESS: [Sultans](https://github.com/sequenceiq/sultans) is SequenceIQ's registration, user management and custom login service. If you'd like to have registration and custom login features you should deploy your own Sultans application and provide its base address here or you can use our deployed version on our QA environment. If you'd like to connect to our QA server please contact us for connection details. (format: `http[s]://[host]:[port]`)
- ULU_SERVER_PORT: (optional, default: 3000) - if you'd like to run Uluwatu on a port different than the default

If the environment variables are set, simply run `node server.js`

###Running Uluwatu in Docker

If you'd like to deploy a stable version of Uluwatu somewhere, we recommend to use its Docker image (that's how we do it in production) so it's not needed to have node installed on the server. This image grabs the latest release from the Github repo and starts the node server. The environment variables should be provided like above.
```
docker run -d --name uluwatu \
 -e "ULU_CLOUDBREAK_ADDRESS=$ULU_CLOUDBREAK_ADDRESS" \
 -e "ULU_IDENTITY_ADDRESS=$ULU_IDENTITY_ADDRESS" \
 -e "ULU_SULTANS_ADDRESS=$ULU_SULTANS_ADDRESS" \
 -e "ULU_OAUTH_CLIENT_ID=$ULU_OAUTH_CLIENT_ID" \
 -e "ULU_OAUTH_CLIENT_SECRET=$ULU_OAUTH_CLIENT_SECRET" \
 -e "ULU_OAUTH_REDIRECT_URI=$ULU_OAUTH_REDIRECT_URI" \
 -p 3000:3000 sequenceiq/uluwatu
 ```

<!--ui.md-->
