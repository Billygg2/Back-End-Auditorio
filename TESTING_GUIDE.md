# 🧪 Pruebas en Thunderclient / Postman

## 📦 Archivos incluidos

1. **`THUNDER_REQUESTS.md`** - Guía completa en Markdown con todos los endpoints
2. **`thunderclient_collection.json`** - Colección exportable para Thunderclient
3. **`postman_collection.json`** - Colección exportable para Postman

---

## 🚀 Cómo importar en Thunderclient

### Opción 1: Importar el archivo JSON

1. Abre **Thunderclient** en VS Code
2. Haz clic en el ícono de **importar** (arriba a la izquierda)
3. Selecciona el archivo `thunderclient_collection.json`
4. ✅ La colección se importará automáticamente

### Opción 2: Crear manualmente

- Copia los JSONs del archivo `THUNDER_REQUESTS.md` y pégalos en Thunderclient
- O usa los ejemplos directamente en el formato indicado

---

## 📨 Cómo importar en Postman

1. Abre **Postman**
2. Haz clic en **Import** (esquina superior izquierda)
3. Selecciona el archivo `postman_collection.json`
4. ✅ La colección se importará con variables precargadas

### Variables automáticas en Postman

Los tokens se guardan automáticamente después de hacer login:
- **ADMIN_TOKEN** → Se guarda al hacer login como admin
- **USER_TOKEN** → Se guarda al hacer login como usuario

---

## ⚡ Flujo de Pruebas Recomendado

### 1️⃣ **Registrar usuarios**
```bash
POST /auth/register
{
  "username": "admin",
  "password": "admin123",
  "role": "ADMIN"
}

POST /auth/register
{
  "username": "usuario1",
  "password": "user123",
  "role": "USER"
}
```

### 2️⃣ **Obtener tokens**
```bash
POST /auth/login (admin)
POST /auth/login (usuario)
```
💾 Guarda los tokens en variables

### 3️⃣ **Crear eventos**
```bash
POST /api/eventos
Authorization: Bearer {{USER_TOKEN}}
```

### 4️⃣ **Ver mis eventos**
```bash
GET /api/eventos/mis-eventos
Authorization: Bearer {{USER_TOKEN}}
```

### 5️⃣ **Admin aprueba evento**
```bash
PUT /api/eventos/1/aprobar-rechazar
Authorization: Bearer {{ADMIN_TOKEN}}
```

### 6️⃣ **Ver calendario público**
```bash
GET /api/eventos/calendario-completo
(sin autenticación)
```

---

## 📋 Resumen de URLs

| Endpoint | Método | Auth | Rol |
|----------|--------|------|-----|
| `/auth/register` | POST | ❌ | Público |
| `/auth/login` | POST | ❌ | Público |
| `/api/eventos` | GET | ✅ | ADMIN |
| `/api/eventos` | POST | ✅ | USER/ADMIN |
| `/api/eventos/mis-eventos` | GET | ✅ | USER/ADMIN |
| `/api/eventos/pendientes` | GET | ✅ | ADMIN |
| `/api/eventos/rechazados` | GET | ✅ | ADMIN |
| `/api/eventos/aprobados` | GET | ❌ | Público |
| `/api/eventos/{id}` | GET | ✅ | USER/ADMIN |
| `/api/eventos/{id}` | PUT | ✅ | USER/ADMIN |
| `/api/eventos/{id}` | DELETE | ✅ | USER/ADMIN |
| `/api/eventos/{id}/aprobar-rechazar` | PUT | ✅ | ADMIN |
| `/api/eventos/{id}/cancelar` | PUT | ✅ | USER/ADMIN |
| `/api/eventos/disponibilidad` | GET | ✅ | USER/ADMIN |
| `/api/eventos/fecha/{fecha}` | GET | ✅ | ADMIN |
| `/api/eventos/proximos` | GET | ❌ | Público |
| `/api/eventos/calendario-completo` | GET | ❌ | Público |

---

## ⚙️ Configuración Base

**Base URL**: `http://localhost:8080`

**Headers comunes**:
```
Content-Type: application/json
Authorization: Bearer <TOKEN>
```

---

## 💡 Tips de Prueba

✅ **Primero registra y haz login** para obtener tokens  
✅ **Guarda los tokens en variables** de entorno para reutilizarlos  
✅ **Prueba sin autenticación** en los endpoints públicos  
✅ **Intenta con permisos insuficientes** para ver errores 403  
✅ **Verifica disponibilidad** antes de crear eventos en la misma fecha/hora  

---

## 🐛 Troubleshooting

| Problema | Solución |
|----------|----------|
| 401 Unauthorized | Token expirado o inválido, haz login nuevamente |
| 403 Forbidden | Tu rol no tiene permisos para esta acción |
| 404 Not Found | El ID del evento no existe |
| 400 Bad Request | Verifica el formato JSON, fechas (YYYY-MM-DD), horas (HH:mm) |
| CORS error | Asegúrate que la API está en `http://localhost:8080` |

---

## 📝 Notas Importantes

- **JWT válido por**: 1 hora (3600 segundos)
- **Formatos de fecha**: `YYYY-MM-DD` (ej: 2026-02-15)
- **Formatos de hora**: `HH:mm` (ej: 14:30)
- **Puerto por defecto**: 8080 (cambiar si es diferente)
- **Base de datos**: PostgreSQL (configurar en `application.yaml`)

---
