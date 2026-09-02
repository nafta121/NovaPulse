package com.example.core.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.feature.widget.BriefingSession

data class MusicRecommendation(
    val title: String,
    val subtitle: String,
    val genreTag: String,
    val query: String,
    val spotifyUri: String
)

object MusicRecommendationHelper {

    private const val TAG = "MusicHelper"

    val POPULAR_SPOTIFY_CHIPS = listOf(
        "V-Pop Thịnh Hành",
        "Sơn Tùng M-TP",
        "Đen Vâu",
        "Vũ.",
        "HIEUTHUHAI",
        "Indie Việt Tuyển Chọn",
        "Top Hits Vietnam",
        "Acoustic Chill",
        "Rap Việt"
    )

    fun getRecommendationForSession(hour: Int, minute: Int = 0): MusicRecommendation {
        val session = BriefingSession.fromHourAndMinute(hour, minute)
        return when (session) {
            BriefingSession.EARLY_MORNING -> MusicRecommendation(
                title = "V-Pop & Acoustic Buổi Sáng",
                subtitle = "Giai điệu tươi vui, nhẹ nhàng đón ngày mới",
                genreTag = "Spotify • Khởi đầu ngày mới",
                query = "V-Pop Morning Acoustic",
                spotifyUri = "spotify:search:V-Pop%20Acoustic"
            )
            BriefingSession.MID_MORNING -> MusicRecommendation(
                title = "Deep Focus - Nhạc Tập Trung Cao Độ",
                subtitle = "Nhạc không lời & Piano giúp bạn đạt trạng thái Flow",
                genreTag = "Spotify • Khung giờ tập trung",
                query = "Deep Focus Piano Work",
                spotifyUri = "spotify:search:Deep%20Focus%20Work"
            )
            BriefingSession.NOON -> MusicRecommendation(
                title = "Cafe Trưa Thư Thái & Acoustic",
                subtitle = "Âm nhạc nhẹ nhàng hỗ trợ tiêu hóa và thư giãn giữa giờ",
                genreTag = "Spotify • Nghỉ trưa & Thưởng thức",
                query = "Acoustic Cafe Lunch Chill",
                spotifyUri = "spotify:search:Acoustic%20Cafe"
            )
            BriefingSession.AFTERNOON -> MusicRecommendation(
                title = "V-Pop Hits & Năng Lượng Chiều",
                subtitle = "Duy trì năng lượng với các bản hit thịnh hành",
                genreTag = "Spotify • Buổi chiều năng lượng",
                query = "V-Pop Hits Afternoon",
                spotifyUri = "spotify:search:V-Pop%20Hits"
            )
            BriefingSession.EVENING -> MusicRecommendation(
                title = "Hoàng Hôn Indie & Chillout",
                subtitle = "Tận hưởng những phút giây tan làm và chuyển giao ngày",
                genreTag = "Spotify • Hoàng hôn thư giãn",
                query = "Indie Viet Sunset Chill",
                spotifyUri = "spotify:search:Indie%20Viet"
            )
            BriefingSession.NIGHT -> MusicRecommendation(
                title = "Night Unwind - Thư Giãn Tối",
                subtitle = "Ballad & Lofi thả lỏng tâm trí sau ngày làm việc",
                genreTag = "Spotify • Thư giãn buổi tối",
                query = "Night Ballad Chillout",
                spotifyUri = "spotify:search:Night%20Ballad"
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
     * Mở trực tiếp ứng dụng Spotify
     */
    fun openSpotifyApp(context: Context) {
        try {
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:home")).apply {
                setPackage("com.spotify.music")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(spotifyIntent)
        } catch (e: Exception) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Cannot launch Spotify: ${e2.message}")
            }
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
                } catch (e3: Exception) {
                    Log.e(TAG, "Cannot launch music player: ${e3.message}")
                }
            }
        }
    }
}
