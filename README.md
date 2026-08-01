# Spotify Playlist API

API REST para gestión de playlists musicales con integración de Spotify para enriquecimiento automático de géneros.

## Stack

| Componente | Tecnología |
|---|---|
| Framework | Spring Boot 3.3.2 |
| Java | 17 LTS |
| Base de datos | H2 (en memoria) |
| Seguridad | JWT stateless + CSRF (Double Submit Cookie) |
| Build | Maven |

## Requisitos

- JDK 17+
- Maven 3.8+
- Cuenta de Spotify (gratuita o premium)

## Obtener credenciales de Spotify

1. Ve a [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Inicia sesión con tu cuenta de Spotify
3. Haz clic en **Create app**
4. Llena nombre y descripción (ej: "Playlist API")
5. Copia el **Client ID** y **Client Secret**
6. Ve a **Settings** > **Redirect URIs** y añade `http://localhost:8081` (no se usa OAuth redirect pero es requerido por Spotify)

## Variables de entorno

Copia `.env.example` a `.env` y completa los valores:

```properties
SPOTIFY_CLIENT_ID=tu_client_id
SPOTIFY_CLIENT_SECRET=tu_client_secret
JWT_SECRET=unaClaveSecretaDeAlMenos32BytesDeLongitud
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
SERVER_PORT=8081
CORS_ORIGINS=http://localhost:5173
```

El archivo `.env` **nunca** se commitea (está en `.gitignore`).

### En IntelliJ IDEA

Ve a Run > Edit Configurations > Environment Variables y pega las variables.

### En terminal (Windows PowerShell)

```powershell
$env:SPOTIFY_CLIENT_ID="tu_client_id"
$env:SPOTIFY_CLIENT_SECRET="tu_client_secret"
$env:JWT_SECRET="unaClaveSecretaDeAlMenos32BytesDeLongitud"
$env:ADMIN_PASSWORD="admin123"
```

### En terminal (Linux/Mac)

```bash
export SPOTIFY_CLIENT_ID=tu_client_id
export SPOTIFY_CLIENT_SECRET=tu_client_secret
export JWT_SECRET=unaClaveSecretaDeAlMenos32BytesDeLongitud
export ADMIN_PASSWORD=admin123
```

## Ejecutar

```bash
mvn spring-boot:run
```

La API arranca en `http://localhost:8081`.

Consola H2: `http://localhost:8081/h2-console` (requiere autenticación).

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/auth/login` | No | Login. Retorna JWT en body + cookie `access_token` |
| `GET` | `/auth/me` | Sí | Usuario autenticado actual |
| `POST` | `/auth/logout` | Sí | Elimina cookie de sesión |
| `POST` | `/lists` | Sí | Crear playlist |
| `GET` | `/lists` | Sí | Listar todas las playlists |
| `GET` | `/lists/{name}` | Sí | Obtener playlist por nombre |
| `DELETE` | `/lists/{name}` | Sí | Eliminar playlist |

### Ejemplo de login

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Respuesta:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

### Ejemplo de crear playlist

```bash
curl -X POST http://localhost:8081/lists \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzM4NCJ9..." \
  -d '{
    "nombre": "Rock Classics",
    "descripcion": "Lo mejor del rock",
    "canciones": [
      {
        "titulo": "Bohemian Rhapsody",
        "artista": "Queen",
        "album": "A Night at the Opera",
        "anno": "1975",
        "genero": ""
      }
    ]
  }'
```

Si `genero` viene vacío o nulo, la API consulta automáticamente a Spotify para obtener el género del artista.

## Credenciales por defecto

| Campo | Valor |
|---|---|
| Usuario | `admin` (configurable con `ADMIN_USERNAME`) |
| Contraseña | Debe definirse en `ADMIN_PASSWORD` (sin default, falla al iniciar si no se configura) |

## Seguridad

- JWT firmado con HMAC-SHA384 (mínimo 32 bytes de clave)
- Token en header `Authorization: Bearer <token>` y cookie `httpOnly`
- CSRF mediante Double Submit Cookie (`XSRF-TOKEN` + `X-CSRF-Token`)
- CORS configurable vía `CORS_ORIGINS`
- Cabeceras HSTS, X-Content-Type-Options, X-XSS-Protection
