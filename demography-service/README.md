# Demography Service

## Окружение

```bash
cd people-collection-service
cp .env.example .env
export $(cat .env | xargs)

cd ..
docker compose up
```

## Запуск

> [!WARNING]
> `SERVICE_HOST` должен быть адресом, по которому `Consul` (в контейнере) может достучаться до `demography-service` (на хосте). Для Linux — `172.17.0.1`, для macOS/Windows — `host.docker.internal`.

```bash
cd demography-service
cp .env.example .env
export $(cat .env | xargs)

./gradlew clean build

SERVICE_PORT=8081 SERVICE_HOST=172.17.0.1 ./gradlew bootRun # Инстанс 1
SERVICE_PORT=8082 SERVICE_HOST=172.17.0.1 ./gradlew bootRun # Инстанс 2
```
