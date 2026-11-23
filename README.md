# NUBEPLAY – Backend (Spring Boot)

API REST de la tienda online **NUBEPLAY** (DSY1104 – Evaluación Parcial 3).
Spring Boot 3 · Spring Data JPA · MySQL · Spring Security + JWT · Swagger (springdoc).

Frontend: https://github.com/01100010-01101101/Web-NubePlay

## Requisitos

- Java 17 o superior
- Maven 3.9+ (o el que trae IntelliJ / VS Code)
- MySQL 8 (XAMPP, MySQL Workbench o Docker). Opcional: perfil `h2` sin MySQL.

## Ejecutar

```bash
# 1) Con MySQL (usuario root sin clave por defecto; la BD "nubeplay" se crea sola)
mvn spring-boot:run

#    Con otra clave:
#    Windows PowerShell:  $env:DB_PASSWORD="miClave"; mvn spring-boot:run
#    Linux / macOS:       DB_PASSWORD=miClave mvn spring-boot:run

# 2) Sin MySQL (base de datos en memoria H2)
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
```

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Al iniciar por primera vez se cargan 3 categorías, 34 productos y un usuario por rol:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@duoc.cl | admin |
| Vendedor | vendedor@duoc.cl | vendedor |
| Cliente | cliente@gmail.com | cliente |

## Pruebas de integración

```bash
mvn test
```

Levantan la aplicación completa contra H2 en memoria y prueban con MockMvc: login, registro,
renovación de tokens, tokens alterados, permisos por rol, CRUD de productos y categorías,
validaciones, y el flujo de compra (stock, cupón, anulación).

## Estructura

```
src/main/java/cl/nubeplay/api
├── config/        OpenAPI (Swagger) y carga de datos iniciales
├── controller/    Controladores REST /api/v1/...
├── dto/           Datos de entrada/salida (records con validaciones)
├── exception/     ApiException y manejador global de errores (JSON)
├── model/         Entidades JPA: Usuario, Categoria, Producto, Boleta, DetalleBoleta, Rol
├── repository/    Repositorios Spring Data JPA
├── security/      JWT (generación/validación), filtro, reglas por rol, CORS
└── service/       Lógica de negocio
```

## Endpoints (resumen)

| Método | Ruta | Acceso |
|---|---|---|
| POST | /api/v1/auth/login | Público |
| POST | /api/v1/auth/registro | Público (crea Cliente) |
| POST | /api/v1/auth/refresh | Público (requiere refreshToken) |
| GET | /api/v1/auth/me | Autenticado |
| GET | /api/v1/categorias, /api/v1/categorias/{id} | Público |
| POST, PUT, DELETE | /api/v1/categorias[/{id}] | Administrador |
| GET | /api/v1/productos[?categoria=&criticos=], /api/v1/productos/{id} | Público |
| POST, PUT, DELETE | /api/v1/productos[/{id}] | Administrador |
| GET, PUT | /api/v1/usuarios/me | Autenticado |
| GET, POST, PUT, DELETE | /api/v1/usuarios[/{id}] | Administrador |
| POST | /api/v1/boletas | Autenticado |
| GET | /api/v1/boletas | Administrador, Vendedor |
| GET | /api/v1/boletas/mis-boletas | Autenticado |
| GET | /api/v1/boletas/{id} | Admin, Vendedor o dueño de la boleta |
| PUT | /api/v1/boletas/{id}/estado | Administrador |
| DELETE | /api/v1/boletas/{id} | Administrador |

## Variables de entorno

| Variable | Por defecto |
|---|---|
| DB_URL | jdbc:mysql://localhost:3306/nubeplay?createDatabaseIfNotExist=true... |
| DB_USER / DB_PASSWORD | root / (vacía) |
| JWT_SECRET | clave de desarrollo (cámbiala en producción, Base64 ≥ 256 bits) |
| CORS_ORIGINS | http://localhost:5173, GitHub Pages del frontend |
| PORT | 8080 |
