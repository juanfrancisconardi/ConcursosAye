# Concursos CGE

Aplicación Android para docentes y personal educativo de Entre Ríos. Monitorea las publicaciones de concursos del **Consejo General de Educación** ([cge.entrerios.gov.ar/concursos](https://cge.entrerios.gov.ar/concursos/)) y avisa cuando aparecen avisos que coinciden con tus **palabras clave**.

> **Aviso:** aplicación independiente, no oficial ni afiliada al CGE.

## Funciones principales

- Sincronización ligera: **una consulta** al índice de concursos por sync (respeta límites del sitio).
- Palabras clave con matching tolerante a acentos y mayúsculas.
- Notificaciones de concursos relevantes.
- Modo “solo al abrir” o chequeo diario programado.
- Filtro por categoría (Inicial, Primario, Secundario, Superior, Supervisor) y por antigüedad (días).

## Stack

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · WorkManager · Retrofit/OkHttp · Jsoup

## Compilar

1. Android Studio Koala+ con SDK 34.
2. Abrir la carpeta del proyecto y sincronizar Gradle.
3. Run en dispositivo API 24+.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## Publicar en Play Store

| Documento | Contenido |
|-----------|-----------|
| [docs/PLAY_STORE.md](docs/PLAY_STORE.md) | Firma, AdMob, bundle, checklist |
| [docs/GITHUB_PAGES.md](docs/GITHUB_PAGES.md) | Activar la URL de privacidad |
| [docs/PLAY_STORE_LISTING.md](docs/PLAY_STORE_LISTING.md) | Textos para copiar en Play Console |

**URL de privacidad:** https://juanfrancisconardi.github.io/ConcursosAye/privacy.html (requiere activar Pages en GitHub)

- Firma release: `keystore.properties.example`
- Monetización: **Google AdMob** (banner). Configurar IDs en `gradle.properties`

## Configuración de AdMob y privacidad

En `gradle.properties`:

```properties
ADMOB_APP_ID=ca-app-pub-...
ADMOB_BANNER_UNIT_ID=ca-app-pub-.../...
PRIVACY_POLICY_URL=https://tu-dominio/privacy
```

Los valores por defecto son **IDs de prueba de Google** (solo desarrollo).

## Estructura

```text
app/src/main/java/ar/gov/entrerios/cge/concursos/
├── core/          # modelos, Room, red, scraper, utilidades
├── data/          # repositorios y mappers
├── domain/        # casos de uso
├── feature/       # pantallas (home, results, keywords, settings, details)
├── ads/           # banner AdMob
├── work/          # sincronización en background
└── ui/            # tema y componentes compartidos
```

## Limitaciones

- El CGE no ofrece API pública; la app lee HTML. Cambios en el sitio pueden requerir ajustes en el scraper.
- Wordfence puede limitar IPs con muchas consultas; la app aplica throttle y cooldown automático.
- Intervalo mínimo de WorkManager en modo diario: 15 minutos (restricción de Android).

## Licencia

Uso privado / publicación según decisión del titular del repositorio.
