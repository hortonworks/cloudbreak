package httplogger

import (
	"log"
	"net/http"
	"time"
)

type loggedRoundTripper struct {
	rt  http.RoundTripper
	log HTTPLogger
}

func (c *loggedRoundTripper) RoundTrip(request *http.Request) (*http.Response, error) {
	c.log.LogRequest(request)
	startTime := time.Now()
	response, err := c.rt.RoundTrip(request)
	duration := time.Since(startTime)
	c.log.LogResponse(request, response, err, duration)
	return response, err
}

// NewLoggedTransport takes an http.RoundTripper and returns a new one that logs requests and responses
func NewLoggedTransport(rt http.RoundTripper, log HTTPLogger) http.RoundTripper {
	return &loggedRoundTripper{rt: rt, log: log}
}

// HTTPLogger defines the interface to log http request and responses
type HTTPLogger interface {
	LogRequest(*http.Request)
	LogResponse(*http.Request, *http.Response, error, time.Duration)
}

// DefaultLogger is an http logger that will use the standard logger in the log package to provide basic information about http responses
type DefaultLogger struct {
}

// LogRequest doens't do anything since we'll be logging replies only
func (dl DefaultLogger) LogRequest(*http.Request) {
}

// LogResponse logs path, host, status code and duration in milliseconds
func (dl DefaultLogger) LogResponse(req *http.Request, res *http.Response, err error, duration time.Duration) {
	duration /= time.Millisecond
	if err != nil {
		log.Printf("HTTP Request method=%s host=%s path=%s status=error durationMs=%d error=%q", req.Method, req.Host, req.URL.Path, duration, err.Error())
	} else {
		log.Printf("HTTP Request method=%s host=%s path=%s status=%d durationMs=%d", req.Method, req.Host, req.URL.Path, res.StatusCode, duration)
	}
}

// DefaultLoggedTransport wraps http.DefaultTransport to log using DefaultLogger
var DefaultLoggedTransport = NewLoggedTransport(http.DefaultTransport, DefaultLogger{})
