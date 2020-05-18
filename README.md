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
