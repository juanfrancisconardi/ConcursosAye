# Activar GitHub Pages (política de privacidad)

La URL pública que necesitás en **Google Play** y en la app quedará así:

**https://juanfrancisconardi.github.io/ConcursosAye/privacy.html**

(Página principal: https://juanfrancisconardi.github.io/ConcursosAye/)

## Pasos (una sola vez)

1. Entrá al repositorio en GitHub:  
   https://github.com/juanfrancisconardi/ConcursosAye

2. **Settings** → menú izquierdo **Pages**.

3. En **Build and deployment** → **Source**:
   - Elegí **Deploy from a branch**.

4. En **Branch**:
   - Rama: **main**
   - Carpeta: **/docs**
   - Clic en **Save**.

5. Esperá 1–3 minutos. Recargá la página de Settings → Pages hasta ver:
   > Your site is live at https://juanfrancisconardi.github.io/ConcursosAye/

6. Abrí en el navegador:
   - https://juanfrancisconardi.github.io/ConcursosAye/privacy.html  
   Debe verse la política de privacidad.

## Ya configurado en el proyecto

En `gradle.properties` está:

```properties
PRIVACY_POLICY_URL=https://juanfrancisconardi.github.io/ConcursosAye/privacy.html
```

Recompilá la app para que **Ajustes → Política de privacidad** abra esa URL.

## Play Console

En la ficha de la app, campo **Política de privacidad**, pegá la misma URL:

`https://juanfrancisconardi.github.io/ConcursosAye/privacy.html`

## Si cambiás de usuario o repo

La URL sigue el patrón `https://<usuario>.github.io/<nombre-repo>/privacy.html`.  
Actualizá `PRIVACY_POLICY_URL` en `gradle.properties` y el enlace en Play Console.
