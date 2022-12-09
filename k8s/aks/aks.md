# Deploying to Azure Kubernetes Service

This version of Redis Bank is excellent for demonstrating the active-active capabilities of Redis Enterprise on managed Kubernetes clusters.

For example, you could deploy the microservices and UI in 'UK South', and the data generation service in 'UK West' - Redis will handle the data replication between the regions. 

The instructions below detail the steps for getting Redis Enterprise configured on Azure Kubernetes Servce (AKS) alongside the Redis Bank microservices and UI.

## Prerequisites

1. [An Azure subscription](https://azure.microsoft.com/en-gb/pricing/purchase-options/pay-as-you-go/)
1. [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
1. [Docker](https://docs.docker.com/get-docker/)
1. A domain name and/or DNS zone that you can configure.

## Before you begin

If you are not yet a Redis active-active aficionado, you may find it simpler to deploy a standard Redis Enterprise cluster (and databases) on AKS to begin with. Once everything is working end to end, you can modify the Redis configuration to support active-active and deploy the data generation application to a region seperate from the UI.

## Azure Kubernetes Service

Login to your subscription via the Azure CLI:

```
az login
```
Get your preferred region names (e.g. UK regions):

```
az account list-locations | grep uk
```

Create a resource group:

```
az group create --name <resource-group-name> --location <region>
```
Create an Azure Container Registry (ACR):
```
az acr create --resource-group <resource-group-name> --name <acr> --sku Basic
```
Stand up a cluster:

```
az aks create \
--resource-group <resource-group-name> \
--name <cluster-name> \
--enable-managed-identity \
--node-count 4 \
--generate-ssh-keys \
--attach-acr <acr> \
--node-vm-size Standard_D4s_v3
```

* For an active-active demo, you need to create two AKS clusters (for example, in seperate Azure regions).

* If you have issues provisioning AKS clusters, check the virtual machine [quota](https://learn.microsoft.com/en-us/azure/quotas/view-quotas) for your region.

* Make sure you meet the minimum required specification (vCPUs and RAM) for Redis Enterprise when configuring your cluster.

## Install Redis Enterprise operator and set up cluster

Refer to [this documentation](https://docs.redis.com/latest/kubernetes/deployment/quick-start/) to install Redis Enterprise on Kubernetes.

For previous demonstrations, HAProxy has been used for ingress. Be sure to the follow the instructions as outlined in the [Redis ingress controller documentation](https://docs.redis.com/latest/kubernetes/re-databases/set-up-ingress-controller/).

There is a standard custom resource for a cluster in the [../aks/rec](../aks/rec) folder.  Two custom resource definitions for active-active are located in the folder [../aks/rec/active-active](../aks/rec/active-active) to configure RECs in each AKS deployment.

## Configure databases

Redis Bank utilises four databases (one active-active database which contains a stream, and individual databases for each respective microsevice - personal finance management, transactions and account management).

You may choose to expose these databases via ingress controllers, or route traffic internally and leverage the services that make these databases available inside Kubernetes.

At time of writing, you can't automate creation of active-active databases using custom resource definitions. There is an example command for use with `crdb-cli` which you can find here:

//TODO

By default, the active-active database doesn't require a password. As above, before venturing into active-active, you may choose to stand up a common Redis database that contains the Redis Stream datatype. To avoid modifying the Redis Bank UI code, set an empty password when you create this database - this can be done by creating a Kubernetes secret that you reference in your Redis Enterprise cluster custom resource definition:

`kubectl create secret generic redisbank-secret --from-literal=password=''`

## Build container images and push to Azure Container Registry

Build a Docker image using:

```
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=<acrname>.azurecr.io/redisbank-ui
```

Login to Azure Container Registry:

```
az acr login --name <acrname>
```

Push Docker image using:

```
docker push <acrname>.azurecr.io/redisbank-ui:latest
```

## Deployments, services and ingress

Once you have container images in your Azure Container Registry, it's simply a matter of updating the Kubernetes manifests to match your configuration, and deploy them via `kubectl`.

Check the deployment files for the databases in the [../aks/db](../aks/db) folder and the apps in the [../aks/apps](../aks/apps/) folder and replace the relevant placeholders (Docker hub or other ID and yourdomain and tld).

For example, to deploy the account management microservice:

```
kubectl apply -f am-app.service.yaml \
-f am-app.ingress.yaml \
-f am-app.deployment.yaml
```

When configuring the manifests, you can reference Redis connection settings using Kubernetes environment variables, or Kubernetes secrets.

Environment variables:

```
env:
- name: SPRING_REDIS_HOST
  value: $(AM_DB_SERVICE_HOST)
- name: SPRING_REDIS_HOST
  value: $(AM_DB_SERVICE_PORT)
```

Kubernetes secrets:
```
env:
- name: SPRING_REDIS_HOST
    valueFrom:
      secretKeyRef:
        name: redb-am-db
        key: service_name
- name: SPRING_REDIS_PORT
    valueFrom:
      secretKeyRef:
        name: redb-am-db
        key: port
- name: SPRING_REDIS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: redb-am-db
        key: password
```

### Questions, support, issues?
Hit that `New Issue` button, or reach out to the author directly.