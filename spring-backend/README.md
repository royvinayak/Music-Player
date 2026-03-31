# Playlisto Spring Boot

## prerequisites

- Java 17+ installed
- Gradle installed in PATH, or use Gradle wrapper (`gradlew`)

## install wrapper manually

If `gradlew` is not available, run (requires internet):

```powershell
cd c:\Users\risha\Downloads\playlisto-0.1\playlisto-0.1\spring-backend
gradle wrapper --gradle-version 8.3.3
```

## run

```powershell
cd c:\Users\risha\Downloads\playlisto-0.1\playlisto-0.1\spring-backend
./gradlew bootRun
```

## API endpoints

- `POST /api/load-folder`
- `GET /api/stream/{fileId}?filename={name}`
- `GET /api/sources`
- `POST /api/sources`
- `POST /api/sources/activate/{sourceId}`
- `DELETE /api/sources/{sourceId}`
- `GET /api/providers`
- `POST /api/providers`
- `POST /api/providers/activate/{providerId}`

## frontend proxy

In `frontend/vite.config.ts`, set:

```ts
server: {
  proxy: {
    '/api': 'http://localhost:8080',
  },
}
```

Then run frontend normally:

```bash
cd c:\Users\risha\Downloads\playlisto-0.1\playlisto-0.1\frontend
npm install
npm run dev
```
