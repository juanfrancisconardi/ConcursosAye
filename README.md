# Concursos CGE — Buscador de Concursos del Consejo General de Educación de Entre Ríos

Aplicación Android nativa (Kotlin + Jetpack Compose + Material 3) que monitorea
automáticamente las publicaciones de concursos del sitio del
**Consejo General de Educación de Entre Ríos**
(<https://cge.entrerios.gov.ar/concursos/>) y notifica al usuario cuando aparecen
publicaciones que coinciden con sus **palabras clave** (terapista ocupacional,
psicólogo, equipo técnico, EOE, SAIE, etc.).

---

## Stack técnico

| Capa            | Tecnología                                              |
|-----------------|---------------------------------------------------------|
| Lenguaje        | **Kotlin 2.0.20**                                       |
| UI              | **Jetpack Compose** + **Material 3** (BOM 2024.09.02)   |
| Arquitectura    | **Clean Architecture + MVVM** (presentation / domain / data) |
| DI              | **Hilt 2.52**                                           |
| Persistencia    | **Room 2.6.1**                                          |
| Background      | **WorkManager 2.9.1**                                   |
| HTTP            | **Retrofit 2.11** + **OkHttp 4.12**                     |
| Scraping HTML   | **Jsoup 1.18.1** (sin WebView)                          |
| Concurrencia    | **Coroutines + Flow**                                   |
| Navegación      | **Navigation Compose 2.8.2**                            |
| Logging         | **Timber**                                              |

---

## Cómo compilar y correr

Tenés dos opciones equivalentes:

### Opción A — Android Studio (recomendada)

1. Instalar **Android Studio Koala (2024.1.1)** o superior.
2. `File → Open…` y seleccionar la carpeta raíz del proyecto.
3. Android Studio descargará el wrapper, sincronizará Gradle y generará automáticamente el `gradle/wrapper/gradle-wrapper.jar`.
4. Aceptar/instalar el Android SDK 34 cuando lo pida.
5. Conectar un dispositivo (API 24+) o crear un emulador.
6. Pulsar **Run ▶** sobre la configuración `app`.

### Opción B — Línea de comandos

```bash
# Requisitos: JDK 17 y Android SDK 34 + Build-Tools 34
# Primera vez: si no existe el gradle-wrapper.jar todavía, generalo:
gradle wrapper --gradle-version 8.9 --distribution-type bin

# Build de debug
./gradlew :app:assembleDebug

# Instalación directa en el dispositivo conectado
./gradlew :app:installDebug
```

> En Windows reemplazar `./gradlew` por `gradlew.bat`.

---

## Estructura del proyecto

```text
app/src/main/java/ar/gov/entrerios/cge/concursos/
├── ConcursosApp.kt               # @HiltAndroidApp + Configuration.Provider
├── MainActivity.kt               # @AndroidEntryPoint + Compose root + deep links
│
├── core/
│   ├── model/                    # Concurso, Keyword, Category, AppSettings…
│   ├── util/                     # TextNormalizer, KeywordMatcher, DateFormatter…
│   ├── database/                 # Room: AppDatabase, DAOs, Entities, Relations
│   └── network/                  # Retrofit (CgeApi) + CgeScraper (Jsoup)
│
├── domain/
│   ├── repository/               # Interfaces (Concurso/Keyword/Settings)
│   └── usecase/                  # Casos de uso (Observe/Sync/Add/Update/Delete…)
│
├── data/
│   ├── mapper/                   # Entity ↔ Domain ↔ DTO
│   └── repository/               # Implementaciones de los repositorios
│
├── di/                           # Módulos Hilt (Network/Database/Repository)
├── work/                         # SyncConcursosWorker + SyncScheduler
├── notifications/                # NotificationHelper (canal, push, deep link)
├── navigation/                   # Rutas + NavHost + bottom nav
├── ui/
│   ├── theme/                    # Material 3 + DarkMode dinámico
│   └── components/               # ConcursoCard, EmptyState, LoadingIndicator
└── feature/
    ├── home/                     # Lista de concursos relevantes
    ├── results/                  # Todos los concursos + SearchBar
    ├── details/                  # Detalle + "Abrir sitio original"
    ├── keywords/                 # CRUD de palabras clave (chips)
    └── settings/                 # Intervalo, notificaciones, modo oscuro
```

---

## Cómo funciona el monitoreo (alto nivel)

1. **`ConcursosApp.onCreate`** crea (si hace falta) las keywords por defecto y
   programa un **`PeriodicWorkRequest`** con `SyncScheduler.schedulePeriodic()`.
2. Cada `refreshIntervalMinutes` (mínimo 15 — limitación de WorkManager)
   se lanza **`SyncConcursosWorker`**.
3. El worker invoca **`SyncConcursosUseCase → ConcursoRepositoryImpl.sync()`**:
   1. Descarga el HTML de cada **categoría monitoreada**
      (`/concursos/inicial/`, `/primario/`, `/secundario/`, `/superior/`, `/supervisor/`).
   2. Parsea con **Jsoup** y obtiene URL, título, fecha y excerpt.
   3. Para cada URL **nueva** (no presente en la tabla `concursos`):
      - descarga el detalle,
      - aplica **matching** con keywords del usuario,
      - calcula **score** (`TÍTULO = 10`, `CONTENIDO = 4`) y guarda en Room.
   4. Para URLs ya conocidas **no se vuelve a descargar**: sólo se
      recalcula el matching contra las keywords actuales (evita requests duplicados).
4. Si hay **nuevos concursos con `score > 0`** y las notificaciones están habilitadas,
   `NotificationHelper.notifyNewConcursos()` dispara una **notificación** por cada uno.
   Al tocarla, se abre el detalle vía deep link `concursoscge://detalle/{id}`.

### Matching robusto (`core/util/KeywordMatcher.kt`)

- **Tolerante a acentos**: NFD + remoción de diacríticos.
- **Insensible a mayúsculas**.
- **Coincidencias parciales** (substring sobre texto normalizado).
- Ejemplo: la keyword `"psicólogo"` también detecta `"PSICOLOGAS"`, `"Psicológica"`, etc.

### Scraping defensivo (`core/network/CgeScraper.kt`)

- Probaron varios selectores (`article.post`, `article`, `.entry-title a`, …)
  para sobrevivir cambios parciales en el tema WordPress del CGE.
- Limpia scripts, sidebars, footers y comentarios antes de extraer texto.
- Tolera fallos de detalle: si no puede descargar el contenido completo, usa
  el excerpt del listado y deja el ítem persistido.

---

## Pantallas

| Pantalla   | Descripción                                                                   |
|------------|-------------------------------------------------------------------------------|
| **Home**   | Concursos **relevantes** (score > 0) con chips de keywords y badge "NUEVO".  |
| **Resultados** | Todos los concursos descargados, con `DockedSearchBar` para filtrado.    |
| **Detalle**| Título + contenido completo + FAB "Abrir sitio original" (navegador externo).|
| **Keywords** | CRUD completo con chips, switches enable/disable y filtro de búsqueda.     |
| **Ajustes**| Intervalo (15min..24h), notificaciones, modo oscuro y categorías a monitorear. |

Diseño Material 3 con `LazyColumn`, `Card`, `PullToRefreshBox`, `FilterChip`,
`ExposedDropdownMenuBox`, `DockedSearchBar`, `ExtendedFloatingActionButton`.

---

## Permisos solicitados

- `INTERNET` y `ACCESS_NETWORK_STATE` — descargar HTML del CGE.
- `POST_NOTIFICATIONS` — solo Android 13+ (Tiramisu).
- `RECEIVE_BOOT_COMPLETED` y `WAKE_LOCK` — para que WorkManager
  reprograme el chequeo tras un reinicio.

---

## Roadmap (preparado en la arquitectura)

La separación en capas y los módulos de DI están pensados para crecer hacia:

- **Firebase Auth** para login multi-dispositivo.
- **Cloud sync** (Firestore/Backend propio).
- **Múltiples sitios** (basta con agregar un nuevo `XxxScraper` y registrarlo en Hilt).
- **IA semántica**: el matcher actual puede convivir con un `EmbeddingMatcher`.
- **Filtros avanzados** por categoría/fecha/cargo.

---

## Test plan rápido

1. Compilar e instalar en un dispositivo / emulador (API 24+).
2. Abrir la app: se siembran keywords por defecto (terapista ocupacional, EOE, SAIE, …).
3. Ir a **Ajustes → "Sincronizar ahora"** y esperar 10–30 s.
4. Volver a **Home**: aparecerán los concursos relevantes (si los hay).
5. Tocar uno: se ve el detalle. FAB → abre la URL original en el navegador.
6. En **Keywords**, agregar `"psicopedagogo"` y volver a sincronizar; los
   matches anteriores se **re-evalúan automáticamente**.
7. Cerrar la app: el `PeriodicWorkRequest` sigue ejecutándose en background.

---

## Notas / Limitaciones conocidas

- **Intervalo mínimo real**: 15 minutos (impuesto por WorkManager).
- El sitio del CGE no expone API JSON, por lo que cualquier cambio profundo en
  el HTML puede requerir ajustar los selectores de `CgeScraper`. El scraper
  es defensivo y registra warnings con Timber cuando algo no resuelve.
- El esquema de DB es `version = 1` con `fallbackToDestructiveMigration`.
  Para producción, agregar migraciones formales antes de publicar v2.
