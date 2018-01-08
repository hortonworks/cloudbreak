# cbm
1. get swagger-codegen.jar 

2. get swagger.json

3. java -jar swagger-codegen-cli.jar generate -i swagger.json -l nodejs-server -o cbm

4. modify api/swagger.json basepath to /cb/api
