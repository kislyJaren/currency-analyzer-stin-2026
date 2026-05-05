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
- ukladani nastaveni do H2 databaze,
- nacitani kurzu z externiho REST API,
- zobrazeni kurzu k vybranemu datu,
- vypocet prumernych kurzu za obdobi,
- logovani chyb do databaze,
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

## H2 console:

```text
http://localhost:8080/h2-console
```

```text
jdbc:h2:file:./data/currency-analyzer
User Name: sa
Password:
```

Na Renderu je H2 console vypnuta.

## Docker a Render


Pouzite environment variables na Renderu:

```text
EXCHANGE_RATE_API_KEY=***
H2_CONSOLE_ENABLED=false
SPRING_DATASOURCE_URL=jdbc:h2:file:/tmp/currency-analyzer
APP_USERNAME=user
APP_PASSWORD=password
```

Render nastavuje port automaticky pres `PORT`.

## Git workflow

Projekt pouziva vetve:

```text
main
develop
feature/*
```
