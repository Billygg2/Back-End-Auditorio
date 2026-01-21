# Colección de Pruebas - Thunderclient

**Base URL**: `http://localhost:8080`

---

## 🔐 AUTENTICACIÓN

### 1. Registrar nuevo usuario (ADMIN)

**Método**: `POST`  
**URL**: `http://localhost:8080/auth/register`  
**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "username": "admin",
  "password": "admin123",
  "role": "ADMIN"
}
```

**Response esperado** (201):
```json
"Usuario registrado correctamente"
```

---

### 2. Registrar nuevo usuario (USER)

**Método**: `POST`  
**URL**: `http://localhost:8080/auth/register`  
**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "username": "usuario1",
  "password": "user123",
  "role": "USER"
}
```

**Response esperado** (201):
```json
"Usuario registrado correctamente"
```

---

### 3. Login - Obtener JWT Token

**Método**: `POST`  
**URL**: `http://localhost:8080/auth/login`  
**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response esperado** (200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJST0xFX0FETUluIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNzEwODI0MzIxLCJleHAiOjE3MTA4Mjc5MjF9.xxxxx"
}
```

**⚠️ Guardar este token para usar en las siguientes peticiones**

---

### 4. Login - Usuario normal (USER)

**Método**: `POST`  
**URL**: `http://localhost:8080/auth/login`  
**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "username": "usuario1",
  "password": "user123"
}
```

**Response esperado** (200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.xxxxx"
}
```

---

## 📅 EVENTOS (Requiere JWT Token)

Para todas las siguientes peticiones, añade este header:
```
Authorization: Bearer <TOKEN_OBTENIDO_DEL_LOGIN>
```

---

### 5. Crear evento (USER o ADMIN)

**Método**: `POST`  
**URL**: `http://localhost:8080/api/eventos`  
**Headers**:
```
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "nombreEvento": "Conferencia de Tecnología",
  "descripcion": "Conferencia sobre innovación en software",
  "fechaEvento": "2026-02-15",
  "horaInicio": "09:00",
  "horaFin": "12:00",
  "numeroAsistentes": 150,
  "publicoExterno": true,
  "requiereRegistroPrevio": true,
  "tipoDisposicion": "Auditorio"
}
```

**Response esperado** (201):
```json
{
  "id": 1,
  "nombreEvento": "Conferencia de Tecnología",
  "descripcion": "Conferencia sobre innovación en software",
  "fechaEvento": "2026-02-15",
  "horaInicio": "09:00",
  "horaFin": "12:00",
  "numeroAsistentes": 150,
  "publicoExterno": true,
  "requiereRegistroPrevio": true,
  "tipoDisposicion": "Auditorio",
  "estado": "PENDIENTE",
  "motivoRechazo": null
}
```

---

### 6. Listar mis eventos (USER o ADMIN)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/mis-eventos`  
**Headers**:
```
Authorization: Bearer <TOKEN>
```

**Response esperado** (200):
```json
[
  {
    "id": 1,
    "nombreEvento": "Conferencia de Tecnología",
    "estado": "PENDIENTE",
    "fechaEvento": "2026-02-15",
    "horaInicio": "09:00",
    "horaFin": "12:00"
  }
]
```

---

### 7. Obtener evento por ID (USER o ADMIN)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/1`  
**Headers**:
```
Authorization: Bearer <TOKEN>
```

**Response esperado** (200):
```json
{
  "id": 1,
  "nombreEvento": "Conferencia de Tecnología",
  "descripcion": "Conferencia sobre innovación en software",
  "fechaEvento": "2026-02-15",
  "horaInicio": "09:00",
  "horaFin": "12:00",
  "numeroAsistentes": 150,
  "estado": "PENDIENTE"
}
```

---

