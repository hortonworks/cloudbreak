sed "s/CM_VERSION/$1/g" config.json > config-temp.json
java -jar swagger-codegen-cli.jar generate -i cm-swagger-$1.json -l spring --library spring-mvc -c config-temp.json -o .
rm config-temp.json