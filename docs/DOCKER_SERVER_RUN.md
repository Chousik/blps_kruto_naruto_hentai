# Server Docker Run

## Public ports

- `8433/tcp` -> `core-service` on WildFly
- `8434/tcp` -> `payment-worker-service` webhook/API

If you terminate TLS or route traffic through Nginx/Caddy, publish `80/443` on the proxy and forward:

- `https://your-domain/...` -> `http://127.0.0.1:8433/...`
- `https://your-domain/payments/...` -> `http://127.0.0.1:8434/payments/...`

## Private-only ports

- `9990/tcp` -> WildFly management, bound to `127.0.0.1`
- PostgreSQL, Kafka, Zookeeper are not published outside Docker

## First start

1. Copy `.env.server.example` to `.env` and fill in real secrets.
2. Ensure the XML accounts directory is writable by container uid `1000`.

```bash
mkdir -p data/security
chmod 775 data/security
```

If you already have an `accounts.xml`, keep it in `data/security/accounts.xml`.

3. Build and start:

```bash
docker compose --env-file .env up -d --build
```

4. Open the firewall only for the ports you really want to expose:

```bash
sudo ufw allow 8433/tcp
sudo ufw allow 8434/tcp
```

If `payment-worker-service` sits only behind your reverse proxy, do not open `8434` publicly.

## Checks

```bash
docker compose ps
docker compose logs -f core-service
docker compose logs -f payment-worker-service
```

Core app:

- `http://your-server:8433/core-service/swagger-ui/index.html`
- `http://your-server:8433/core-service/rest-endpoints-smoke.html`

## Restart

```bash
docker compose --env-file .env up -d --build
```

## Stop

```bash
docker compose down
```

## Notes

- WildFly inside the container listens on `8433`, not `8080`.
- Registrations are stored in `data/security/accounts.xml`, so keep that directory persistent.
- If YooKassa webhook does not need direct public access, put `payment-worker-service` behind your reverse proxy instead of publishing `8434`.
- WildFly management is published only as `127.0.0.1:9990`, so use SSH if you need remote access:

```bash
ssh -L 9990:127.0.0.1:9990 user@your-server
```
