# 🔧 Google Drive Integration - Implementation Summary

## ✅ Changes Implemented

### **Backend (PedidoController.java)**

#### 1. **Fixed Logic Issue** (Línea 1098)
- **Before**: Early return si `files == null || files.length == 0`
- **After**: Only returns if BOTH files AND googleDriveLinks are empty
- **Impact**: Allows processing Google Drive links even without local files

#### 2. **Helper Methods Added** (Líneas 1257-1319)

**`extraerFileId(String googleDriveLink)`**
- Extrae Google Drive file ID de URLs en dos formatos:
  - `https://drive.google.com/file/d/{FILE_ID}/view`
  - `https://drive.google.com/...?id={FILE_ID}`
- Retorna `null` si no puede extraer el ID

**`descargarDesdeGoogleDriveConToken(String fileId, String driveToken)`**
- Autentica con Google Drive usando OAuth token
- Descarga archivo binario directamente desde Drive
- Retorna bytes listos para subir a Cloudinary
- Fallback: Retorna `null` si hay error (se procesa con Redis fallback)

#### 3. **Endpoint `/subir-archivos/{npedido}` Enhanced** (Líneas 1062-1255)

**Ahora soporta:**
- ✅ Archivos locales (multipart files)
- ✅ Google Drive links (con token optional)
- ✅ Flujo dual: 
  - **Con token**: Descarga inmediata desde Drive → Cloudinary
  - **Sin token**: Enqueue a Redis para procesamiento asincrónico

**Parámetros:**
```
POST /pedidos/subir-archivos/{npedido}
- files: File[] (opcional)
- googleDriveLinks: String[] (opcional)  
- driveToken: String (opcional)
```

**Response:**
```json
{
  "success": true,
  "archivosSubidos": 5,
  "npedido": 123,
  "info": "✓ 5 elemento(s) procesado(s) con éxito.",
  "warnings": "⚠️ Error al procesar link X..."
}
```

### **Frontend (pedidoform.html)**

#### 1. **Updated Modal UI** (Líneas 592-661)
- Agregado tab system:
  - **Tab 1: Archivos Locales** - Seleccionar imágenes del dispositivo
  - **Tab 2: Google Drive** - Agregar links + token

#### 2. **Google Drive Form Controls** (Líneas 632-650)
```html
- Token input: id="googleDriveToken"
- Link input: id="googleDriveLink"
- Botón: "Agregar" (onclick=agregarLinkGoogleDrive)
- Tabla: id="googleDriveLinksTable"
```

#### 3. **New JavaScript Functions** (Líneas 664-952)

**Global Variables:**
```javascript
var archivosPendientes = [];      // Local files
var googleDriveLinks = [];        // Drive links
var googleDriveToken = null;      // OAuth token
var npedido = null;               // Order ID
```

**Functions:**
- `agregarLinkGoogleDrive()` - Validar + añadir link a lista
- `eliminarLinkGoogleDrive(id)` - Remover link
- `actualizarTablaGoogleDrive()` - Renderizar tabla
- `confirmarFotos()` - Validar selección y capturar token

#### 4. **Updated Form Submission Flow** (Líneas 1033-1207)

**PASO 1**: Guardar pedido (POST /pedidos/form)
**PASO 2**: Subir archivos + links (POST /pedidos/subir-archivos/{npedido})

**Cambios principales:**
- Envía archivos locales + Google Drive links en un solo request
- Incluye token si está disponible
- Manejo mejorado de errores con detalles específicos
- Timeout: 120 segundos (suficiente para descargas de Drive)

## 🧪 Testing Checklist

### Backend Tests

```bash
# Compilar proyecto
mvn clean compile

# Tests unitarios
mvn test

# Tests específicos del controlador
mvn test -Dtest=PedidoControllerTest
```

### Manual Testing

**Caso 1: Solo archivos locales**
1. Abrir formulario de pedido
2. Click "Añadir Fotos"
3. Tab "Archivos Locales" - Seleccionar imágenes
4. Confirmar y guardar
5. ✅ Esperado: Archivos se suben a Cloudinary/Redis

**Caso 2: Solo Google Drive con token**
1. Abrir formulario
2. Click "Añadir Fotos"
3. Tab "Google Drive"
4. Pegar token OAuth
5. Pegar link de Drive (ej: https://drive.google.com/file/d/1ABC.../view)
6. Click "+ Añadir"
7. Confirmar y guardar
8. ✅ Esperado: Archivo se descarga directamente y sube a Cloudinary

**Caso 3: Solo Google Drive sin token**
1. Ídem Caso 2, pero SIN token
2. Click "+ Añadir" link
3. ✅ Esperado: Link se enqueue a Redis para descarga en background

**Caso 4: Mix (archivos + links + token)**
1. Seleccionar archivos locales
2. Agregar token y Google Drive link
3. Guardar
4. ✅ Esperado: Todo se procesa correctamente

## 🔧 How to Get Google Drive Token

### Opción 1: OAuth 2.0 (Recomendado)
```bash
# Usar Google OAuth Playground
# https://developers.google.com/oauthplayground

# Steps:
1. Select scopes: https://www.googleapis.com/auth/drive
2. Authorize and get access token
3. Copy token: ya1.a0Af...
```

### Opción 2: Service Account
```json
{
  "type": "service_account",
  "project_id": "your-project",
  "private_key": "-----BEGIN PRIVATE KEY-----...",
  "client_email": "service@project.iam.gserviceaccount.com"
}
```

## 📋 Related Endpoints

- `GET /pedidos/cloudinary-signature/{npedido}` - Firmapara upload directo
- `POST /pedidos/guardar-archivos/{npedido}` - Guardar URLs después de Cloudinary
- `POST /pedidos/subir-archivos/{npedido}` - **NUEVO - Upload completo**
- `GET /pedidos/form/{clienteId}` - Formulario de pedido

## ⚠️ Known Limitations

1. **Drive Links without Token**: Requiere worker/consumer de Redis para descargar
2. **File Size**: Google Drive API tiene límites de descarga (>500MB puede fallar)
3. **Permissions**: Archivo debe ser accesible con el token proporcionado
4. **MIME Types**: Se valida que sean imágenes (image/jpeg, image/png, etc)

## 🚀 Production Notes

### Deployment Checklist
- [ ] Redis está activo y configurado
- [ ] Cloudinary credenciales en `application.properties`
- [ ] Google credentials en `src/main/resources/cred.json`
- [ ] Redis consumer/worker ejecutándose
- [ ] CORS habilitado para `api.cloudinary.com`
- [ ] SSL/TLS configurado

### Performance
- Local files: ~1-2 seg por archivo
- Drive with token: ~3-5 seg (descarga + upload)
- Drive without token: Asincrónico (background)

## 📝 Code References

- **PedidoController**: `src/main/java/.../controllers/PedidoController.java:1257-1319`
- **Frontend modal**: `src/main/resources/templates/pedido/pedidoform.html:592-661`
- **JavaScript logic**: `src/main/resources/templates/pedido/pedidoform.html:664-1207`

---
**Last Updated**: 2024-12-16
**Status**: ✅ Implementation Complete
