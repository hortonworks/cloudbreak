package client

import (
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"regexp"
	"strings"
)

func GetToken(identityUrl string, username string, password string, clientId string) string {
	form := url.Values{"credentials": {fmt.Sprintf(`{"username":"%s","password":"%s"}`, username, password)}}
	req, _ := http.NewRequest("POST", fmt.Sprintf("%s?response_type=token&client_id=%s", identityUrl, clientId), strings.NewReader(form.Encode()))
	req.Header.Add("Accept", "application/x-www-form-urlencoded")
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded")

	client := &http.Client{
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			return errors.New("Don't redirect!")
		},
	}

	resp, _ := client.Do(req)
	location := resp.Header.Get("Location")
	regexp := regexp.MustCompile("access_token=(.*)&expires_in")
	tokenBytes := regexp.Find([]byte(location))
	tokenString := string(tokenBytes)
	token := tokenString[13 : len(tokenString)-11]
	return token
}
