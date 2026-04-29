FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
COPY data ./data
COPY erpnext-ra ./erpnext-ra

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon clean war
RUN ./gradlew --no-daemon -p erpnext-ra clean rar

FROM eclipse-temurin:17-jre-jammy

ARG WILDFLY_VERSION=39.0.1.Final
ENV JBOSS_HOME=/opt/jboss/wildfly

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && mkdir -p /opt/jboss \
    && curl -fsSL -o /tmp/wildfly.tar.gz "https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.tar.gz" \
    && tar -xzf /tmp/wildfly.tar.gz -C /opt/jboss \
    && mv "/opt/jboss/wildfly-${WILDFLY_VERSION}" "$JBOSS_HOME" \
    && rm -f /tmp/wildfly.tar.gz \
    && apt-get purge -y --auto-remove curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 1000 --gid 0 --home-dir "$JBOSS_HOME" --shell /usr/sbin/nologin jboss \
    && mkdir -p "$JBOSS_HOME/data/blps" \
    && chown -R jboss:root /opt/jboss

COPY --from=builder /workspace/build/libs/core-service.war /opt/jboss/wildfly/standalone/deployments/core-service.war
COPY --from=builder /workspace/erpnext-ra/build/distributions/erpnext-ra-0.0.1-SNAPSHOT.rar /opt/jboss/wildfly/standalone/deployments/erpnext-ra.rar
COPY --from=builder /workspace/data/security/accounts.xml /opt/jboss/wildfly/data/blps/accounts.xml

USER jboss

EXPOSE 8433

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-Djboss.http.port=8433", "-Dblps.security.accounts-location=/opt/jboss/wildfly/data/blps/accounts.xml"]
