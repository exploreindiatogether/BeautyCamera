package com.tejaslabs.beauty.ai.camera.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tejaslabs.beauty.ai.camera.gallery.GalleryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryImportScreen(
    onBack: () -> Unit,
    onPhotoSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    androidx.activity.compose.BackHandler(onBack = onBack)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Photos, 1: Albums
    
    var photos by remember { mutableStateOf<List<GalleryHelper.PhotoItem>>(emptyList()) }
    var albums by remember { mutableStateOf<List<GalleryHelper.AlbumItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Query MediaStore asynchronously
    LaunchedEffect(key1 = true) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val queriedPhotos = GalleryHelper.fetchPhotos(context)
            val queriedAlbums = GalleryHelper.fetchAlbums(context)
            withContext(Dispatchers.Main) {
                photos = queriedPhotos
                albums = queriedAlbums
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Dark mode background
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Gallery",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Tabs
        val tabsList = listOf("Photos", "Albums")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFFFF4081)
                )
            }
        ) {
            tabsList.forEachIndexed { index, name ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = name,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) Color(0xFFFF4081) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF4081))
            }
        } else {
            if (selectedTab == 0) {
                if (photos.isEmpty()) {
                    EmptyGalleryState(text = "No photos found on this device.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().weight(1f).padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(photos) { photo ->
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = "Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clickable { onPhotoSelected(photo.uri) }
                            )
                        }
                    }
                }
            } else {
                if (albums.isEmpty()) {
                    EmptyGalleryState(text = "No albums found on this device.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().weight(1f).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(albums) { album ->
                            AlbumGridItem(album = album, onClick = {
                                // For simplicity, clicking an album can filter photos or we just select it
                                // In this MVP, we show albums as folders
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumGridItem(
    album: GalleryHelper.AlbumItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        AsyncImage(
            model = album.coverUri,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
        Text(
            text = "${album.photoCount} Photos",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun EmptyGalleryState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}
