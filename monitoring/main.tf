terraform {
    required_version = ">=0.12.0"
}

provider "datadog" {
  api_key = var.datadog_api_key
  app_key = var.datadog_app_key
}

resource "datadog_dashboard_list" "cloudbreak_monitoring_list" {
    depends_on = [
        datadog_dashboard.rest_monitors
    ]

    name = "Cloudbreak Monitoring"
    dash_item {
        type = "custom_timeboard"
        dash_id = datadog_dashboard.rest_monitors.id
    }
}

resource "datadog_dashboard" "rest_monitors" {
    title         = "Rest Communication Monitoring"
    description   = "HTTP request/response based graphs"
    layout_type   = "ordered"
    is_read_only  = true
    widget {
        timeseries_definition {
            request {
                q = "avg:monitoring.http_server_requests_seconds{outcome:success} by {uri,method}"
                display_type ="line"
                style {
                    palette = "dog_classic"
                    line_type = "solid"
                    line_width = "normal"
                }
            }
            title = "Avg Success Respons times by URL"
            show_legend = true
            time = {
                live_span = "1d"
            }
            yaxis {
                scale = "linear"
                include_zero = true
                max = "auto"
            }
        }
    }
    widget {
        timeseries_definition {
            request {
                q = "count_nonzero(sum:monitoring.http_server_requests_seconds{outcome:success} by {uri,method})"
                display_type ="line"
                style {
                    palette = "dog_classic"
                    line_type = "solid"
                    line_width = "normal"
                }
            }
            title = "Success Responses"
            show_legend = true
            time = {
                live_span = "1d"
            }
            yaxis {
                scale = "linear"
                include_zero = true
                max = "auto"
            }
        }
    }
    widget {
        timeseries_definition {
            request {
                q = "count_nonzero(sum:monitoring.http_server_requests_seconds{outcome:client_error} by {uri,method})"
                display_type ="line"
                style {
                    palette = "dog_classic"
                    line_type = "solid"
                    line_width = "normal"
                }
            }
            title = "Client Error Responses"
            show_legend = true
            time = {
                live_span = "1d"
            }
            yaxis {
                scale = "linear"
                include_zero = true
                max = "auto"
            }
        }
    }
    widget {
        timeseries_definition {
            request {
                q = "avg:monitoring.http_server_requests_seconds{outcome:client_error} by {uri,method}"
                display_type ="line"
                style {
                    palette = "dog_classic"
                    line_type = "solid"
                    line_width = "normal"
                }
            }
            title = "Avg Client Error Respons times by URL"
            show_legend = true
            time = {
                live_span = "1d"
            }
            yaxis {
                scale = "linear"
                include_zero = true
                max = "auto"
            }
        }
    }
    widget {
        timeseries_definition {
            request {
                q = "count_not_null(sum:monitoring.http_server_requests_seconds{status:500} by {uri,method})"
                display_type ="line"
                style {
                    palette = "dog_classic"
                    line_type = "solid"
                    line_width = "normal"
                }
            }
            title = "Server Error Responses"
            show_legend = true
            time = {
                live_span = "1d"
            }
            yaxis {
                scale = "linear"
                include_zero = true
                max = "auto"
            }
        }
    }
}