/** Extracts a YouTube video ID (watch/youtu.be/embed/shorts) for the grid's thumbnail - decorative only, not used for playback. */
export function getYoutubeThumbnailUrl(videoUrl: string): string | null {
  try {
    const url = new URL(videoUrl)
    let videoId: string | null = null

    if (url.hostname === 'youtu.be') {
      videoId = url.pathname.slice(1)
    } else if (url.hostname === 'youtube.com' || url.hostname === 'www.youtube.com' || url.hostname.endsWith('.youtube.com')) {
      if (url.pathname === '/watch') {
        videoId = url.searchParams.get('v')
      } else if (url.pathname.startsWith('/embed/') || url.pathname.startsWith('/shorts/')) {
        videoId = url.pathname.split('/')[2] ?? null
      }
    }

    return videoId ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : null
  } catch {
    return null
  }
}
