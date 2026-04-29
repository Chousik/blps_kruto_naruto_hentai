# Core-Service Migration to WildFly + JCA ERP Adapter

## 1. Build and prepare ERPNext Resource Adapter (.rar)

1. Build adapter JAR:
   - `cd erpnext-ra`
   - `gradlew.bat clean jar`
2. Package `.rar` manually:
   - create archive `erpnext-ra.rar`
   - include:
     - `META-INF/ra.xml`
     - `erpnext-ra-0.0.1-SNAPSHOT.jar`
3. Deploy `.rar` to WildFly:
   - copy to `WILDFLY_HOME/standalone/deployments/`

## 2. Register RA connection in WildFly

Add resource-adapter config in `standalone.xml` (or via CLI):

- `archive=erpnext-ra.rar`
- transaction support: `NoTransaction`
- config properties:
  - `BaseUrl`
  - `ApiKey`
  - `ApiSecret`
- JNDI name for CF, for example:
  - `java:/eis/ErpNextCF`

## 3. Convert core-service from Boot executable JAR to WAR

1. In `build.gradle.kts`:
   - apply `war` plugin
   - keep Spring MVC/Security/Data JPA dependencies
   - mark embedded Tomcat as `providedRuntime` (or remove embedded container starter)
2. Main app class:
   - extend `SpringBootServletInitializer`
3. Build:
   - `gradlew.bat clean war`
4. Deploy resulting WAR into WildFly:
   - `WILDFLY_HOME/standalone/deployments/core-service.war`

## 4. Externalize infrastructure from app YAML to WildFly

Move these to WildFly/JNDI/env:

1. Datasource (`java:/jdbc/blps`)
2. Kafka bootstrap and consumer/producer props
3. ERP JCA CF JNDI name (`java:/eis/ErpNextCF`)

In app, read them from env/JNDI instead of hardcoded local defaults.

## 5. Wire core-service to JNDI ConnectionFactory

1. Replace direct Spring bean factory for ERP CF with JNDI lookup bean:
   - lookup `java:/eis/ErpNextCF`
2. `ErpNextSyncService` continues to use `ConnectionFactory.getConnection()`.

## 6. Transaction model

Current RA is `NoTransaction`.

Implications:

1. ERP calls are out-of-transaction from DB perspective.
2. Keep after-commit strategy for ERP sync (already used), so DB commit happens first.

## 7. Smoke checklist on WildFly

1. Deploy `.rar` and `.war` successfully.
2. Create extra service:
   - Quotation created in ERP.
3. ACCEPT:
   - Sales Order created.
4. Payment URL assigned event:
   - Sales Invoice created.

## 8. Important note about current RA state

Current `erpnext-ra` is a baseline SPI scaffold for WildFly integration.
Before production use, complete:

1. HTTP execution operations in `ErpNextConnectionImpl` for ERP endpoints.
2. Robust exception mapping and retry policy.
3. Pooling, validation, and lifecycle hardening.
4. Security: move secrets to Elytron credential store.
