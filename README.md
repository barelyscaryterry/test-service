# Test Service

Candidate service under test — a Spring Boot REST CRUD API for beers, backed by DynamoDB.
Receives replayed traffic from `capture-proxy` / `replay-engine`.

## API

| Method | Path             | Description                  |
|--------|------------------|-------------------------------|
| POST   | `/api/beers`     | Create a beer                 |
| GET    | `/api/beers`     | List all beers                |
| GET    | `/api/beers/{id}`| Get a beer by id              |
| PATCH  | `/api/beers/{id}`| Partially update a beer       |

### Beer shape

```json
{
  "id": "generated-uuid",
  "name": "Pliny the Elder",
  "brewery": "Russian River",
  "style": "Double IPA",
  "abv": 8.0,
  "createdAt": "2026-08-12T00:00:00Z",
  "updatedAt": "2026-08-12T00:00:00Z"
}
```

`POST` body: `name`, `brewery`, `style`, `abv` (all required).
`PATCH` body: any subset of `name`, `brewery`, `style`, `abv` — only provided fields are updated.

## Running locally

1. Start local DynamoDB:

   ```bash
   docker compose up -d
   ```

2. Create the `beers` table (one-time, table lives in the in-memory local DynamoDB so this needs
   to be redone whenever the container restarts):

   ```bash
   aws dynamodb create-table \
     --table-name beers \
     --attribute-definitions AttributeName=id,AttributeType=S \
     --key-schema AttributeName=id,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST \
     --endpoint-url http://localhost:8000 \
     --region us-east-1
   ```

3. Run the app with the `local` profile, which points the DynamoDB client at
   `http://localhost:8000` instead of real AWS:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

The app starts on `http://localhost:8080`.

### Try it

```bash
curl -X POST http://localhost:8080/api/beers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Pliny the Elder","brewery":"Russian River","style":"Double IPA","abv":8.0}'

curl http://localhost:8080/api/beers

curl -X PATCH http://localhost:8080/api/beers/<id> \
  -H 'Content-Type: application/json' \
  -d '{"abv":8.2}'
```

## Deploying against real AWS

Without the `local` profile, the app makes no endpoint override and the AWS SDK talks to real
DynamoDB, authenticating via the normal credential chain (IAM instance profile once deployed,
same pattern as `capture-proxy`). Set `AWS_REGION` if not `us-east-1`, and ensure a `beers` table
exists with a string partition key `id`.

## Building a jar

```bash
mvn clean package
java -jar target/test-service-1.0.0-SNAPSHOT.jar
```
