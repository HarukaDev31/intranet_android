# Intranet Probusiness — App Android (Login + Soporte TI + Push)

App nativa Android (Kotlin + Jetpack Compose) con **alcance intencionalmente reducido**: login contra
la intranet, manejo del token JWT (incl. refresh y logout), y el canal de **Soporte TI** (listar tickets,
crear ticket, chat de mensajes), con notificaciones push vía Firebase Cloud Messaging.

No incluye ningún otro módulo de la intranet (carga consolidada, RRHH, etc.) ni funciones de staff
(asignar, cambiar prioridad/complejidad/estado) — esas viven en el panel web para roles PM/Soporte.

Este proyecto fue compilado y verificado (`assembleDebug`) durante su creación, así que el código
en sí es válido; lo que falta es tu configuración de Firebase.

## Antes de abrir el proyecto

1. **Backend**: este cliente depende de cambios ya aplicados en `intranet_back` (tabla `usuario_device`,
   endpoints `POST/DELETE /api/auth/device/token`, envío de push desde `SoporteTiMensajeCreado`). Asegúrate
   de que ese backend esté desplegado con esos cambios antes de probar login/push end-to-end.
2. **Firebase**: crea (o usa) un proyecto en [Firebase Console](https://console.firebase.google.com/),
   agrega una app Android con el package `com.probusiness.intranet`, descarga el `google-services.json`
   real y reemplaza el archivo placeholder en `app/google-services.json` (el que hay ahora es un
   placeholder que solo permite compilar, no enviar/recibir push reales).
3. En el backend, genera una cuenta de servicio (Firebase Console → Configuración del proyecto →
   Cuentas de servicio → Generar nueva clave privada) y configura `FIREBASE_CREDENTIALS` /
   `FIREBASE_PROJECT_ID` en el `.env` del backend (ver `config/firebase.php`).

## Abrir y correr

1. Abre esta carpeta (`intranet_android`) en Android Studio (Koala/Ladybug o más reciente). Android Studio
   detectará el wrapper de Gradle (`./gradlew`, ya generado y probado — Gradle 8.14.3) y sincronizará solo.
2. Si necesitas apuntar a otro backend que no sea QA, cambia `BASE_URL` en `app/build.gradle.kts`
   (`defaultConfig.buildConfigField`).
3. Ejecuta la app en un emulador o dispositivo con Android 8.0 (API 26) o superior.

## Notas sobre las notificaciones push

- El backend envía cada push con payload combinado `notification` + `data`. Con la app en **primer plano**,
  `IntranetFirebaseMessagingService.onMessageReceived` construye la notificación manualmente (con el ícono
  y canal `soporte_ti`) y al tocarla navega directo al chat del ticket.
- Con la app en **segundo plano o cerrada**, Android muestra la notificación automáticamente (usando el
  ícono/canal configurados como `default_notification_icon`/`default_notification_channel_id` en el
  Manifest) y `onMessageReceived` no se ejecuta — es comportamiento estándar de FCM, no un bug. El tap
  igual abre el chat correcto porque `MainActivity` lee tanto el extra propio como las claves crudas del
  payload `data` que Android reenvía en ese caso.
- El token FCM se envía en el login (`platform=android`, `fcm_token`, `device_id`) y también se reenvía
  si Firebase lo rota mientras hay sesión activa (`onNewToken` → `POST /auth/device/token`). Si rota sin
  sesión activa, se guarda localmente y se sincroniza en el siguiente login.

## Estructura

- `data/remote` — Retrofit `ApiService`, DTOs (kotlinx.serialization), interceptors de auth/refresh.
- `data/local` — `SessionManager` (token y datos de usuario en `EncryptedSharedPreferences`).
- `data/repository` — `AuthRepository`, `SupportRepository`, `DeviceInfoProvider`.
- `ui/login`, `ui/support`, `ui/navigation` — pantallas Compose + ViewModels + grafo de navegación.
- `notifications` — `IntranetFirebaseMessagingService` + `NotificationHelper`.
- `di` — módulos Hilt (red).

## Íconos

`app/src/main/res/drawable/ic_launcher_foreground.xml` y `ic_launcher_background.xml` son un placeholder
vectorial (círculo/anillo) — reemplázalos por el logo real de Probusiness cuando lo tengas.
