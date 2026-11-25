# Guía de Despliegue Local en Kubernetes (Kind) y Prueba de Recuperación (RTO)

Esta guía detalla paso a paso cómo levantar el Sistema Hotelero completo en un clúster local de **Kind** respetando la separación FinOps (`on-demand` vs. `spot`), validar el escalamiento horizontal de pods y ejecutar backups/restauración en S3 (LocalStack).

---

## 1. Levantar el Clúster Kind con Etiquetas y Taints FinOps

```bash
kind create cluster --name hotel-cluster --config infra/k8s/kind-config.yaml
```

Verifica que los 3 nodos estén `Ready` y con sus etiquetas/taints aplicados:

```bash
kubectl get nodes --show-labels
kubectl describe nodes | grep -E "Name:|Taints:"
```

---

## 2. Construir y Cargar las Imágenes Locales en Kind

Ejecuta desde la raíz del monorepositorio:

```bash
docker build -t ms-reservas:latest -f infra/docker/backend.Dockerfile .
docker build -t hotel-frontend:latest -f infra/docker/frontend.Dockerfile .

kind load docker-image ms-reservas:latest hotel-frontend:latest --name hotel-cluster
```

---

## 3. Aplicar los Manifiestos de Kubernetes

El archivo `config.yaml` crea el namespace `hotel-system`, el `ConfigMap` y el `Secret`. Para desplegar toda la arquitectura (excluyendo `kind-config.yaml`, que es solo para la creación del clúster Kind):

```bash
kubectl apply -f infra/k8s/config.yaml
kubectl apply -f infra/k8s/data-infra.yaml
kubectl apply -f infra/k8s/api-deployment.yaml -f infra/k8s/worker-deployment.yaml -f infra/k8s/frontend-deployment.yaml
kubectl apply -f infra/k8s/backup-cronjob.yaml
```

*(Opcional si tienes KEDA v2.16+ instalado en el clúster: `kubectl apply -f infra/k8s/autoscaling.yaml`).*

Verifica el despliegue y la distribución de los pods (`3` API + `2` Frontend en nodo `on-demand`, `3` Workers en nodo `spot`):

```bash
kubectl get pods -n hotel-system -o wide
```

---

## 4. Acceso Local y Prueba Funcional

Abre un túnel hacia el servicio del frontend (y opcionalmente hacia la API):

```bash
kubectl port-forward -n hotel-system svc/frontend 3000:80
```

En otra terminal, puedes probar la creación de reservas y el procesamiento asíncrono vía SQS:

```bash
kubectl port-forward -n hotel-system svc/ms-reservas-api 8080:8080
```

---

## 5. Prueba Manual de Backup hacia S3 (`CronJob`)

Dispara una ejecución manual inmediata del `CronJob` `postgres-s3-backup`:

```bash
kubectl create job --from=cronjob/postgres-s3-backup test-backup -n hotel-system
```

Monitorea el Job hasta que pase a estado `Completed`:

```bash
kubectl get jobs -n hotel-system -w
kubectl logs -n hotel-system job/test-backup -c pg-dump
kubectl logs -n hotel-system job/test-backup -c s3-upload
```

Verifica que el archivo `.sql.gz` exista en el bucket `s3://hotel-backups/` dentro de LocalStack:

```bash
kubectl exec -n hotel-system deploy/localstack -- awslocal s3 ls s3://hotel-backups/
```

---

## 6. Prueba de Recuperación / Restauración (RTO)

1. Identifica el nombre exacto del último backup en S3:

```bash
BACKUP_FILE=$(kubectl exec -n hotel-system deploy/localstack -- awslocal s3 ls s3://hotel-backups/ | sort | tail -n 1 | awk '{print $4}')
echo "Restaurando: $BACKUP_FILE"
```

2. Descarga el archivo desde LocalStack y restáuralo directamente sobre PostgreSQL:

```bash
kubectl exec -n hotel-system deploy/localstack -- awslocal s3 cp "s3://hotel-backups/${BACKUP_FILE}" - \
  | kubectl exec -i -n hotel-system deploy/postgres -- sh -c 'gunzip | psql -U hotel_admin -d hotel_reservas_db'
```

3. Verifica la integridad de los registros restaurados:

```bash
kubectl exec -n hotel-system deploy/postgres -- psql -U hotel_admin -d hotel_reservas_db -c "SELECT id, room_id, status, created_at FROM reservations ORDER BY id DESC LIMIT 5;"
```
