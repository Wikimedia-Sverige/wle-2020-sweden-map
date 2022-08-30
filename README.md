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
For Apache:
```
<VirtualHost *:80>
        [...]
        DocumentRoot /home/wle-2020-sweden-map/client/src/main/webapp
        [...]
        ProxyPass /api/ http://localhost:8080/api/
        ProxyPassReverse /api/ http://localhost:8080/api/
</VirtualHost>
```

In order to keep the index warm,
setup a cron job that execute a query matching large parts of (or all) the data now and then.

```
# m h  dom mon dow   command
* * * * * curl --header "Content-Type: application/json"   --request POST   --data '{"boundingBox":{"southLatitude":58.06625598088457,"westLongitude":14.886474609375002,"northLatitude":60.71888458495197,"eastLongitude":19.1436767578125},"distanceTolerance":0.05}'   http://localhost:8080/api/nvr/search/envelope > /dev/null 2>&1
```

If the server uses a lot of CPU, add a gradle.properties file with `org.gradle.jvmargs=-Xmx1000m`.
