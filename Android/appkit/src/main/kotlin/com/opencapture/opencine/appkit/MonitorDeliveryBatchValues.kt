package com.opencapture.opencine.appkit

import com.opencapture.opencine.media.MediaTransferRate
import kotlin.math.roundToInt

public enum class MonitorDeliveryDestination {
    NATIVE_SHARE, REMOTE_UPLOAD;
    public fun title(remoteDisplayName: String?): String = if (this == NATIVE_SHARE) "分享" else remoteDisplayName ?: "上传"
    public fun subtitle(remoteDisplayName: String?, nativeShareSubtitle: String = "分享至其他应用"): String =
        if (this == NATIVE_SHARE) nativeShareSubtitle else "上传到${remoteDisplayName ?: "远程"}项目"
}
public enum class MonitorDeliveryPostExportAction { SYSTEM_SHARE, SAVE_TO_PHOTOS }
public data class MonitorDeliveryExportConfiguration<C, F>(
    public val logTransform: C, public val exportFormat: F,
    public val bakeLUT: Boolean = true, public val bakeLUTExposure: Boolean = true,
    public val convertLog: Boolean = false, public val includeMetadata: Boolean = true,
    public val forceReupload: Boolean = false,
)
public data class MonitorDeliveryBatchProgress(
    public val destination: MonitorDeliveryDestination, public val totalClips: Int,
    public val clipIndex: Int, public val clipFraction: Double,
    public val isCaching: Boolean = false, public val isSwitchingNetworks: Boolean = false,
    public val filename: String? = null, public val uploadCaption: String? = null, public val stage: Stage = Stage.PREPARING,
    /** The camera download's speed while caching ("42 MB/s", "Stalled"); null hides it. */
    public val transferRate: String? = null,
) {
    public enum class Stage { PREPARING, CACHING, EXPORTING, UPLOADING, SAVING, SWITCHING_NETWORKS, AWAITING_PHOTOS_ACCESS }
    public val overallFraction: Double get() = if (totalClips > 0)
        ((maxOf(0, clipIndex - 1).toDouble() + clipFraction) / totalClips).coerceAtMost(1.0) else 0.0
    public val isPreparingClip: Boolean get() = clipFraction <= 0.0
    public val showsBusySpinner: Boolean get() = isPreparingClip || isSwitchingNetworks || stage == Stage.SAVING || stage == Stage.AWAITING_PHOTOS_ACCESS
    public val percentText: String get() = if (isPreparingClip) "" else "${(overallFraction * 100).roundToInt()}%"
    public fun statusLine(uploadCaption: String? = null): String {
        if (isSwitchingNetworks) return "正在切换网络…"
        if (stage == Stage.AWAITING_PHOTOS_ACCESS) return "正在等待相册访问权限…"
        if (stage == Stage.SAVING) return "正在存入相册…"
        val verb = if (isCaching) {
            if (isPreparingClip) "正在从相机缓存素材…" else "从相机缓存素材"
        } else if (destination == MonitorDeliveryDestination.NATIVE_SHARE) {
            if (clipFraction > 0 && clipFraction < 0.95) "正在导出" else if (isPreparingClip) "正在准备…" else "正在准备分享"
        } else {
            if (clipFraction > 0 && clipFraction < 0.45) "正在导出" else if (isPreparingClip) "正在准备…" else uploadCaption ?: this.uploadCaption ?: "正在上传"
        }
        // Before the first byte the only reading is Stalled: it still shows.
        if (isCaching && isPreparingClip && transferRate != null) return "正在从相机缓存素材 · ${transferRate}"
        if (isPreparingClip) return verb
        if (isCaching && transferRate != null) return "$verb $percentText · $transferRate"
        return "$verb $percentText"
    }
}
public sealed interface MonitorDeliveryBatchOutcome<A> {
    public data class Share<A>(public val urls: List<A>, public val metadataText: String?, public val stagingCleanupURL: A?) : MonitorDeliveryBatchOutcome<A>
    public data class SavedToPhotos<A>(public val count: Int) : MonitorDeliveryBatchOutcome<A>
    public data class Uploaded<A>(public val summary: String) : MonitorDeliveryBatchOutcome<A>
    public data class Failed<A>(public val message: String) : MonitorDeliveryBatchOutcome<A>
}
public class MonitorDeliveryAdmission(private val current: () -> Boolean) {
    public fun isCurrent(): Boolean = current()
}
public class MonitorDeliveryOriginalService<R>(
    public val available: (R) -> Boolean, public val acquire: suspend (R) -> Unit, public val progress: (R) -> Double?,
    /** Whether the last acquisition of an uncached record ended because the camera stopped answering. */
    public val cameraStoppedResponding: (R) -> Boolean = { false },
    /** The running acquisition's download speed, read with its progress; null shows none. */
    public val rate: (R) -> MediaTransferRate? = { null },
)
public class MonitorDeliveryExportService<R, A>(
    public val release: suspend (A) -> Unit, public val prepare: suspend (R, (Double) -> Unit) -> A?,
) {
    public constructor(prepare: suspend (R, (Double) -> Unit) -> A?) : this({}, prepare)
}
public data class MonitorDeliveryShareFiles<A>(public val urls: List<A>, public val metadataText: String? = null, public val cleanup: A? = null)
/** One OS insert per item, so a rejected item never discards the others (A2-56). */
public class MonitorDeliveryPhotosService<A>(public val save: suspend (A) -> Boolean)
/** Existing OS authorization, admitted before cache or export work. */
public class MonitorDeliveryPhotosAuthorizationService(
    public val isPending: () -> Boolean, public val authorize: suspend () -> Unit,
)
/**
 * The remote's record of confirmed uploads (A2-53): a record already uploaded is skipped before
 * any cache or export work unless the operator asks to re-upload; every confirmed upload is
 * recorded.
 */
