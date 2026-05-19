# Publicar Concursos CGE en Google Play

## 1. Cuenta y requisitos

1. [Cuenta de desarrollador de Google Play](https://play.google.com/console/signup) (pago único).
2. Verificar identidad y completar el perfil de desarrollador.

## 2. Firma de release

```bash
keytool -genkey -v -keystore concursos-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias concursos
```

Copiá `keystore.properties.example` a `keystore.properties` y completá las rutas (ese archivo **no** se sube a Git).

Generá el bundle:

```bash
./gradlew :app:bundleRelease
```

El `.aab` queda en `app/build/outputs/bundle/release/`.

## 3. AdMob (monetización)

1. Creá una app en [AdMob](https://admob.google.com/).
2. Creá un bloque **Banner** y copiá los IDs reales.
3. En `gradle.properties` reemplazá:

```properties
ADMOB_APP_ID=ca-app-pub-XXXXXXXX~YYYYYYYY
ADMOB_BANNER_UNIT_ID=ca-app-pub-XXXXXXXX/ZZZZZZZZ
```

Los IDs de prueba de Google solo sirven en desarrollo; **no** uses ingresos reales con ellos.

## 4. Política de privacidad (obligatorio)

Play Console exige una **URL pública**. Opciones:

- **GitHub Pages:** publicá `docs/PRIVACY_POLICY.md` como sitio (o `docs/privacy.html`).
- Subí la URL en `gradle.properties`:

```properties
PRIVACY_POLICY_URL=https://tu-usuario.github.io/ConcursosAye/privacy
```

La misma URL va en Play Console → Política de privacidad.

## 5. Ficha de la tienda

| Campo | Sugerencia |
|-------|------------|
| Título | Concursos CGE — Alertas docentes |
| Descripción corta | Avisos de concursos del CGE Entre Ríos según tus palabras clave. |
| Categoría | Educación |
| Contenido | Todos / sin restricción de edad |
| Anuncios | Sí, contiene anuncios |
| Datos recopilados | Declarar identificador de publicidad (AdMob) |

### Capturas

Mínimo 2 capturas de teléfono (1080×1920 o similar): Home con listado, Pantalla de keywords, Ajustes.

### Icono

512×512 PNG para la tienda. El proyecto usa `ic_launcher_foreground`; exportá una versión completa si hace falta.

## 6. Declaraciones en Play Console

- **No es app gubernamental oficial** — aclarar en la descripción que no está afiliada al CGE.
- **Permiso POST_NOTIFICATIONS** — explicar que es para alertas de concursos.
- **ID de publicidad** — declarar uso por AdMob.
- **Seguridad de datos** — formulario según lo que declare AdMob y almacenamiento local.

## 7. Pruebas antes de producción

- [ ] `bundleRelease` firma correctamente.
- [ ] Sync con datos reales (sin spamear el CGE).
- [ ] Anuncios con IDs reales en build release interno.
- [ ] Política de privacidad accesible desde Ajustes.

## 8. Lanzamiento

1. Play Console → Crear aplicación → Subir `.aab` a **Prueba interna**.
2. Probar con testers.
3. Promover a **Producción** (revisión de Google, 1–7 días).

## Bloqueos del sitio CGE (Wordfence)

Si el WiFi o la IP fueron limitados por exceso de consultas, el bloqueo suele **levantarse solo** en 15–60 minutos. La app ya limita peticiones; evitá sincronizar en bucle durante las pruebas.
