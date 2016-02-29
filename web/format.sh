#!/usr/bin/env bash

WEB_LOCATION="$(cd `dirname "${BASH_SOURCE[0]}"` && pwd)"

find $WEB_LOCATION -type f -name "*.js" | grep -v -e "/lib/" -e "/node_modules/" -e "/app/static/bower_components" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $WEB_LOCATION/.jsbeautifyrc --replace --type "js"
find $WEB_LOCATION -type f -name "*.css" | grep -v -e "/app/static/bower_components" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $WEB_LOCATION/.jsbeautifyrc --replace --type "css"
find $WEB_LOCATION -type f -name *.html | grep -v -e "/node_modules" -e "/app/static/bower_components"  | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $WEB_LOCATION/.jsbeautifyrc --replace --type "html"
find $WEB_LOCATION -type f -name *.tag -print0 | xargs -0 -I FILE js-beautify FILE --config $WEB_LOCATION/.jsbeautifyrc --replace --type "html"

