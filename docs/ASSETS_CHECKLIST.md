# Checklist publicación — Concursos CGE

Marcá cuando esté listo. Si ya publicaste otras apps, varios pasos te serán familiares.

## Lo que ya está en el proyecto

- [x] Política de privacidad online
- [x] Textos de ficha en `PLAY_STORE_LISTING.md`

## Lo que necesitamos de vos

| # | Dato | Para qué |
|---|------|----------|
| 1 | **Imagen del ícono** (PNG, fondo transparente, ideal 1024×1024) | Launcher + Play Store |
| 2 | **Correo de soporte** | Ficha de la tienda |
| 3 | **Capturas** 1080×1920 (mín. 2) | Play Console |
| 4 | **IDs reales de AdMob** (opcional al inicio) | `gradle.properties` |
| 5 | Firmar con tu keystore | Generar `.aab` |

## Play Console — orden sugerido

1. **Crear app** → package `ar.gov.entrerios.cge.concursos`
2. **Ficha de Play Store**
   - Subir ícono 512×512 (tu imagen)
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
