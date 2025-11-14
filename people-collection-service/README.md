# People Service

```bash
cp .env.example .env
```

```bash
./gradlew clean build

cp people-ejb/build/libs/people-ejb.jar $WILDFLY_HOME/standalone/deployments/
cp people-web/build/libs/people-web.war $WILDFLY_HOME/standalone/deployments/
```

```bash
export $(cat .env | xargs)
docker compose up
```

Для тестов в [`api-tests`](../api-tests) выключаем Rate Limit:

```bash
export $(cat .env | xargs)

cd $WILDFLY_HOME
./bin/standalone.sh -b 0.0.0.0 \
                    -Dapi.rate.limit.disabled=true \
                    -Djavax.net.ssl.trustStore=./standalone/configuration/truststore.jks \
                    -Djavax.net.ssl.trustStorePassword=changeit \
                    -DCONSUL_HOST="$CONSUL_HOST" -DCONSUL_PORT="$CONSUL_PORT" -DEUREKA_URL="$EUREKA_URL"
```

### Настройка динамически расширяемого EJB-пула:

Подключитесь к WildFly через CLI:

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect
```

Выполните команды:

```bash
# 1. Создать пул, размер которого масштабируется от worker-пула (HTTP threads)
/subsystem=ejb3/strict-max-bean-instance-pool=people-service-pool:add(derive-size=from-worker-pools,max-pool-size=100,timeout=5,timeout-unit=MINUTES)

# 2. Назначить его как пул по умолчанию для всех Stateless Session Beans
/subsystem=ejb3:write-attribute(name=default-slsb-instance-pool, value=people-service-pool)

# 3. Перезагрузить сервер
reload

# 4. Убедитесь, что пул настроен
/subsystem=ejb3/strict-max-bean-instance-pool=people-service-pool:read-resource
# Вывод должен содержать:
# {
#     "outcome" => "success",
#     "result" => {
#         "derive-size" => "from-worker-pools",
#         "max-pool-size" => 100,
#         "timeout" => 5L,
#         "timeout-unit" => "MINUTES"
#     }
# }
```
