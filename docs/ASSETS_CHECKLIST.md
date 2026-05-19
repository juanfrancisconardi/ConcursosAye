# Checklist publicación — Concursos CGE

Marcá cuando esté listo. Si ya publicaste otras apps, varios pasos te serán familiares.

## Lo que ya está en el proyecto

- [x] Icono adaptativo en la app (`mipmap` + vectores)
- [x] Icono de notificaciones monocromático
- [x] Política de privacidad online
- [x] Textos de ficha en `PLAY_STORE_LISTING.md`
- [x] PNG para Play en `store-assets/`

## Lo que necesitamos de vos

| # | Dato | Para qué |
|---|------|----------|
| 1 | **Correo de soporte** (el de tu cuenta Play o uno dedicado) | Ficha de la tienda y política de privacidad |
| 2 | **Capturas** 1080×1920 (mín. 2) | Play Console |
| 3 | **IDs reales de AdMob** (si querés ingresos antes del release) | `gradle.properties` |
| 4 | Confirmar que **firmás** con tu keystore habitual | Generar `.aab` |

## Play Console — orden sugerido

1. **Crear app** → package `ar.gov.entrerios.cge.concursos`
2. **Ficha de Play Store**
   - Subir `store-assets/icon-512.png`
   - Subir `store-assets/feature-graphic-1024x500.png`
   - Subir capturas
   - Descripción: `docs/PLAY_STORE_LISTING.md`
   - URL privacidad: https://juanfrancisconardi.github.io/ConcursosAye/privacy.html
3. **Políticas de la app** → clasificación de contenido + seguridad de datos
4. **Prueba interna** → subir `app-release.aab`
5. **Producción** cuando la prueba esté OK

## Compilar el AAB (con tu keystore)

Android Studio → **Build → Generate Signed Bundle / APK** → **Android App Bundle** → tu keystore.

O, si tenés `keystore.properties` en la raíz del proyecto:

```bash
gradlew :app:bundleRelease
```

## Versión

Actual en `app/build.gradle.kts`: `versionCode = 1`, `versionName = "1.0.0"`.  
En cada actualización futura: subir `versionCode` siempre; `versionName` es lo que ve el usuario.
