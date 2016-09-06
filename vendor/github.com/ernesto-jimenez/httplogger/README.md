Small golang library useful for logging API requests.

It wraps any http.Transport to log its requests and responses,
including the duration time.

# Usage

See [example/example.go](example/example.go)

```go
package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/ernesto-jimenez/httplogger"
)

func main() {
	client := http.Client{
		Transport: httplogger.NewLoggedTransport(http.DefaultTransport, newLogger()),
	}

	client.Get("http://google.com")
}

type httpLogger struct {
	log *log.Logger
}

func newLogger() *httpLogger {
	return &httpLogger{
		log: log.New(os.Stderr, "log - ", log.LstdFlags),
	}
}

func (l *httpLogger) LogRequest(req *http.Request) {
	l.log.Printf(
		"Request %s %s",
		req.Method,
		req.URL.String(),
	)
}

func (l *httpLogger) LogResponse(req *http.Request, res *http.Response, err error, duration time.Duration) {
	duration /= time.Millisecond
	if err != nil {
		l.log.Println(err)
	} else {
		l.log.Printf(
			"Response method=%s status=%d durationMs=%d %s",
			req.Method,
			res.StatusCode,
			duration,
			req.URL.String(),
		)
	}
}
```

Output:

```
% go run example/example.go
log - 2014/08/17 02:19:19 Request GET http://google.com
log - 2014/08/17 02:19:19 Response method=GET status=302
durationMs=85 http://google.com
log - 2014/08/17 02:19:19 Request GET
http://www.google.co.uk/?gfe_rd=cr&ei=GwPwU4GtPMKo8we3koKwDg
log - 2014/08/17 02:19:20 Response method=GET status=200
durationMs=138
http://www.google.co.uk/?gfe_rd=cr&ei=GwPwU4GtPMKo8we3koKwDg
```

# LICENSE

Copyright (c) 2015 Ernesto Jimenez

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
