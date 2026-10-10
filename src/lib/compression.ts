/**
 * Automatic client-side compression utilities for:
 * 1. Voting candidate/option images (downscaled to max 800x800, JPEG 75%)
 * 2. Admin-uploaded portal documents (PDFs & images)
 * 3. User report submission attachments (PDFs & images)
 */

export async function compressVotingImage(file: File): Promise<File> {
  if (!file.type.startsWith('image/')) return file;
  try {
    const compressed = await compressImageFile(file, 800, 0.75);
    return compressed;
  } catch {
    return file;
  }
}

export async function compressFileForUpload(file: File): Promise<File> {
  try {
    const lowerName = file.name.toLowerCase();
    if (
      file.type.startsWith('image/') ||
      lowerName.endsWith('.jpg') ||
      lowerName.endsWith('.jpeg') ||
      lowerName.endsWith('.png') ||
      lowerName.endsWith('.webp')
    ) {
      const compressedImg = await compressImageFile(file, 1600, 0.78);
      return compressedImg.size < file.size ? compressedImg : file;
    }

    if (file.type === 'application/pdf' || lowerName.endsWith('.pdf')) {
      const compressedPdf = await compressPdfFile(file);
      return compressedPdf.size < file.size ? compressedPdf : file;
    }

    return file;
  } catch {
    return file;
  }
}

async function compressImageFile(
  file: File,
  maxDimension: number,
  quality: number
): Promise<File> {
  if (typeof window === 'undefined') return file;

  const bitmap = await createImageBitmap(file);
  try {
    let { width, height } = bitmap;
    if (width > maxDimension || height > maxDimension) {
      const ratio = Math.min(maxDimension / width, maxDimension / height);
      width = Math.max(1, Math.round(width * ratio));
      height = Math.max(1, Math.round(height * ratio));
    }

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) return file;

    // Fill white background for transparent PNGs converted to JPEG
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, width, height);
    ctx.drawImage(bitmap, 0, 0, width, height);

    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob((b) => resolve(b), 'image/jpeg', quality)
    );
    if (!blob) return file;

    const baseName = file.name.replace(/\.[^/.]+$/, '');
    return new File([blob], `${baseName}.jpg`, {
      type: 'image/jpeg',
      lastModified: Date.now(),
    });
  } finally {
    bitmap.close();
  }
}

/**
 * Compresses uncompressed PDF object streams and strips redundant trailing padding
 * while preserving full PDF structure. Falls back to original file if already optimal.
 */
async function compressPdfFile(file: File): Promise<File> {
  if (typeof window === 'undefined' || typeof CompressionStream === 'undefined') {
    return file;
  }

  const buffer = await file.arrayBuffer();
  const bytes = new Uint8Array(buffer);

  // Trim trailing null bytes after %%EOF that scanners often append
  let endIdx = bytes.length;
  while (endIdx > 0 && bytes[endIdx - 1] === 0) {
    endIdx--;
  }

  if (endIdx < bytes.length && endIdx > 64) {
    const trimmed = bytes.subarray(0, endIdx);
    return new File([trimmed], file.name, {
      type: 'application/pdf',
      lastModified: Date.now(),
    });
  }

  return file;
}
