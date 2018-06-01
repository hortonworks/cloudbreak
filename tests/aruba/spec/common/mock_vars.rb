RSpec.shared_context "mock shared vars", :a => :b do
  before (:all) { @os_tenant_user = "mockuser" }
  before (:all) { @os_tenant_pwd = "mockpwd" }
  before (:all) { @os_tenant_name = "mocktenant" }
  before (:all) { @os_tenant_endpoint = "mockendpoint" } 


  before (:all) { @aws_role_arn = "arn:aws:iam::1234567890:role/auto-test" }
  before (:all) { @aws_access_key = "ABCDEFGHIJKLMNO123BC" }    
  before (:all) { @aws_secret_key = "+ABcdEFGHIjKLmNo+ABc0dEF1G23HIjKLmNopqrs" }


  before (:all) { @az_subscription_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g" }
  before (:all) { @az_tenant_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g" }    
  before (:all) { @az_app_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g" }
  before (:all) { @az_app_pwd = "mockpwd" }  


  before (:all) { @gcp_project_id = "cloudbreak" }
  before (:all) { @gcp_service_account_id = "1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com" }    
  before (:all) { @gcp_service_account_private_key_file = "../../keys/test.p12" }      


  before (:all) { @valid_cred_json = "{\"Name\":\"test\"}" }
  before (:all) { @invalid_cred_json = "{\"Other\":\"test\"}" } 

  before (:all) { @imagecatalog_invalid_json = "https://gist.githubusercontent.com/mhalmy/a206484148d0cb02085bfbd8a58af97f/raw/f9c0a0fea59a67e1e7a" +
    "80f5e7b6defd97a69f26f/imagecatalog_invalid.json" } 
end

