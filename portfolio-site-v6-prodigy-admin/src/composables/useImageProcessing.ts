export interface ProcessedImage {
  image: Blob
  thumbnailImage: Blob
}

// Long-edge caps, not exact dimensions - aspect ratio is preserved.
const FULL_MAX_DIMENSION = 2048
const THUMBNAIL_MAX_DIMENSION = 480
const FULL_QUALITY = 0.9
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

// Falls back to PNG on browsers that can't encode WebP (older Safari) - fine, just worth
// knowing if an uploaded image ends up larger than expected.
function canvasToWebpBlob(canvas: HTMLCanvasElement, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('Failed to encode image.'))),
      'image/webp',
      quality,
    )
  })
}

/**
 * Converts a single uploaded image into the two sizes HighlightItem needs
 * (src/types/highlight.ts) - a full-size version for the modal and a smaller
 * thumbnail for the grid card - re-encoding both as WebP along the way. Returns
 * Blobs (not data URLs) since these get uploaded to the real image-upload
 * endpoint - build an object URL via URL.createObjectURL for local preview.
 */
export async function processImageUpload(file: File): Promise<ProcessedImage> {
  if (!file.type.startsWith('image/')) {
    throw new Error('Please choose an image file.')
  }

  const bitmap = await createImageBitmap(file)
  try {
    const [image, thumbnailImage] = await Promise.all([
      canvasToWebpBlob(resizeToCanvas(bitmap, FULL_MAX_DIMENSION), FULL_QUALITY),
      canvasToWebpBlob(resizeToCanvas(bitmap, THUMBNAIL_MAX_DIMENSION), THUMBNAIL_QUALITY),
    ])
    return { image, thumbnailImage }
  } finally {
    bitmap.close()
  }
}

/**
 * Same conversion, but for entities with only one image field (Project -
 * see src/types/project.ts, which has no separate thumbnail) - full-size,
 * re-encoded as WebP, nothing else. Returns a Blob (not a data URL) since this
 * gets uploaded to the real image-upload endpoint - build an object URL via
 * URL.createObjectURL for local preview.
 */
export async function processSingleImageUpload(file: File): Promise<Blob> {
  if (!file.type.startsWith('image/')) {
    throw new Error('Please choose an image file.')
  }

  const bitmap = await createImageBitmap(file)
  try {
    return await canvasToWebpBlob(resizeToCanvas(bitmap, FULL_MAX_DIMENSION), FULL_QUALITY)
  } finally {
    bitmap.close()
  }
}
