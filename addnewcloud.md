##Add new cloud providers

Cloudbreak is built from ground up on the idea of being cloud provider agnostic. All the external API's are cloud agnostic, and we have 
internally abstracted wokring with individual cloud providers API's. Nevertheless adding new cloud providers is extremely important for us, thus 
in order to speed up the process and linking the new provider API with Cloudbreak we came up with an SDK and list of responsibilities.
Once these interfaces are implemented, and the different providers API calls are `translated` you are ready to go.

Though we are working on a few popular providers to add to Cloudbreak we'd like to hear your voice as well - your ideas, provider requests or `contribution` is highly appreciated.

1. Metadata service

2. Notifications

3. Account management
