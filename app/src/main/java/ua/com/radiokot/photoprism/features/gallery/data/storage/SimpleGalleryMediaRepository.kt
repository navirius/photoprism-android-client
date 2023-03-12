package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.model.PagingOrder
import ua.com.radiokot.photoprism.base.data.storage.SimplePagedDataRepository
import ua.com.radiokot.photoprism.extension.mapSuccessful
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.logic.MediaThumbnailUrlFactory

class SimpleGalleryMediaRepository(
    private val photoPrismPhotosService: PhotoPrismPhotosService,
    private val thumbnailUrlFactory: MediaThumbnailUrlFactory,
    pageLimit: Int,
) : SimplePagedDataRepository<GalleryMedia>(
    pagingOrder = PagingOrder.DESC,
    pageLimit = pageLimit,
) {
    override fun getPage(
        limit: Int,
        cursor: String?,
        order: PagingOrder
    ): Single<DataPage<GalleryMedia>> {
        val offset = cursor?.toInt() ?: 0

        return {
            photoPrismPhotosService.getPhotos(
                count = limit,
                offset = cursor?.toInt() ?: 0,
                order = when (pagingOrder) {
                    PagingOrder.DESC -> PhotoPrismOrder.NEWEST
                    PagingOrder.ASC -> PhotoPrismOrder.OLDEST
                }
            )
        }
            .toSingle()
            .subscribeOn(Schedulers.io())
            .map { photoPrismPhotos ->
                photoPrismPhotos.mapSuccessful {
                    GalleryMedia(
                        source = it,
                        thumbnailUrlFactory = thumbnailUrlFactory,
                    )
                }
            }
            .map { galleryMediaItems ->
                DataPage(
                    items = galleryMediaItems,
                    nextCursor = (limit + offset).toString()
                )
            }
    }
}