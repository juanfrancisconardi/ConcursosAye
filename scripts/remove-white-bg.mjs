/**
 * Makes border-connected near-white pixels transparent (keeps white inside emblem).
 */
import sharp from "sharp";
import { readFileSync, writeFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));

function isBackgroundPixel(r, g, b, a) {
  if (a < 16) return true;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const lum = 0.299 * r + 0.587 * g + 0.114 * b;
  const sat = max - min;
  // Light, low-saturation pixels (white/gray padding), not red/green emblem colors.
  return lum >= 198 && sat <= 38;
}

function floodTransparent(data, width, height, channels) {
  const size = width * height;
  const visited = new Uint8Array(size);
  const queue = [];

  const pushIfBg = (x, y) => {
    const i = y * width + x;
    if (visited[i]) return;
    const o = i * channels;
    const r = data[o];
    const g = data[o + 1];
    const b = data[o + 2];
    const a = channels === 4 ? data[o + 3] : 255;
    if (!isBackgroundPixel(r, g, b, a)) return;
    visited[i] = 1;
    queue.push(i);
  };

  for (let x = 0; x < width; x++) {
    pushIfBg(x, 0);
    pushIfBg(x, height - 1);
  }
  for (let y = 0; y < height; y++) {
    pushIfBg(0, y);
    pushIfBg(width - 1, y);
  }

  while (queue.length) {
    const i = queue.pop();
    const x = i % width;
    const y = (i / width) | 0;
    if (x > 0) pushIfBg(x - 1, y);
    if (x < width - 1) pushIfBg(x + 1, y);
    if (y > 0) pushIfBg(x, y - 1);
    if (y < height - 1) pushIfBg(x, y + 1);
  }

  for (let i = 0; i < size; i++) {
    if (visited[i] && channels === 4) {
      data[i * channels + 3] = 0;
    }
  }
}

async function processFile(inputPath, outputPath) {
  const { data, info } = await sharp(inputPath)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });

  floodTransparent(data, info.width, info.height, info.channels);

  await sharp(data, {
    raw: {
      width: info.width,
      height: info.height,
      channels: info.channels,
    },
  })
    .png({ compressionLevel: 9 })
    .toFile(outputPath);

  console.log(`OK ${outputPath} (${info.width}x${info.height})`);
}

const args = process.argv.slice(2);
const inputs =
  args.length > 0
    ? args
    : [
        resolve(__dirname, "../app/src/main/res/drawable-nodpi/ic_launcher_fg.png"),
      ];

for (const input of inputs) {
  await processFile(input, input);
}
