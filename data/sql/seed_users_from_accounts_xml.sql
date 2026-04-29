-- Seed users table from data/security/accounts.xml accounts.
-- Safe to run multiple times due to ON CONFLICT guards.

INSERT INTO users (
    id,
    username,
    email,
    first_name,
    last_name,
    role,
    created_at,
    updated_at
)
VALUES
    ('11111111-1111-4111-8111-111111111111', 'guest-alex', 'guest.alex@example.com', 'Alex', 'Guest', 'GUEST', NOW(), NOW()),
    ('22222222-2222-4222-8222-222222222222', 'guest-vika', 'guest.vika@example.com', 'Vika', 'Guest', 'GUEST', NOW(), NOW()),
    ('33333333-3333-4333-8333-333333333333', 'host-dmitry', 'host.dmitry@example.com', 'Dmitry', 'Host', 'HOST', NOW(), NOW()),
    ('44444444-4444-4444-8444-444444444444', 'host-elena', 'host.elena@example.com', 'Elena', 'Host', 'HOST', NOW(), NOW()),
    ('55555555-5555-4555-8555-555555555555', 'ops-olga', 'ops.olga@example.com', 'Olga', 'Ops', 'ADMIN', NOW(), NOW()),
    ('8b9aeae2-81e2-4001-b916-4876596c973f', 'smoke_guest_20260428', 'smoke.guest.20260428@example.com', 'Smoke', 'Guest', 'GUEST', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role,
    updated_at = NOW();
