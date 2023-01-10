package com.example.photogallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.photogallery.api.GalleryItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "PhotoGalleryViewModel"

class PhotoGalleryViewModel : ViewModel() {
    private val photoRepository = PhotoRepository()
    private val preferencesRepository = PreferencesRepository.get()

    private val _uiState: MutableStateFlow<PhotoGalleryUiState> =
        MutableStateFlow(PhotoGalleryUiState())
    val uiState: StateFlow<PhotoGalleryUiState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.storedQuery.collectLatest {
                query ->
                fetchGalleryItems(query).collect {
                        images ->
                        _uiState.update {
                            oldState -> oldState.copy(
                                images = images,
                                query = query
                            )
                    }
                }
            }
        }

        viewModelScope.launch {
           preferencesRepository.isPolling.collectLatest{
               isPolling -> _uiState.update {
                    it.copy(isPolling = isPolling)
               }
           }
        }
    }

    fun toggleIsPooling(){
        Log.d(TAG, "view model pooling")
        viewModelScope.launch {
            preferencesRepository.setPooling(!uiState.value.isPolling)
           /* _uiState.update {
                it.copy(isPolling = !uiState.value.isPolling)
            }*/
        }
    }

    fun setQuery(query: String){
        viewModelScope.launch {
            preferencesRepository.setStoredQuery(query)

            fetchGalleryItems(query).collectLatest {
                images ->
                _uiState.update { oldState ->
                    oldState.copy(
                        images = images,
                        query = query
                    )
                }
            }
        }
    }

    private fun fetchGalleryItems(query: String): Flow<PagingData<GalleryItem>> {
        return if(query.isNotEmpty()){
            photoRepository.searchPhotos(query).cachedIn(viewModelScope)
        } else {
            photoRepository.fetchPhotos().cachedIn(viewModelScope)
        }
    }

    fun getPhotos(): Flow<PagingData<GalleryItem>> {
        return photoRepository.fetchPhotos()
            .cachedIn(viewModelScope)
    }
}

data class PhotoGalleryUiState(
    val images:PagingData<GalleryItem> = PagingData.empty(),
    val query: String = "",
    val isPolling: Boolean = false
)