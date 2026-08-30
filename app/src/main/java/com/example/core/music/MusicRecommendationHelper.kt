package com.example.core.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.feature.widget.BriefingSession
import java.time.LocalTime

data class MusicRecommendation(
    val title: String,
    val subtitle: String,
    val genreTag: String,
    val query: String,
    val spotifyUri: String
)

object MusicRecommendationHelper {

    private const val TAG = "MusicHelper"

    fun getRecommendationForSession(hour: Int, minute: Int = 0): MusicRecommendation {
        val session = BriefingSession.fromHourAndMinute(hour, minute)
        return when (session) {
            BriefingSession.EARLY_MORNING -> MusicRecommendation(
                title = "Acoustic Buổi Sáng - Tỉnh Táo",
                subtitle = "Giai điệu mộc nhẹ nhàng đón ngày mới",
                genreTag = "Spotify • Khởi đầu ngày mới",
                query = "Morning Acoustic Chill",
                spotifyUri = "spotify:search:Morning%20Acoustic"
            )
            BriefingSession.MID_MORNING -> MusicRecommendation(
                title = "Deep Focus - Nhạc Tập Trung Peak Hour",
                subtitle = "Nhạc không lời & Piano giúp bạn đạt trạng thái Flow",
                genreTag = "Spotify • Khung giờ tập trung",
                query = "Deep Focus Piano Work",
                spotifyUri = "spotify:search:Deep%20Focus%20Work"
            )
            BriefingSession.NOON -> MusicRecommendation(
                title = "Bữa Trưa Thư Thái & Acoustic",
                subtitle = "Âm nhạc nhẹ nhàng hỗ trợ tiêu hóa và thư giãn giữa giờ",
                genreTag = "Spotify • Nghỉ trưa & Thưởng thức",
                query = "Acoustic Cafe Lunch Chill",
                spotifyUri = "spotify:search:Acoustic%20Cafe"
            )
            BriefingSession.AFTERNOON -> MusicRecommendation(
                title = "Good Afternoon - Lo-Fi Chillout",
                subtitle = "Duy trì năng lượng chiều mát mẻ với nhịp điệu Lo-fi",
                genreTag = "Spotify • Buổi chiều năng lượng",
                query = "Lofi Beats Afternoon Focus",
                spotifyUri = "spotify:search:Afternoon%20Lofi"
            )
            BriefingSession.EVENING -> MusicRecommendation(
                title = "Hoàng Hôn Acoustic & Chillout",
                subtitle = "Tận hưởng những phút giây tan làm và chuyển giao ngày",
                genreTag = "Spotify • Hoàng hôn thư giãn",
                query = "Sunset Chillout Acoustic",
                spotifyUri = "spotify:search:Sunset%20Chill"
            )
            BriefingSession.NIGHT -> MusicRecommendation(
                title = "Night Unwind - Thư Giãn Tối",
                subtitle = "Lofi & Smooth Jazz thả lỏng tâm trí sau ngày làm việc",
                genreTag = "Spotify • Thư giãn buổi tối",
                query = "Night Lofi Smooth Jazz",
                spotifyUri = "spotify:search:Night%20Unwind"
            )
            BriefingSession.LATE_NIGHT -> MusicRecommendation(
                title = "Late Night - Piano Giấc Ngủ Sâu",
                subtitle = "Giai điệu ru êm dịu dẫn lối vào giấc ngủ phục hồi",
                genreTag = "Spotify • Ngủ ngon & Phục hồi",
                query = "Night Relaxing Piano Sleep Music",
                spotifyUri = "spotify:search:Night%20Relaxing%20Piano"
            )
        }
    }

    /**
     * Mở ứng dụng nghe nhạc với Deep Link Intent (Spotify -> YouTube App -> Web YouTube)
     */
    fun openMusicApp(context: Context, query: String, spotifyUri: String? = null) {
        try {
            // Ưu tiên 1: Mở ứng dụng Spotify
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri ?: "spotify:search:$query")).apply {
                setPackage("com.spotify.music")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(spotifyIntent)
            Log.d(TAG, "Launched Spotify via Deep Link")
        } catch (e: Exception) {
            try {
                // Ưu tiên 2: Mở ứng dụng YouTube
                val youtubeIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(youtubeIntent)
                Log.d(TAG, "Launched YouTube App via Intent")
            } catch (e2: Exception) {
                try {
                    // Fallback 3: Mở trình duyệt Web tìm kiếm YouTube
                    val webUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(query)
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                    Log.d(TAG, "Launched YouTube Web Fallback")
                } catch (e3: Exception) {
                    Log.e(TAG, "Cannot launch music player: ${e3.message}")
                }
            }
        }
    }
}
