/**
 * Genera ic_launcher_fg.png y play-store-assets/icon-512.png desde icono.png (raíz).
 */
import sharp from "sharp";
import { mkdirSync } from "fs";
import { dirname, join } from "path";
import { fileURLToPath } from "url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const SRC = join(ROOT, "icono.png");
const BG = { r: 0, g: 0, b: 0, alpha: 1 };

async function squareIcon(size, outPath, inset = 0.12) {
  const inner = Math.round(size * (1 - inset * 2));
  const fg = await sharp(SRC)
    .resize(inner, inner, { fit: "contain", background: BG })
    .png()
    .toBuffer();

  await sharp({
    create: {
      width: size,
      height: size,
      channels: 4,
      background: BG,
    },
  })
    .composite([{ input: fg, gravity: "center" }])
    .png()
    .toFile(outPath);

  console.log("OK", outPath, `${size}x${size}`);
}

mkdirSync(join(ROOT, "play-store-assets"), { recursive: true });
mkdirSync(join(ROOT, "app/src/main/res/drawable-nodpi"), { recursive: true });

// Foreground del adaptive icon (alta resolución, fondo negro como icono.png)
await squareIcon(512, join(ROOT, "app/src/main/res/drawable-nodpi/ic_launcher_fg.png"), 0.1);
// Icono de alta resolución para Google Play Console
await squareIcon(512, join(ROOT, "play-store-assets/icon-512.png"), 0.08);
