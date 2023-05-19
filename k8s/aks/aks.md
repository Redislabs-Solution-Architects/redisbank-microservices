# Deploying to Azure Kubernetes Service

This version of Redis Bank is excellent for demonstrating the active-active capabilities of Redis Enterprise on managed Kubernetes clusters.

For example, you could deploy the microservices and UI in 'UK South' Azure region, and the data generation service in 'UK West' - Redis Enterprise will handle the data replication between the regions. 

The instructions below detail the steps for getting Redis Enterprise configured on Azure Kubernetes Service (AKS) alongside the Redis Bank microservices and UI.

## Prerequisites

1. [An Azure subscription](https://azure.microsoft.com/en-gb/pricing/purchase-options/pay-as-you-go/)
1. [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
1. [Docker](https://docs.docker.com/get-docker/)
1. A domain name and/or DNS zone that you can configure.

## Before you begin

If you are not yet a Redis active-active aficionado, you may find it simpler to get started by deploying a standard Redis Enterprise cluster (and databases) on AKS within a single region. Once everything is working end to end, you can deploy a second AKS cluster and configure an active-active database. Finally, deploy the data generation application to a region separate from the UI and microservices (e.g. 'UK West').

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
Connect to the AKS cluster:
```
az aks get-credentials --resource-group <resource-group-name> --name <cluster-name>
```

* For an active-active demo, you need to create two AKS clusters (for example, in separate Azure regions).

* If you have issues provisioning AKS clusters, check the virtual machine [quota](https://learn.microsoft.com/en-us/azure/quotas/view-quotas) for your region.

* Make sure you meet the minimum required specification (vCPUs and RAM) for Redis Enterprise when configuring your cluster.

## Install Redis Enterprise operator and set up cluster

Refer to [this documentation](https://docs.redis.com/latest/kubernetes/deployment/quick-start/) to install Redis Enterprise on Kubernetes.

For previous demonstrations, HAProxy has been used for ingress. Be sure to the follow the instructions as outlined in the [Redis ingress controller documentation](https://docs.redis.com/latest/kubernetes/re-databases/set-up-ingress-controller/).

There is a standard custom resource for a cluster in the [../aks/rec](../aks/rec) folder.  Two custom resource definitions for active-active are located in the folder [../aks/rec/active-active](../aks/rec/active-active) to configure RECs in each AKS deployment.

## Configure databases

Redis Bank utilizes four databases (one active-active database which contains a stream, and individual databases for each respective microservice - personal finance management, transactions and account management).

You may choose to expose these databases via ingress controllers, or route traffic internally and leverage the services that make these databases available inside Kubernetes.

Here is an example of using `crdb-cli` to create an active-active database. In this following example the following values are being used in the placeholders described in the [documentation](https://docs.redis.com/latest/kubernetes/active-active/create-aa-database/):

* Kubernetes namespace: ns-uksouth and ns-ukwest
* Cluster names: rec-uksouth and rec-ukwest
* Wildcard DNS records: *.uksouth.aks.demo.redislabs.com and *.ukwest.aks.demo.redislabs.com

```
crdb-cli crdb create \
  --name eventbus \
  --memory-size 200mb \
  --encryption yes \
  --instance fqdn=rec-uksouth.ns-uksouth.svc.cluster.local,url=https://api.uksouth.aks.demo.redislabs.com,username=demo@redislabs.com,password=<password>,replication_endpoint=eventbus-cluster.uksouth.aks.demo.redislabs.com:443,replication_tls_sni=eventbus-cluster.uksouth.aks.demo.redislabs.com \
  --instance fqdn=rec-ukwest.ns-ukwest.svc.cluster.local,url=https://api.ukwest.aks.demo.redislabs.com,username=demo@redislabs.com,password=<password>,replication_endpoint=eventbus-cluster.ukwest.aks.demo.redislabs.com:443,replication_tls_sni=eventbus-cluster.ukwest.aks.demo.redislabs.com

```
> **_NOTE:_**  Once the database has been created, you will need to log in to each cluster and 'enable TLS for all communications'.

> **_NOTE:_** The active-active database command above isn't using the password flag. If testing the deployments using the [standard database example](../aks/db/eventbus-db.yaml), you can set an empty password by creating a Kubernetes secret before you create the database: `kubectl create secret generic redb-eventbus --from-literal=password=''`

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

Check the deployment files for the databases in the [../aks/db](../aks/db) folder and the apps in the [../aks/apps](../aks/apps/) folder and replace the relevant placeholders.

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