### 8. Listar TODOS los eventos (ADMIN solo)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos`  
**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
```

**Response esperado** (200):
```json
[
  {
    "id": 1,
    "nombreEvento": "Conferencia de Tecnología",
    "estado": "PENDIENTE",
    "fechaEvento": "2026-02-15"
  },
  {
    "id": 2,
    "nombreEvento": "Seminario de Gestión",
    "estado": "APROBADO",
    "fechaEvento": "2026-02-20"
  }
]
```

---

### 9. Listar eventos PENDIENTES (ADMIN solo)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/pendientes`  
**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
```

**Response esperado** (200):
```json
[
  {
    "id": 1,
    "nombreEvento": "Conferencia de Tecnología",
    "estado": "PENDIENTE",
    "fechaEvento": "2026-02-15"
  }
]
```

---

### 10. Listar eventos RECHAZADOS (ADMIN solo)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/rechazados`  
**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
```

**Response esperado** (200):
```json
[
  {
    "id": 5,
    "nombreEvento": "Evento cancelado",
    "estado": "RECHAZADO",
    "motivoRechazo": "Conflicto de fechas"
  }
]
```

---

### 11. Listar eventos APROBADOS (Público)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/aprobados`  
**Headers**: (Sin autenticación requerida)

**Response esperado** (200):
```json
[
  {
    "id": 2,
    "nombreEvento": "Seminario de Gestión",
    "estado": "APROBADO",
    "fechaEvento": "2026-02-20"
  }
]
```

---

### 12. Aprobar o rechazar evento (ADMIN solo)

**Método**: `PUT`  
**URL**: `http://localhost:8080/api/eventos/1/aprobar-rechazar`  
**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Body (Aprobar)**:
```json
{
  "estado": "APROBADO",
  "motivoRechazo": null
}
```

**Body (Rechazar)**:
```json
{
  "estado": "RECHAZADO",
  "motivoRechazo": "No hay disponibilidad en esa fecha"
}
```

**Response esperado** (200):
```json
{
  "id": 1,
  "nombreEvento": "Conferencia de Tecnología",
  "estado": "APROBADO",
  "motivoRechazo": null
}
```

---

### 13. Actualizar evento (USER o ADMIN - propietario)

**Método**: `PUT`  
**URL**: `http://localhost:8080/api/eventos/1`  
**Headers**:
```
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "nombreEvento": "Conferencia de Tecnología (Actualizada)",
  "descripcion": "Nueva descripción",
  "fechaEvento": "2026-02-16",
  "horaInicio": "10:00",
  "horaFin": "13:00",
  "numeroAsistentes": 200,
  "publicoExterno": true,
  "requiereRegistroPrevio": false,
  "tipoDisposicion": "Auditorio Principal"
}
```

**Response esperado** (200):
```json
{
  "id": 1,
  "nombreEvento": "Conferencia de Tecnología (Actualizada)",
  "numeroAsistentes": 200,
  "estado": "PENDIENTE"
}
```

---

### 14. Verificar disponibilidad de fecha/hora

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/disponibilidad?fecha=2026-02-15&horaInicio=14:00&horaFin=16:00`  
**Headers**:
```
Authorization: Bearer <TOKEN>
```

**Response esperado** (200):
```json
true
```

---

### 15. Cancelar evento (USER o ADMIN - propietario)

**Método**: `PUT`  
**URL**: `http://localhost:8080/api/eventos/1/cancelar?motivo=Cambio de planes`  
**Headers**:
```
Authorization: Bearer <TOKEN>
```

**Response esperado** (200):
```json
{
  "id": 1,
  "nombreEvento": "Conferencia de Tecnología",
  "estado": "CANCELADO",
  "motivoRechazo": "Cambio de planes"
}
```

---

### 16. Eliminar evento (USER o ADMIN - propietario)

**Método**: `DELETE`  
**URL**: `http://localhost:8080/api/eventos/1`  
**Headers**:
```
Authorization: Bearer <TOKEN>
```

**Response esperado** (204): Sin contenido

---

### 17. Listar eventos por fecha específica (ADMIN solo)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/fecha/2026-02-15`  
**Headers**:
```
Authorization: Bearer <ADMIN_TOKEN>
```

**Response esperado** (200):
```json
[
  {
    "id": 1,
    "nombreEvento": "Conferencia de Tecnología",
    "fechaEvento": "2026-02-15",
    "horaInicio": "09:00",
    "horaFin": "12:00"
  }
]
```

