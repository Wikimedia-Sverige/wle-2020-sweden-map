Depends on JWBF:

```
git clone git@github.com:eldur/jwbf.git
cd jwbf
mvn install -Dtest=false -DfailIfNoTests=false
```

Start API server on port 8080 by executing `./gradlew build run` from the root.

Webapp available in `client/src/main/webapp`

Our Nginx setup:
```
server {
	server_name _;
	listen 80;
	root /home/wle-2020-sweden-map/client/src/main/webapp;
	location /api/ {
		proxy_pass	http://localhost:8080/api/;
	}
	client_max_body_size 1M;
}
```

In order to keep the index warm,
setup a cron job that execute a query matching large parts of (or all) the data now and then.

```
# m h  dom mon dow   command
* * * * * curl --header "Content-Type: application/json"   --request POST   --data '{"boundingBox":{"southLatitude":58.06625598088457,"westLongitude":14.886474609375002,"northLatitude":60.71888458495197,"eastLongitude":19.1436767578125},"distanceTolerance":0.05}'   http://localhost:8080/api/nvr/search/envelope > /dev/null 2>&1
```