public class MonitorDeliveryUploadHistory<R>(public val wasUploaded: (R) -> Boolean, public val recordUploaded: (R) -> Unit)
public class MonitorDeliveryUploadService<A>(public val progressCaption: String = "正在上传", public val upload: suspend (A, (Double) -> Unit) -> Unit)
public class MonitorDeliveryInternetService<H>(
    /** A new route lease, or null when another hop holds the route (never stolen). */
    public val needsHop: () -> Boolean, public val begin: () -> H?, public val owns: (H) -> Boolean,
    public val wait: suspend (H) -> Boolean, public val restoreEntered: suspend (H) -> Unit,
    public val restore: (H) -> Unit,
) {
    public constructor(needsHop: () -> Boolean, begin: () -> H?, owns: (H) -> Boolean,
        wait: suspend (H) -> Boolean, restore: (H) -> Unit) :
        this(needsHop, begin, owns, wait, { restore(it) }, restore)
}
public data class MonitorDeliveryBatchCounts(
    public val prepared: Int, public val uploaded: Int, public val skipped: Int,
    public val failed: Int, public val uncached: Int,
)
/**
 * The one batch policy every app runs (A2-56): a failing item never stops the others, Photos
 * access is asked before any cache or export work, and the outcome counts every item.
 */
public object MonitorDeliveryBatchPolicy {
    public fun cacheFailure(count: Int): String = "未能从相机缓存 ${count} 个片段。请检查连接后重试。"
    /** The link died mid-transfer: the camera stopped answering while caching. */
    public fun cameraStoppedResponding(count: Int): String =
        "缓存 ${count} 个片段时相机无响应。请重新连接后重试。"
    public const val EMPTY_SELECTION: String = "请至少选择一个片段。"
    public const val INTERNET_FAILURE: String =
        "断开相机 Wi-Fi 后未能联网。请检查移动数据或其他 Wi-Fi 后重试。"
    /** The note shared beside a partial native share, or null when every item was prepared. */
    public fun nativeSummary(counts: MonitorDeliveryBatchCounts): String? = buildList {
        if (counts.failed > 0) add("已准备 ${counts.prepared} 项，${counts.failed} 项失败。")
        if (counts.uncached > 0) add("有 ${counts.uncached} 个片段未能从相机缓存。")
    }.joinToString(" ").ifEmpty { null }
    /** A Photos save that saved nothing or rejected an item fails with its exact counts. */
    public fun photosFailure(saved: Int, failed: Int): String? =
        if (saved == 0 || failed > 0) "已保存 ${saved} 项，${failed} 项未能存入相册。" else null
}

public fun <A> MonitorDeliveryBatchOutcome<A>.completion(): MonitorDeliveryCompletion<MonitorDeliveryBatchOutcome<A>, MonitorDeliveryShareFiles<A>> = when (this) {
    is MonitorDeliveryBatchOutcome.Share -> MonitorDeliveryCompletion(this, share = MonitorDeliveryShareFiles(urls, metadataText, stagingCleanupURL))
    is MonitorDeliveryBatchOutcome.SavedToPhotos -> MonitorDeliveryCompletion(this, toast = "已将 ${count} 个片段存入相册")
    is MonitorDeliveryBatchOutcome.Uploaded -> MonitorDeliveryCompletion(this, toast = summary)
    is MonitorDeliveryBatchOutcome.Failed -> MonitorDeliveryCompletion(this, toast = message)
}
