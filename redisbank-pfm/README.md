## Redisbank test data generation app

## Building and publishing

1. Build the app locally using `./mvnw clean package`
1. Build a Docker image using `./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=<yourdockerhubid>/redisbank-pfm`
1. Push Docker image using `docker push <yourdockerhubid>/redisbank-pfm:latest`
