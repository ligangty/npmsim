This project is a POC project to simulate the NPM server to prove the npm working mechanism. It simulates the "npm install" and "npm publish" actions with a very simple response to let a simple test package can build successfully & publish successfully

Uses following steps to simulate the actions 


* **bin/prepare.sh** - starts the simulation npm registry server, and clone the test npm project with specified npm configurations
* **bin/npm_install.sh** - simulates the "npm install" cli on test project using the simulation registry server
* **bin/npm_publish.sh** - simulates the "npm publish" cli on test project using the simulation registry server
* **bin/clean_all.sh** - shutdown the server and clean all related resources

When doing the simulated actions, all logs are stored in bin/mvn.out, you can see the request headers for the npm actions there.

**NOTE:** this project needs the following prerequisites:
* java 1.8+
* maven3.x
* npm4.x