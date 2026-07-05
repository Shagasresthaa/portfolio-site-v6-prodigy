export interface ProcessedImage {
  image: string
  thumbnailImage: string
}

// Long-edge caps, not exact dimensions - aspect ratio is preserved.
const FULL_MAX_DIMENSION = 1600
const THUMBNAIL_MAX_DIMENSION = 480
const FULL_QUALITY = 0.82
const THUMBNAIL_QUALITY = 0.75

function resizeToCanvas(bitmap: ImageBitmap, maxDimension: number): HTMLCanvasElement {
  const scale = Math.min(1, maxDimension / Math.max(bitmap.width, bitmap.height))
  const width = Math.round(bitmap.width * scale)
  const height = Math.round(bitmap.height * scale)

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('Canvas 2D context is not available.')
  ctx.drawImage(bitmap, 0, 0, width, height)
  return canvas
}

function canvasToWebpDataUrl(canvas: HTMLCanvasElement, quality: number): Promise<string> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error('Failed to encode image.'))
          return
        }
        const reader = new FileReader()
        reader.onloadend = () => resolve(reader.result as string)
        reader.onerror = () => reject(new Error('Failed to read encoded image.'))
        reader.readAsDataURL(blob)
      },
      'image/webp',
      quality,
    )
  })
}

/**
 * Converts a single uploaded image into the two sizes HighlightItem needs
 * (src/types/highlight.ts) - a full-size version for the modal and a smaller
 * thumbnail for the grid card - re-encoding both as WebP along the way. The
 * old site stored uploaded images as-is (see MomentForm.tsx in
 * portfolio-site-v5-prime), which is exactly the "images as-is" mistake this
 * is meant to correct. There's no backend yet to do this server-side, so it
 * happens here in the browser via Canvas instead - swap for a real upload
 * endpoint (that presumably does the same resizing/re-encoding, or better,
 * server-side) once one exists.
 *
 * Note: canvas.toBlob('image/webp') silently falls back to PNG on browsers
 * that can't encode WebP (older Safari) - fine for this mock, but worth
 * knowing if an exported image looks larger than expected.
 */
export async function processImageUpload(file: File): Promise<ProcessedImage> {
  if (!file.type.startsWith('image/')) {
    throw new Error('Please choose an image file.')
  }

  const bitmap = await createImageBitmap(file)
  try {
    const [image, thumbnailImage] = await Promise.all([
      canvasToWebpDataUrl(resizeToCanvas(bitmap, FULL_MAX_DIMENSION), FULL_QUALITY),
      canvasToWebpDataUrl(resizeToCanvas(bitmap, THUMBNAIL_MAX_DIMENSION), THUMBNAIL_QUALITY),
    ])
    return { image, thumbnailImage }
  } finally {
    bitmap.close()
  }
}
