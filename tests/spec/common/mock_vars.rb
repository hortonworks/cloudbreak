RSpec.shared_context "mock shared vars", :a => :b do
  before (:all) {
    @env_name = "mockenv"

    @aws_credential_name = "amazon"

    @aws_role_arn = "arn:aws:iam::1234567890:role/auto-test"
    @aws_access_key = "ABCDEFGHIJKLMNO123BC"
    @aws_secret_key = "+ABcdEFGHIjKLmNo+ABc0dEF1G23HIjKLmNopqrs"

    @mock_password = "mockpassword"

    @mock_endpoint_setup = "/cb/api/setup"
    @mock_endpoint_reset = "/cb/api/reset"
    @mock_password = "mockpassword"

    @environment_file = "../../templates/aws-environment-skeleton.json"
  }
end
