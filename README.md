This project is a POC project to simulate the NPM server to prove the npm working mechanism. It simulates the "npm install" and "npm publish" actions with a very simple response to let a simple test package can build successfully & publish successfully

Uses following steps to simulate the actions 


* **bin/prepare.sh** - starts the simulation npm registry server, and clone the test npm project with specified npm configurations, you can use -p $port to specify port to start the simulation server.(default is 8000). After prepared, access http://localhost:$port/jquery to see if it is started correctly.(will return a big json content)
* **bin/npm_install.sh** - simulates the "npm install" cli on test project using the simulation registry server.(need to install npm)
* **bin/npm_publish.sh** - simulates the "npm publish" cli on test project using the simulation registry server.(need to install npm)
* **bin/clean_all.sh** - shutdown the server and clean all related resources

When doing the simulated actions, all logs are stored in bin/mvn.out, you can see the request headers for the npm actions there.

**NOTE:** this project needs the following prerequisites:
* java 1.8+
* maven3.x
* npm3+
