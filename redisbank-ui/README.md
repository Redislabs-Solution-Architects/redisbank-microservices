## Redisbank ui app

## Building and publishing

1. Build the app locally using `./mvnw clean package`
1. Build a Docker image using `./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=<yourdockerhubid>/redisbank-ui`
1. Push Docker image using `docker push <yourdockerhubid>/redisbank-ui:latest`

# Deploying on k8s

1. Navigate to the [k8s](k8s) folder.
1. Edit the `ui-app.deployment.yaml` file with the required env vars, or better yet, use k8s secrets. Also add your docker hub id.
1. Deploy the app using `kubectl apply -f ui-app.deployment.yaml`
1. Create a service for the app using `kubectl apply -f ui-app.service.yaml`
1. Edit the `ui-app.route.yaml` file with the required host name of your choice
1. (OpenShift) Create a route for the app using `kubectl apply -f ui-app.route.yaml`
1. (Other ingress) TBD