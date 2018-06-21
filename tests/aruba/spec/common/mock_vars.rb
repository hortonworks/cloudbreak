RSpec.shared_context "mock shared vars", :a => :b do
  before (:all) { 
    @os_tenant_user = "mockuser"
    @os_tenant_name = "mocktenant"
    @os_tenant_endpoint = "mockendpoint"

    @aws_role_arn = "arn:aws:iam::1234567890:role/auto-test"
    @aws_access_key = "ABCDEFGHIJKLMNO123BC"
    @aws_secret_key = "+ABcdEFGHIjKLmNo+ABc0dEF1G23HIjKLmNopqrs"

    @az_subscription_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g"
    @az_tenant_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g"
    @az_app_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g"

    @gcp_project_id = "cloudbreak"
    @gcp_service_account_id = "1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com"
    @gcp_service_account_private_key_file = "../../keys/test.p12"

    @valid_cred_json = "{\"Name\":\"test\"}"
    @invalid_cred_json = "{\"Other\":\"test\"}"

    @imagecatalog_invalid_json = "https://gist.githubusercontent.com/mhalmy/a206484148d0cb02085bfbd8a58af97f/raw/f9c0a0fea59a67e1e7a" +
    "80f5e7b6defd97a69f26f/imagecatalog_invalid.json"

  	@db_name = "mockdbname" 
  	@db_url = "jdbc:postgresql://test:99/mock" 
  	@db_user = "mockuser" 
    @ldap_url = "ldap://mock-12345.elb.eu-west-1.amazonaws.com:89" 
    
    @mock_password = "mockpassword"	
  }
end

