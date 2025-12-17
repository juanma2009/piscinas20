# Configuración Google Drive Picker - Guía Completa

## 🎯 Descripción General

Este documento describe cómo configurar Google Drive Picker en la aplicación para permitir que los usuarios seleccionen fotos directamente desde su Google Drive.

---

## 📋 Requisitos Previos

- Cuenta de Google Cloud Platform (GCP)
- Acceso a Google Cloud Console
- Proyecto creado en GCP
- OAuth 2.0 configurado

---

## 🔧 Pasos de Configuración

### 1. Crear/Configurar Proyecto en Google Cloud Console

1. Ir a [Google Cloud Console](https://console.cloud.google.com)
2. Crear un nuevo proyecto (si no existe)
3. Ir a **APIs & Services** → **Library**
4. Buscar y habilitar:
   - **Google Drive API**
   - **Google Picker API**

### 2. Crear Credenciales OAuth 2.0

1. Ir a **APIs & Services** → **Credentials**
2. Hacer clic en **Create Credentials** → **OAuth client ID**
3. Seleccionar **Web application**
4. Completar la información:
   - **Name**: `joyeria-app-picker`
   - **Authorized JavaScript origins**:
     - `http://localhost:8080`
     - `https://tudominio.com` (tu dominio en producción)
   - **Authorized redirect URIs**: (dejar en blanco para Picker)
5. Copiar el **Client ID**
6. En caso necesario, crear también un **API Key** (Cloud Console → APIs & Services → Credentials → Create Credentials → API Key)

### 3. Actualizar Configuración en la Aplicación

#### application.properties

```properties
# Google OAuth Configuration
GOOGLE_CLIENT_ID=YOUR_CLIENT_ID.apps.googleusercontent.com
GOOGLE_API_KEY=YOUR_API_KEY
```

#### Reemplazar en pedidoform.html

En la línea 677, reemplazar:
```javascript
const GOOGLE_CLIENT_ID = 'YOUR_CLIENT_ID_HERE.apps.googleusercontent.com';
```

Con tu Client ID real:
```javascript
const GOOGLE_CLIENT_ID = '123456789-abcdefghijk.apps.googleusercontent.com';
```

---

## 🏗️ Arquitectura de la Solución

### Frontend (JavaScript)

```
┌─────────────────────────────────────────┐
│  Modal: Añadir Fotos                    │
│  ├─ Tab: Archivos Locales               │
│  │  └─ Input tipo file (traditional)    │
│  └─ Tab: Google Drive                   │
│     ├─ Botón: "Abrir Google Drive"      │
│     ├─ Google Auth2 (OAuth)             │
│     ├─ Google Picker (selección)        │
│     └─ Tabla: Archivos seleccionados    │
└─────────────────────────────────────────┘
```

### Backend (Java)

```
POST /pedidos/subir-archivos/{npedido}
├─ Parámetros:
│  ├─ files (multipart) - Archivos locales
│  ├─ googleDriveFileIds - IDs de Google Drive
│  └─ googleDriveToken - Token OAuth
├─ Procesamiento:
│  ├─ Archivos locales → Base64 → Redis → Background upload
│  └─ Drive files → Descarga API → Cloudinary (inmediato)
└─ Respuesta: JSON con estado
```

---

## 📱 Flujo de Uso

### Usuario

1. Abre modal "Añadir Fotos"
2. **Opción A - Archivos Locales**:
   - Selecciona archivos de su PC
   - Se agregan a tabla
3. **Opción B - Google Drive**:
   - Clica "Abrir Google Drive"
   - Autentica con Google (primera vez)
   - Picker abre mostrando archivos de Drive
   - Selecciona fotos (múltiple)
   - Archivos se agregan a tabla
4. Clica "Confirmar Selección"
5. Formulario se guarda
6. Archivos se suben a:
   - Locales → Redis (cola) → Cloudinary (background)
   - Drive → Cloudinary (inmediato)

---

## 🔐 Seguridad

### Tokens
- **ID Token**: Se obtiene de `auth2.currentUser.get().getAuthResponse().id_token`
- **Access Token**: Requiere scope `drive.readonly` para descargar archivos
- Los tokens se envían en encabezado `Authorization: Bearer {token}`

### Validaciones
- Solo se aceptan imágenes (MIME type)
- Tamaño máximo: 50MB por archivo
- Google Drive API v3 con autenticación OAuth

---

## 🐛 Solución de Problemas

### "Google API aún está cargando"
- Esperar a que `gapi` se cargue (2-3 segundos)
- Verificar que `https://apis.google.com/js/api.js` se cargó correctamente

### "❌ No se pudo autenticar con Google"
- Verificar que el Client ID es correcto
- Verificar que el dominio está en "Authorized JavaScript origins"
- Limpiar cookies/caché del navegador

### "Archivo no encontrado en Drive (404)"
- El archivo se movió o fue eliminado
- El usuario no tiene permiso de lectura
- El archivo ID es incorrecto

### "Token inválido o expirado (401)"
- El token expiró (típicamente 1 hora)
- El usuario revocó el acceso a la app
- Solicitar autenticación nuevamente

---

## 📊 Estructura de Datos

### googleDriveLinks (JavaScript)
```javascript
[
  {
    id: "FILE_ID_123",           // Google Drive File ID
    name: "foto1.jpg",            // Nombre del archivo
    size: 1048576                 // Tamaño en bytes
  }
]
```

### FormData enviado al servidor
```
POST /pedidos/subir-archivos/{npedido}
- files: MultipartFile[]         // Archivos locales
- googleDriveFileIds: String[]   // File IDs de Drive
- googleDriveToken: String       // Bearer token
```

---

## 🚀 Despliegue en Producción

### 1. Actualizar CORS en GCP
En Google Cloud Console → APIs & Services → Credentials:
```
Authorized JavaScript origins:
- https://tudominio.com
- https://www.tudominio.com
```

### 2. Configurar variables de entorno
```bash
GOOGLE_CLIENT_ID=tuClientID@apps.googleusercontent.com
GOOGLE_API_KEY=tu_api_key
```

### 3. HTTPS obligatorio
Google Picker requiere HTTPS en producción.

---

## 📝 Cambios Realizados

### Frontend (`pedidoform.html`)
- Agregado Google API script (`https://apis.google.com/js/api.js`)
- Nuevo sistema de autenticación con Google Auth2
- Google Picker integrado en modal
- Tabla de archivos seleccionados de Drive
- FormData actualizado para enviar fileIds

### Backend (`PedidoController.java`)
- Nuevo endpoint con parámetros `googleDriveFileIds` y `googleDriveToken`
- Método `descargarDesdeGoogleDriveAPI()` para descargar archivos
- Integración directa con Cloudinary (sin Redis para archivos de Drive)

### Backend (`RedisQueueConsumer.java`)
- Simplificado para solo procesar archivos locales
- Eliminada toda lógica de descarga de Google Drive
- Mantenido flujo de Base64 → Redis → Cloudinary

### Config (`application.properties`)
- Nuevas propiedades: `GOOGLE_CLIENT_ID` y `GOOGLE_API_KEY`

---

## ✅ Testing Checklist

- [ ] Google API carga correctamente
- [ ] Botón "Abrir Google Drive" está visible y funciona
- [ ] Primera autenticación redirige a Google
- [ ] Picker abre y muestra archivos de Drive
- [ ] Se pueden seleccionar múltiples archivos
- [ ] Archivos se agregan a la tabla correctamente
- [ ] Se pueden eliminar archivos de la tabla
- [ ] Formulario se guarda con archivos locales
- [ ] Formulario se guarda con archivos de Drive
- [ ] Archivos locales se procesan en background
- [ ] Archivos de Drive se suben a Cloudinary inmediatamente
- [ ] En logs aparecen los mensajes con emojis correctos

---

## 📞 Contacto / Soporte

Para problemas o preguntas, revisar los logs de la aplicación en nivel DEBUG.

Buscar en logs:
- `initializeGoogleAPI` → Inicialización de Google
- `openGoogleDrivePicker` → Apertura del Picker
- `pickerCallback` → Archivos seleccionados
- `Descargando archivo de Drive` → Descarga API
- `Cloudinary` → Upload final

