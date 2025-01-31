/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.presenter

import android.annotation.SuppressLint
import android.media.MediaDataSource
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.lukaspieper.truvark.Page
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.domain.crypto.decryption.coil.CipherZoomableImageSource
import de.lukaspieper.truvark.ui.extensions.safeDrawingTopAppBar
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.preview.PreviewSampleData
import de.lukaspieper.truvark.ui.views.presenter.views.FileNotFoundContentView
import de.lukaspieper.truvark.ui.views.presenter.views.NotSupportedContentView
import de.lukaspieper.truvark.ui.views.presenter.views.VideoContentView
import me.saket.telephoto.zoomable.ZoomableImage

@Composable
fun PresenterPage(
    parameters: Page.Presenter,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresenterViewModel = hiltViewModel(
        creationCallback = { factory: PresenterViewModel.Factory ->
            factory.create(parameters.folderId)
        }
    )
) {
    val itemsData by viewModel.itemsData.collectAsStateWithLifecycle()
    val imagesFitScreen by viewModel.imagesFitScreen.collectAsStateWithLifecycle(true)

    PresenterView(
        itemsData = itemsData,
        createCipherZoomableImageSource = viewModel::createCipherZoomableImageSource,
        createMediaDataSource = viewModel::createMediaDataSource,
        imagesFitScreen = imagesFitScreen,
        initialFileId = parameters.fileId,
        navigateBack = navigateBack,
        modifier = modifier
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresenterView(
    itemsData: PresenterViewModel.ItemsData,
    createCipherZoomableImageSource: (FileInfo, String) -> CipherZoomableImageSource,
    createMediaDataSource: (FileInfo) -> MediaDataSource,
    imagesFitScreen: Boolean,
    initialFileId: String,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current

    // TODO: Use `Saveable`. Needs to be "synced" with MediaView controls.
    val isTopBarVisible = remember { mutableStateOf(true) }
    var topBarTitle by rememberSaveable { mutableStateOf("") }

    DisposableEffect(isTopBarVisible.value) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {
            if (!isTopBarVisible.value) {
                hide(WindowInsetsCompat.Type.statusBars())
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose {
            insetsController.apply {
                show(WindowInsetsCompat.Type.statusBars())
                show(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isTopBarVisible.value,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.zIndex(1f)
            ) {
                TopAppBar(
                    windowInsets = WindowInsets.safeDrawingTopAppBar,
                    title = {
                        Text(
                            text = topBarTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        },
        contentWindowInsets = if (isTopBarVisible.value) ScaffoldDefaults.contentWindowInsets else WindowInsets(0),
        modifier = modifier.fillMaxSize() // https://stackoverflow.com/a/76916130
    ) { _ ->
        CipherFilePager(
            itemsData = itemsData,
            createCipherZoomableImageSource = createCipherZoomableImageSource,
            createMediaDataSource = createMediaDataSource,
            initialFileId = initialFileId,
            imagesFitScreen = imagesFitScreen,
            isTopBarVisible = isTopBarVisible,
            switchTopBarVisibility = { isTopBarVisible.value = !isTopBarVisible.value },
            updateTopBarTitle = { topBarTitle = it }
        )
    }
}

@Composable
private fun CipherFilePager(
    itemsData: PresenterViewModel.ItemsData,
    createCipherZoomableImageSource: (FileInfo, String) -> CipherZoomableImageSource,
    createMediaDataSource: (FileInfo) -> MediaDataSource,
    initialFileId: String,
    imagesFitScreen: Boolean,
    isTopBarVisible: State<Boolean>,
    switchTopBarVisibility: () -> Unit,
    updateTopBarTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (itemsData.cipherFileEntities.isEmpty()) {
        return
    }

    val pagerState = rememberPagerState(
        initialPage = itemsData.cipherFileEntities.indexOfFirst { item -> item.id == initialFileId },
        pageCount = { itemsData.cipherFileEntities.size }
    )

    LaunchedEffect(pagerState, updateTopBarTitle) {
        snapshotFlow { pagerState.currentPage }.collect { index ->
            val fileFullName = itemsData.cipherFileEntities[index].fullName()
            updateTopBarTitle("${index + 1}/${pagerState.pageCount} - $fileFullName")
        }
    }

    HorizontalPager(
        state = pagerState,
        key = { index -> itemsData.cipherFileEntities[index].id },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { index ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (itemsData.physicalFiles == null) {
                CircularProgressIndicator(modifier = Modifier.align(alignment = Alignment.Center))
            } else {
                val cipherFileEntity = remember(itemsData, index) { itemsData.cipherFileEntities[index] }
                val physicalFile = remember(cipherFileEntity) { itemsData.physicalFiles[cipherFileEntity.id] }

                if (physicalFile == null) {
                    FileNotFoundContentView(cipherFileEntity.fullName())
                } else {
                    CipherFilePresenter(
                        fileName = cipherFileEntity.fullName(),
                        mimeType = cipherFileEntity.mimeType,
                        createCipherZoomableImageSource = {
                            createCipherZoomableImageSource(physicalFile, cipherFileEntity.mimeType)
                        },
                        createMediaDataSource = { createMediaDataSource(physicalFile) },
                        isTopBarVisible = isTopBarVisible,
                        switchTopBarVisibility = switchTopBarVisibility,
                        imagesFitScreen = imagesFitScreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun CipherFilePresenter(
    fileName: String,
    mimeType: String,
    createCipherZoomableImageSource: () -> CipherZoomableImageSource,
    createMediaDataSource: () -> MediaDataSource,
    isTopBarVisible: State<Boolean>,
    switchTopBarVisibility: () -> Unit,
    imagesFitScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    if (mimeType.startsWith("image/")) {
        val imageSource = remember(fileName, mimeType) {
            createCipherZoomableImageSource()
        }

        ZoomableImage(
            image = imageSource,
            contentDescription = null,
            contentScale = if (imagesFitScreen) ContentScale.Fit else ContentScale.Inside,
            onClick = { switchTopBarVisibility() },
            modifier = modifier
        )
    } else if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) {
        val mediaDataSource = remember(fileName) { createMediaDataSource() }

        VideoContentView(
            mediaDataSource = mediaDataSource,
            isTopBarVisible = isTopBarVisible,
            switchTopBarVisibility = switchTopBarVisibility,
            modifier = modifier
        )
    } else {
        NotSupportedContentView(fileName)
    }
}

@PagePreviews
@Composable
private fun PresenterViewPreview() = PreviewHost {
    PresenterView(
        itemsData = PresenterViewModel.ItemsData(
            cipherFileEntities = PreviewSampleData.cipherFileEntities
        ),
        createCipherZoomableImageSource = { _, _ -> error("Not implemented") },
        createMediaDataSource = { error("Not implemented") },
        imagesFitScreen = true,
        initialFileId = "file0",
        navigateBack = { }
    )
}
