# Currency Analyzer

Currency Analyzer je webova aplikace pro analyzu menovych kurzu. Umoznuje nastavit zakladni menu, vybrane meny, zobrazit kurzy k vybranemu datu a vypocitat prumerne kurzy za zvolene obdobi.

## Nasazena aplikace

Aplikace je nasazena mimo localhost na platforme Render:

https://currency-analyzer-stin-2026.onrender.com/

## Prihlaseni

Vychozi udaje:

```text
user / password
```

## Funkce

- prihlaseni pres Spring Security,
- nastaveni zakladni a sledovanych men,
- ukladani nastaveni do PostgreSQL databaze,
- nacitani kurzu z externiho REST API,
- zobrazeni kurzu k vybranemu datu,
- vypocet prumernych kurzu za obdobi,
- logovani udalosti a chyb do databaze,
- automaticke testy, JaCoCo coverage a GitHub Actions,
- nasazeni mimo localhost.

## Struktura projektu

```text
config      - konfigurace aplikace
controller  - webove controllery
client      - komunikace s externim API
dto         - datove objekty
entity      - JPA entity
exception   - zpracovani chyb
repository  - databazove repositories
service     - business logika
```

```text
http://localhost:8080
```

## Databaze

Aplikace pouziva PostgreSQL databazi.

Lokalni PostgreSQL lze spustit pomoci Docker Compose:

```bash
docker compose up -d
```

Vychozi lokalni pripojeni:

```text
Database: currency_analyzer
User: currency_user
Password: currency_password
Host: localhost
Port: 5432
```

Kontrola databaze pres Docker:

```bash
docker exec -it currency-analyzer-postgres psql -U currency_user -d currency_analyzer
```

Kontrola databaze ve Windows, pokud je PostgreSQL nainstalovany lokalne:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -p 5432 -U currency_user -d currency_analyzer
```

Uzitecne SQL prikazy:

```sql
\dt

SELECT * FROM user_settings;

SELECT id, created_at, level, source, message, detail
FROM application_logs
ORDER BY id DESC
LIMIT 10;
```

## Docker a Render

Pouzite environment variables na Renderu:

```text
EXCHANGE_RATE_API_KEY=***
SPRING_DATASOURCE_URL=jdbc:postgresql://.../currency_analyzer?sslmode=require
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_DATASOURCE_USERNAME=***
SPRING_DATASOURCE_PASSWORD=***
APP_USERNAME=user
APP_PASSWORD=password
```

Kontrola databaze na Renderu z Windows PowerShell:

```powershell
$env:PGPASSWORD="heslo_z_Render_PostgreSQL"
$env:PGSSLMODE="require"

& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h dpg-d8090enaqgkc739u34q0-a.frankfurt-postgres.render.com -p 5432 -U currency_app_user -d currency_analyzer
```

Uzitecne SQL prikazy pro kontrolu Render databaze:

```sql
\dt

SELECT * FROM user_settings;

SELECT id, created_at, level, source, message, detail
FROM application_logs
ORDER BY id DESC
LIMIT 10;
```

Render nastavuje port automaticky pres `PORT`.

## Git workflow

Projekt pouziva vetve:

```text
main
develop
feature/*
```
