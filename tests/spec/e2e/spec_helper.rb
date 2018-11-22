require 'aruba/rspec'
require "rspec/json_expectations"
require 'json'
require 'date'
require 'allure-rspec'
require_relative "../common/command_builder"

RSpec.configure do |config|
  config.include AllureRSpec::Adaptor

  config.expect_with :rspec do |expectations|
    expectations.syntax = :expect
  end
  config.color = true
  config.tty = true
  config.formatter = :documentation

  def html_print(&blk)
    puts "<pre>"
    blk.call
    puts "</pre>"
  end
end

AllureRSpec.configure do |config|
  config.output_dir = "allure/allure-results" # - default: gen/allure-results
  config.clean_dir = true # - clean the output directory first? (default: true)
  config.logging_level = Logger::ERROR # - logging level (default: DEBUG)
end