---

### 18. Obtener eventos próximos (Público)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/proximos?dias=30`  
**Headers**: (Sin autenticación requerida)

**Response esperado** (200):
```json
[
  {
    "id": 2,
    "nombreEvento": "Seminario de Gestión",
    "fechaEvento": "2026-02-20",
    "estado": "APROBADO"
  }
]
```

---

### 19. Ver calendario completo (Público)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/calendario-completo`  
**Headers**: (Sin autenticación requerida)

**Response esperado** (200):
```json
{
  "aprobados": [
    {
      "id": 2,
      "nombreEvento": "Seminario de Gestión",
      "fechaEvento": "2026-02-20",
      "estado": "APROBADO"
    }
  ],
  "pendientes": [
    {
      "id": 1,
      "nombreEvento": "Conferencia de Tecnología",
      "fechaEvento": "2026-02-15",
      "estado": "PENDIENTE"
    }
  ]
}
```

---

### 20. Ver calendario con rango de fechas (Público)

**Método**: `GET`  
**URL**: `http://localhost:8080/api/eventos/calendario-completo?fechaInicio=2026-02-01&fechaFin=2026-02-28`  
**Headers**: (Sin autenticación requerida)

**Response esperado** (200):
```json
{
  "aprobados": [...],
  "pendientes": [...]
}
```

---

## 📌 RESUMEN DE PERMISOS

| Endpoint | GET | POST | PUT | DELETE | Requiere | Rol |
|----------|-----|------|-----|--------|----------|-----|
| `/auth/register` | - | ✅ | - | - | No | Público |
| `/auth/login` | - | ✅ | - | - | No | Público |
| `/api/eventos` | ✅ | ✅ | - | - | JWT | ADMIN (GET), USER/ADMIN (POST) |
| `/api/eventos/mis-eventos` | ✅ | - | - | - | JWT | USER/ADMIN |
| `/api/eventos/pendientes` | ✅ | - | - | - | JWT | ADMIN |
| `/api/eventos/rechazados` | ✅ | - | - | - | JWT | ADMIN |
| `/api/eventos/aprobados` | ✅ | - | - | - | No | Público |
| `/api/eventos/{id}` | ✅ | - | ✅ | ✅ | JWT | USER/ADMIN |
| `/api/eventos/{id}/aprobar-rechazar` | - | - | ✅ | - | JWT | ADMIN |
| `/api/eventos/{id}/cancelar` | - | - | ✅ | - | JWT | USER/ADMIN |
| `/api/eventos/disponibilidad` | ✅ | - | - | - | JWT | USER/ADMIN |
| `/api/eventos/fecha/{fecha}` | ✅ | - | - | - | JWT | ADMIN |
| `/api/eventos/proximos` | ✅ | - | - | - | No | Público |
| `/api/eventos/calendario-completo` | ✅ | - | - | - | No | Público |

---

## 🚀 FLUJO RECOMENDADO DE PRUEBAS

1. **Registrar usuarios** (Admin y User)
2. **Hacer login** con ambos y guardar tokens
3. **Crear evento** como User con token de usuario
4. **Ver mis eventos** como User
5. **Ver pendientes** como Admin
6. **Aprobar evento** como Admin
7. **Ver calendario completo** sin autenticación
8. **Actualizar evento** como propietario User
9. **Cancelar evento** como propietario User
10. **Eliminar evento** como propietario User

---

## ⚠️ NOTAS IMPORTANTES

- **Puerto**: La aplicación corre en `http://localhost:8080` (cambiar si es diferente)
- **JWT Token**: Válido por **1 hora** (3600 segundos)
- **Formato de fecha**: `YYYY-MM-DD` (ej: 2026-02-15)
- **Formato de hora**: `HH:mm` (ej: 14:30)
- **Roles**: Se prefijan automáticamente con `ROLE_` si no los tienen
- **CORS**: Habilitado para `localhost:4200`, `localhost:3000`, `localhost:8080`

---
