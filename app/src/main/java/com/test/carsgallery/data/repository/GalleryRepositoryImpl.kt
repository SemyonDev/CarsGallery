package com.test.carsgallery.data.repository

import com.test.carsgallery.data.datasource.RemoteDataSource
import com.test.carsgallery.domain.exception.NetworkException
import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.domain.repository.GalleryRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GalleryRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
) : GalleryRepository {

    override suspend fun getImages(): Result<List<ImageItem>> {
        return try {
            Result.success(remoteDataSource.fetchImages())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(NetworkException(e))
        } catch (e: HttpException) {
            Result.failure(NetworkException(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
