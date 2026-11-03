package com.opencapture.opencine.cameracontrols

import com.opencapture.opencine.vocabulary.MediaClipFormatting
import com.opencapture.opencine.vocabulary.MediaFile
import com.opencapture.opencine.vocabulary.MediaKind
import com.opencapture.opencine.vocabulary.MediaLibraryTab

enum class MediaBrowserLayout {
    GRID,
    LIST,
}

enum class MediaThumbnailSize {
    SMALL,
    MEDIUM,
    LARGE,
    ;

    val gridMinimumDp: Int
        get() =
            when (this) {
                SMALL -> 148
                MEDIUM -> 210
                LARGE -> 280
            }

    val gridMaximumDp: Int
        get() =
            when (this) {
                SMALL -> 200
                MEDIUM -> 300
                LARGE -> 380
            }

    val gridIconSizeDp: Int
        get() =
            when (this) {
                SMALL -> 9
                MEDIUM -> 12
                LARGE -> 15
            }

    val accessibilityLabel: String
        get() =
            when (this) {
                SMALL -> "小缩略图"
                MEDIUM -> "中缩略图"
                LARGE -> "大缩略图"
            }
}

object MediaClipPresentation {
    fun resolutionLabel(resolution: String?): String? {
        if (resolution.isNullOrEmpty()) return null
        val width = resolution.substringBefore('x').toIntOrNull()
        return when {
            width == null -> resolution
            width >= 3840 -> "4K"
            width >= 2560 -> "2.7K"
            width >= 1920 -> "1080p"
            width >= 1280 -> "720p"
            else -> resolution
        }
    }

    fun dateLabel(dateKey: String): String {
        if (dateKey.length != 8) return dateKey
        return "${dateKey.substring(0, 4)}-${dateKey.substring(4, 6)}-${dateKey.substring(6, 8)}"
    }

    fun metadataLine(file: MediaFile, durationOverride: String? = null): String {
        val parts = ArrayList<String>(3)
        resolutionLabel(file.resolution)?.let { parts.add(it) }
        val bytes = MediaClipFormatting.byteLabel(file.sizeBytes)
        if (bytes.isNotEmpty()) parts.add(bytes)
        if (file.kind == MediaKind.VIDEO) {
            parts.add(durationOverride ?: MediaClipFormatting.durationLabel(file.durationSeconds))
        }
        return parts.joinToString(" · ")
    }
}

/** Operator-facing media-browser copy. Never name a sister app. */
object MediaLibraryCopy {
    const val FILTER_EMPTY = "当前分类没有符合筛选条件的素材。"
    const val EMPTY_ALL = "相机中暂无素材。拍摄后下拉刷新。"
    const val EMPTY_FAVORITES = "暂无收藏。为片段标星后即可在此查看。"
    const val EMPTY_VIDEOS = "相机中暂无视频。录制后下拉刷新。"
    const val EMPTY_PHOTOS = "相机中暂无照片。拍摄后下拉刷新。"
    const val DISCONNECTED = "连接相机以查看机身存储的素材。"
    const val DISCONNECTED_EMPTY_CACHE =
        "本机没有缓存。连接相机以查看机身存储的素材。"
    /**
     * Offline, the gallery shows only clips cached on this phone; the last card listing stays
     * hidden (the documented offline browse: downloaded clips only), and the gallery says so.
     */
    const val DISCONNECTED_CACHED_ONLY = "相机已断开。当前显示本机缓存的素材。"
    const val BROWSE_HINT = "轻点打开 · 长按后拖动多选"
    const val SELECTING_HINT = "滑动浏览 · 长按或横向拖动选择"
    const val READING_HINT = "正在读取相机素材…"
    const val LISTING_TITLE = "正在读取相机素材列表…"
    const val FILTER_EMPTY_TITLE = "无符合条件的素材"
    const val DISCONNECTED_TITLE = "相机已断开"
    const val NO_CACHED_TITLE = "无缓存素材"
    const val EMPTY_TITLE = "暂无素材"

    /** The gallery's hint line under its item count (Swift `MonitorMediaGalleryCopy.galleryHint`). */
    fun galleryHint(fetching: Boolean, selecting: Boolean, isLive: Boolean): String =
        when {
            fetching -> READING_HINT
            selecting -> SELECTING_HINT
            isLive -> BROWSE_HINT
            else -> DISCONNECTED_CACHED_ONLY
        }

    /**
     * The empty gallery's heading, in the same order as [emptySubtitle]: "No clips yet" only when a
     * connected camera listed nothing for this tab.
     */
    fun emptyTitle(listing: Boolean, isLive: Boolean, hasListedClips: Boolean, activeFilterCount: Int): String =
        when {
            listing -> LISTING_TITLE
            !isLive && !hasListedClips -> DISCONNECTED_TITLE
            activeFilterCount > 0 -> FILTER_EMPTY_TITLE
            !isLive -> NO_CACHED_TITLE
            else -> EMPTY_TITLE
        }
    const val PROXY_TAG = "代理文件"
    const val PROXY_HELP = "720p 预览。连接相机以分享原片。"

    fun headerTitle(category: MediaLibraryTab): String =
        when (category) {
            MediaLibraryTab.ALL -> "全部素材"
            MediaLibraryTab.VIDEOS -> "视频"
            MediaLibraryTab.PHOTOS -> "照片"
            MediaLibraryTab.FAVORITES -> "收藏"
        }

    /**
     * The empty gallery's line: offline with nothing listed, filtered (before the cache is blamed,
     * ZIB-413), offline with listed clips, or the tab's own.
     */
    fun emptySubtitle(isLive: Boolean, hasListedClips: Boolean, activeFilterCount: Int, category: MediaLibraryTab): String =
        when {
            !isLive && !hasListedClips -> DISCONNECTED
            activeFilterCount > 0 -> FILTER_EMPTY
            !isLive -> DISCONNECTED_EMPTY_CACHE
            category == MediaLibraryTab.FAVORITES -> EMPTY_FAVORITES
            category == MediaLibraryTab.VIDEOS -> EMPTY_VIDEOS
            category == MediaLibraryTab.PHOTOS -> EMPTY_PHOTOS
            else -> EMPTY_ALL
        }
}

object MediaOperatorCopy {
    const val LISTING = "正在读取相机素材列表…"
    const val NOT_CONNECTED = "连接相机以查看素材。"
    const val PLAYBACK_FAILED = "相机未进入回放模式。"
    const val BROWSING_ON_CAMERA = "相机正在回放。关闭回放即可返回实时取景。"
    const val NO_CLIPS = "相机中没有素材。"
    const val LIST_FAILED = "无法读取相机素材列表。"
    const val NOT_DELETABLE = "无法在此删除该片段。"
    const val DELETE_FAILED = "无法删除此片段。"
    const val DOWNLOAD_FAILED = "无法下载此片段。"
    const val THUMB_FAILED = "无法载入缩略图。"
    const val CLIP_OPEN_FAILED = "无法打开此片段。"
    const val CLIP_LOADING = "正在从相机载入片段…"
    const val CLIP_NOT_CACHED = "此片段尚未缓存到本机。"
}
