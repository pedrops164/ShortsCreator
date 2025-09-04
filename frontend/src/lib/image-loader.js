'use client';
export default function myImageLoader({ src, width, quality }) {
  const cdnUrl = process.env.NEXT_PUBLIC_ASSET_CDN_URL;
  if (!cdnUrl) {
    return src;
  }
  return `${cdnUrl}${src}?w=${width}&q=${quality || 75}`;
}