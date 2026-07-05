// Matches most YouTube URL shapes (watch?v=, youtu.be/, embed/, etc.) - the
// video ID is always the 11-character group this regex isolates. Shared by
// HighlightCard.vue and ProjectCard.vue, both of which embed a YouTube demo
// video the same way.
const YOUTUBE_ID_REGEX = /^.*(?:youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/

export function getYoutubeEmbedId(videoUrl: string | undefined): string {
  const match = videoUrl?.match(YOUTUBE_ID_REGEX)
  return match?.[1]?.length === 11 ? match[1] : ''
}
