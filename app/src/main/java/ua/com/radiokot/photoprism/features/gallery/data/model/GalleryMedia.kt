package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismPhoto
import ua.com.radiokot.photoprism.features.gallery.logic.MediaThumbnailUrlFactory
import java.util.*

class GalleryMedia(
    val media: MediaType,
    val hash: String,
    val width: Int,
    val height: Int,
    val takenAt: Date,
    val name: String,
    val smallThumbnailUrl: String,
) {
    constructor(
        source: PhotoPrismPhoto,
        thumbnailUrlFactory: MediaThumbnailUrlFactory,
    ) : this(
        media = MediaType.fromPhotoPrism(source.type),
        hash = source.hash,
        width = source.width,
        height = source.height,
        takenAt = photoPrismDateFormat.parse(source.takenAt)!!,
        name = source.name,
        smallThumbnailUrl = thumbnailUrlFactory.getSmallThumbnailUrl(source.hash),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryMedia

        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    override fun toString(): String {
        return "GalleryMedia(hash='$hash', kind=$media)"
    }


    /**
    photoprism/pkg/media/types.go

    const (
    Unknown  Type = ""
    Image    Type = "image"
    Raw      Type = "raw"
    Animated Type = "animated"
    Live     Type = "live"
    Video    Type = "video"
    Vector   Type = "vector"
    Sidecar  Type = "sidecar"
    Text     Type = "text"
    Other    Type = "other"
    )
     */
    sealed interface MediaType {
        object Unknown : MediaType
        object Image : MediaType
        object Raw : MediaType
        object Animated : MediaType
        object Live : MediaType
        object Video : MediaType
        object Vector : MediaType
        object Sidecar : MediaType
        object Text : MediaType
        object Other : MediaType

        companion object {
            fun fromPhotoPrism(type: String): MediaType =
                when (type) {
                    "" -> Unknown
                    "image" -> Image
                    "raw" -> Raw
                    "animated" -> Animated
                    "live" -> Live
                    "video" -> Video
                    "vector" -> Vector
                    "sidecar" -> Sidecar
                    "text" -> Text
                    "other" -> Other
                    else ->
                        throw IllegalStateException("Unsupported PhotoPrism media type '$type'")
                }
        }
    }
}