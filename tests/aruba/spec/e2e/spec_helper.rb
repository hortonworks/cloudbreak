require 'aruba/rspec'
require "rspec/json_expectations"
require 'json'
require 'date'
require 'allure-rspec'
require_relative "../common/command_builder"

def html_print(&blk)
  puts "<pre>"
  blk.call
  puts "</pre>"
end

RSpec.configure do |c|
  c.include AllureRSpec::Adaptor
end

AllureRSpec.configure do |c|
  c.output_dir = "allure/allure-results" # default: gen/allure-results
  c.clean_dir = true # clean the output directory first? (default: true)
  c.logging_level = Logger::ERROR # logging level (default: DEBUG)
end