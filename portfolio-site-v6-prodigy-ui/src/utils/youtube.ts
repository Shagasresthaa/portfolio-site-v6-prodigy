// Matches watch?v=, youtu.be/, embed/, etc. - isolates the 11-char video ID.
const YOUTUBE_ID_REGEX = /^.*(?:youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/

export function getYoutubeEmbedId(videoUrl: string | undefined): string {
  const match = videoUrl?.match(YOUTUBE_ID_REGEX)
  return match?.[1]?.length === 11 ? match[1] : ''
}
