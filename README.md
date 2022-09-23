# About this repository (work in progress, first release coming 'soon')

This repository uses Redis core data structures, Streams, RediSearch and TimeSeries to build a
Java/Spring Boot/Spring Data Redis Reactive application that shows a searchable transaction overview with realtime updates
as well as a personal finance management overview with realtime balance and biggest spenders updates. UI in Bootstrap/CSS/Vue.

Features in this demo:

- Redis Streams for the realtime transactions
- Redis TimeSeries for the balance over time
- RediSearch for searching transactions
- Sorted Sets for the 'biggest spenders'
- Redis hashes for session storage (via Spring Session)

## Architecture
Contrary to the monolithic version of this application (<https://github.com/Redislabs-Solution-Architects/redisbank>), this repo is setup in a distributed micro-services fashion. This allows components/features to be deployed individually and distributed across clouds/VMs/k8s clusters as desired. See diagram for more info.

## Prerequisites

1. JDK 17 or higher (<https://openjdk.java.net/install/index.html>). Not needed when using Docker.
1. Docker Desktop (<https://www.docker.com/products/docker-desktop>), or Colima with a docker/k8s/containerd runtime.
1. For running on Kubernetes: a Kubernetes cluster
1. A running Redis Stack (<https://redis.io/docs/stack/>) instance or Redis Enterprise (<https://redis.com/redis-enterprise-software/overview/>) cluster (not needed for building/running tests as TestContainers is used there)

## How to run

### Running locally from the commandline using the bundled Maven wrapper

1. Build each app using `./mvnw package`
1. Run Redis Stack using `docker run -p 6379:6379 redis/redis-stack-server:latest`
1. Start each app using `./mvnw spring-boot:run` (the order is not important, but what you'll communicate with from the browser is [redisbank-ui](redisbank-ui))
1. Navigate a browser to [http://localhost:8080](http://localhost:8080) and login

### Using Docker compose
1. Start everything using `docker-compose up`
