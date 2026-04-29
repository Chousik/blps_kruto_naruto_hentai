# Local WildFly Runbook

This runbook starts the current BLPS `core-service` on WildFly and connects it to ERPNext through a real JCA Resource Adapter.

## 1. Prerequisites

- Java 17
- WildFly installed locally
- Docker running
- PostgreSQL / Kafka / ERPNext available locally

## 2. Start local infrastructure

From the repository root:

```bash
docker compose up -d postgres zookeeper kafka
cd frappe_docker
docker compose -f pwd.yml up -d
cd ..
```

ERPNext demo UI will be available at `http://localhost:18080`.

## 3. Build deployable artifacts

From the repository root:

```bash
./gradlew -g .gradle-local clean bootWar
./gradlew -g .gradle-local -p erpnext-ra clean rar
```

Artifacts:

- `build/libs/core-service.war`
- `erpnext-ra/build/distributions/erpnext-ra-0.0.1-SNAPSHOT.rar`

## 4. Export runtime configuration for WildFly

Before starting WildFly, export the variables below in the same shell:

```bash
export WILDFLY_HOME=/Users/hipeoplea/Downloads/wildfly-39.0.1.Final
export DATABASE_URL=jdbc:postgresql://localhost:5432/blps
export DATABASE_USERNAME=blps
export DATABASE_PASSWORD=blps
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export PAYMENT_REQUESTS_TOPIC=payment-requests
export PAYMENT_URL_ASSIGNED_TOPIC=payment-url-assigned

export ERPNEXT_COMPANY=AirBNB
export ERPNEXT_SERVICE_ITEM_CODE=EXTRA_SERVICE
export ERPNEXT_JNDI_NAME=java:/eis/ErpNextCF

export BLPS_ACCOUNTS_XML=/Users/hipeoplea/IdeaProjects/blps_kruto_naruto_hentai/data/security/accounts.xml
```

Start WildFly with explicit XML accounts path:

```bash
export JAVA_OPTS="$JAVA_OPTS -Dblps.security.accounts-location=$BLPS_ACCOUNTS_XML"
$WILDFLY_HOME/bin/standalone.sh
```

Or use the prepared script:

```bash
/Users/hipeoplea/IdeaProjects/blps_kruto_naruto_hentai/scripts/wildfly/start-local.sh
```

## 5. Deploy ERP JCA adapter

In another shell, re-export only the values WildFly CLI needs:

```bash
export WILDFLY_HOME=/Users/hipeoplea/Downloads/wildfly-39.0.1.Final
export ERPNEXT_BASE_URL=http://localhost:18080
export ERPNEXT_API_KEY=replace-me
export ERPNEXT_API_SECRET=replace-me
export ERPNEXT_RA_RAR=/Users/hipeoplea/IdeaProjects/blps_kruto_naruto_hentai/erpnext-ra/build/distributions/erpnext-ra-0.0.1-SNAPSHOT.rar
```

Then run:

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect --file=/Users/hipeoplea/IdeaProjects/blps_kruto_naruto_hentai/docs/wildfly/install-erpnext-ra.cli
```

Or build and deploy both artifacts through one script:

```bash
ERPNEXT_BASE_URL=http://localhost:18080 \
ERPNEXT_API_KEY=replace-me \
ERPNEXT_API_SECRET=replace-me \
/Users/hipeoplea/IdeaProjects/blps_kruto_naruto_hentai/scripts/wildfly/deploy-local.sh
```

What this does:

1. Deploys `erpnext-ra.rar`
2. Registers the resource adapter in WildFly
3. Publishes `ConnectionFactory` as `java:/eis/ErpNextCF`

## 6. Deploy core-service WAR

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect --commands="deploy /Users/hipeoplea/IdeaProjects/blps_kruto_naruto_hentai/build/libs/core-service.war --force"
```

## 7. Smoke-check deployment

Open:

- `http://localhost:8080/core-service/rest-endpoints-smoke.html`
- `http://localhost:8080/core-service/swagger-ui/index.html`

Recommended smoke path:

1. Login as host
2. Create chat
3. Create extra service
4. Verify `Quotation` appears in ERPNext
5. Login as guest
6. Accept extra service
7. Verify `Sales Order` appears in ERPNext
8. Start `payment-service` separately and verify `Sales Invoice` appears after payment URL assignment

## 8. Start payment-service

```bash
cd payment-service
DATABASE_URL=jdbc:postgresql://localhost:5432/blps \
DATABASE_USERNAME=blps \
DATABASE_PASSWORD=blps \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
PAYMENT_REQUESTS_TOPIC=payment-requests \
PAYMENT_URL_ASSIGNED_TOPIC=payment-url-assigned \
./gradlew -g ../.gradle-local bootRun
```

## 9. Quick verification from WildFly CLI

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect --commands="/subsystem=resource-adapters/resource-adapter=erpnext-ra.rar:read-resource(recursive=true)"
$WILDFLY_HOME/bin/jboss-cli.sh --connect --commands="/deployment=core-service.war:read-resource"
```
