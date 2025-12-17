# 🚀 SETUP Google OAuth - Guía Rápida (5 minutos)

## ❌ Error Actual
```
Not a valid origin for the client: http://localhost:8080 has not been registered 
for client ID YOUR_CLIENT_ID_HERE.apps.googleusercontent.com
```

**Causa**: Placeholder no reemplazado + origen no configurado

---

## ✅ Solución Paso a Paso

### 1️⃣ Ir a Google Cloud Console (2 min)

```
https://console.cloud.google.com
```

#### Si NO tienes proyecto:
- Clic en "Select a Project" → "New Project"
- Nombre: `joyeria-app`
- Crear

#### Si YA tienes proyecto:
- Selecciona el proyecto existente

---

### 2️⃣ Habilitar APIs (1 min)

1. Ir a **APIs & Services** → **Library**
2. Buscar: `Google Drive API`
   - Clic en resultado
   - Clic en **ENABLE**
3. Buscar: `Google Picker API`
   - Clic en resultado
   - Clic en **ENABLE**

---

### 3️⃣ Crear Credencial OAuth 2.0 (2 min)

1. Ir a **APIs & Services** → **Credentials**
2. Clic en **+ Create Credentials**
3. Seleccionar: **OAuth client ID**
4. Si te pide "Configure OAuth consent screen":
   - Clic en **Create Consent Screen**
   - Seleccionar **External**
   - Rellenar:
     - App name: `Joyería`
     - User support email: tu email
     - Developer contact: tu email
   - Clic en **Save and Continue**
   - En "Scopes": agregar manualmente:
     - `https://www.googleapis.com/auth/drive.readonly`
   - Clic en **Save and Continue**
   - **Save and go back**

5. Ahora crear credential:
   - Tipo: **Web application**
   - Nombre: `joyeria-local`
   
6. **Authorized JavaScript origins** (agregar):
   ```
   http://localhost:8080
   ```
   - Clic en **+ Add URI**
   - Escribir: `http://localhost:8080`
   - Clic en **Create**

7. **COPIAR el Client ID** que aparece (algo como: `123456789-abc...apps.googleusercontent.com`)

---

### 4️⃣ Reemplazar en tu código

#### En `application.properties` (línea 114):
```properties
GOOGLE_CLIENT_ID=123456789-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com
```

#### En `pedidoform.html` (línea 677):
```javascript
const GOOGLE_CLIENT_ID = '123456789-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com';
```

**Cambiar `YOUR_CLIENT_ID_HERE.apps.googleusercontent.com` por tu ID real**

---

### 5️⃣ Reiniciar aplicación

```bash
# Si está corriendo, para
# Ctrl+C

# Reinicia
mvn spring-boot:run
```

O reinicia en tu IDE (Run → Stop → Run)

---

## ✅ Verificación

### En navegador:
```
http://localhost:8080/pedidos/form/1
```

1. Abre modal "Añadir Fotos"
2. Tab "Google Drive"
3. Debería ver: ✅ Botón verde "🔍 Abrir Google Drive"
4. En consola (F12): NO debe haber error rojo sobre `idpiframe_initialization_failed`

### Logs esperados:
```
✅ Google Auth2 inicializado
```

---

## 🔧 Si Sigue Fallando

### Error: "invalid_client"
- El Client ID está mal copiado
- Verificar caracteres especiales
- Copiar nuevamente de Google Cloud

### Error: "idpiframe_initialization_failed"
- El origin NO está registrado en Google Cloud
- Ir a Credentials → Edit (ícono lápiz)
- Verificar que `http://localhost:8080` esté en "Authorized JavaScript origins"
- Recargar navegador (Ctrl+Shift+R para limpiar caché)

### Botón no aparece/deshabilitado
- Esperar 3-5 segundos después de abrir modal
- Abrir consola (F12) y buscar mensajes de Google API
- Puede haber error de CORS

---

## 📱 Flujo Correcto Tras Config

1. **Abres modal** → Google API se inicializa
2. **Tab Google Drive** → Botón aparece HABILITADO
3. **Clicas botón** → Google Auth se abre (login)
4. **Seleccionas archivos** → Picker muestra Drive
5. **Seleccionas fotos** → Se agregan a tabla
6. **Confirmas** → Se envían a servidor

---

## 🎯 IMPORTANTE para Producción

Cuando despliegues a servidor real, agregar origen:

En Google Cloud Console → Credentials → Edit:

```
Authorized JavaScript origins:
http://localhost:8080
https://tudominio.com
https://www.tudominio.com
```

**NO puedes usar Google Picker en producción sin HTTPS**

---

## ✨ Resumen

| Paso | Acción | Tiempo |
|------|--------|--------|
| 1 | Ir a Google Cloud | 1 min |
| 2 | Habilitar APIs | 1 min |
| 3 | Crear OAuth + copiar Client ID | 2 min |
| 4 | Reemplazar en código | 1 min |
| 5 | Reiniciar app | - |
| **TOTAL** | | **~5 min** |

---

## 📞 Debugging

Si sigue sin funcionar, revisa logs con estos patterns:

```javascript
// En consola del navegador (F12):
// Busca:
initializeGoogleAPI
updateAuthStatus
openGoogleDrivePicker
pickerCallback

// En logs de aplicación:
mvn spring-boot:run | grep -i "google\|drive\|picker"
```

