package com.example.data.model

enum class ResolutionOption(
    val label: String,
    val width: Int,
    val height: Int,
    val is4K: Boolean = false
) {
    RES_4K("4K UHD (3840×2160)", 3840, 2160, true),
    RES_2K("2K QHD (2560×1440)", 2560, 1440, false),
    RES_1080P("1080p FHD (1920×1080)", 1920, 1080, false),
    RES_720P("720p HD (1280×720)", 1280, 720, false)
}

enum class FpsOption(
    val fps: Int,
    val label: String,
    val isHighRefresh: Boolean = false
) {
    FPS_144(144, "144 FPS (Ultra Gaming)", true),
    FPS_120(120, "120 FPS (Competitive)", true),
    FPS_90(90, "90 FPS (Smooth)", true),
    FPS_60(60, "60 FPS (Standard)", false)
}

enum class CodecOption(
    val mimeType: String,
    val label: String,
    val compressionSavings: String
) {
    HEVC("video/hevc", "HEVC / H.265 (HW Accelerated)", "55% smaller file size"),
    AV1("video/av01", "AV1 (Next-Gen CRF)", "65% smaller file size"),
    AVC("video/avc", "AVC / H.264 (Universal)", "Standard file size")
}

enum class BitrateOption(
    val label: String,
    val bps: Int,
    val description: String
) {
    VBR_MINIMAL("Adaptive Minimal Size (VBR)", 15_000_000, "Intelligent bit allocation for tiny file footprint"),
    BALANCED_25("Balanced (25 Mbps)", 25_000_000, "Recommended balance for Snapdragon 685"),
    HIGH_50("High Quality (50 Mbps)", 50_000_000, "Crisp details for high-motion gaming"),
    ULTRA_100("Ultra Master (100 Mbps)", 100_000_000, "Lossless esports clarity")
}

enum class AudioSourceOption(
    val label: String,
    val description: String
) {
    INTERNAL_AND_MIC("Internal Game + Voice", "Mixes device game audio with microphone commentary"),
    INTERNAL_ONLY("Internal Game Audio", "Clean gameplay sounds without background noise"),
    MIC_ONLY("Microphone Voice Only", "Captures voice and team chat through mic"),
    MUTE("No Audio (Mute)", "Silent high-speed video track")
}

enum class Dlss5Mode(
    val label: String,
    val shortBadge: String,
    val description: String
) {
    POST_PROCESS(
        "Post-Process Neural DLSS 5 (Recommended)",
        "DLSS 5 Post",
        "Records at zero touch lag. Upon saving, DLSS 5 AI transforms the clip into 4K 144FPS with neural edge sharpening & frame gen."
    ),
    REALTIME_FALLBACK(
        "Smart Dynamic DLSS 5 (Auto-Fallback)",
        "DLSS 5 Smart",
        "Attempts real-time AI enhancement; if any frame drops or thermals spike, automatically shifts to post-save mode with 0 lag."
    ),
    REALTIME_FORCE(
        "Real-Time DLSS 5 Shader",
        "DLSS 5 Live",
        "Applies real-time GPU super-resolution during active capture."
    ),
    OFF(
        "DLSS 5 Disabled",
        "Raw Mode",
        "Standard direct video recording without AI super-sampling."
    )
}

enum class SnapdragonProfile(
    val label: String,
    val description: String,
    val touchOptimization: Boolean,
    val thermalGuard: Boolean
) {
    REDMI15_ESPORTS(
        "Redmi 15 / SD685 Esports",
        "Optimized for Snapdragon 685 (Kryo 265 + Adreno 610). Zero touch delay, anti-throttling active.",
        touchOptimization = true,
        thermalGuard = true
    ),
    BALANCED_EFFICIENCY(
        "Balanced Efficiency",
        "Keeps phone cool during marathon sessions with optimal HEVC compression.",
        touchOptimization = true,
        thermalGuard = true
    ),
    MAX_PERFORMANCE(
        "Max 4K 144FPS Unlocked",
        "Pushes encoder limits with full hardware allocation and DLSS 5 super-resolution.",
        touchOptimization = true,
        thermalGuard = false
    )
}

data class RecorderSettings(
    val resolution: ResolutionOption = ResolutionOption.RES_4K,
    val fps: FpsOption = FpsOption.FPS_144,
    val codec: CodecOption = CodecOption.HEVC,
    val bitrate: BitrateOption = BitrateOption.VBR_MINIMAL,
    val audioSource: AudioSourceOption = AudioSourceOption.INTERNAL_AND_MIC,
    val dlss5Mode: Dlss5Mode = Dlss5Mode.POST_PROCESS,
    val snapdragonProfile: SnapdragonProfile = SnapdragonProfile.REDMI15_ESPORTS,
    val zeroTouchLagMode: Boolean = true,
    val thermalThrottlingGuard: Boolean = true,
    val showFloatingHud: Boolean = true,
    val countdownSeconds: Int = 3,
    val shakeToStop: Boolean = true,
    val dlss5SharpnessLevel: Float = 0.85f,
    val dlss5FrameGenEnabled: Boolean = true
)
