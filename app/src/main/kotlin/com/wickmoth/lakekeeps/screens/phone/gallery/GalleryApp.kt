package com.wickmoth.lakekeeps.screens.phone.gallery

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.R
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.StoryCalendar
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.gallery.Albums
import com.wickmoth.lakekeeps.game.gallery.Photo
import com.wickmoth.lakekeeps.screens.phone.AppHeader
import com.wickmoth.lakekeeps.screens.phone.Glyph
import com.wickmoth.lakekeeps.screens.phone.GlyphIcon
import com.wickmoth.lakekeeps.screens.phone.HeaderButton
import com.wickmoth.lakekeeps.screens.phone.HeaderHeight
import com.wickmoth.lakekeeps.screens.phone.PhoneApp
import com.wickmoth.lakekeeps.screens.phone.PhoneColors
import com.wickmoth.lakekeeps.screens.phone.PhoneOs
import com.wickmoth.lakekeeps.screens.phone.PhoneText
import com.wickmoth.lakekeeps.screens.phone.PlaceholderImage
import com.wickmoth.lakekeeps.screens.phone.phoneSpec
import com.wickmoth.lakekeeps.screens.phone.phoneText
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.Ease
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.Haptic
import com.wickmoth.lakekeeps.ui.lerp
import com.wickmoth.lakekeeps.ui.tactile
import com.wickmoth.lakekeeps.ui.window
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/** Photos being looked at full size: the album they come from and the one tapped. */
private class Viewing(val photos: List<Photo>, val start: Int)

/** Where each thumbnail on screen is, by photo id, so a photo can fly out of it and back. */
private typealias Thumbnails = MutableMap<String, LayoutCoordinates>

/**
 * The Gallery app: photos by day, the hidden album behind the eye, and a viewer that zooms a photo
 * out of its thumbnail, swipes between photos, and flicks away.
 */
@Composable
internal fun GalleryApp(os: PhoneOs, fit: FrameFit) {
    val scope = rememberCoroutineScope()
    val audio = LocalAudio.current
    val thumbnails = remember { HashMap<String, LayoutCoordinates>() }
    var hiddenOpen by remember { mutableStateOf(false) }
    // 0 = the main album, 1 = the hidden album slid fully in
    val hiddenIn = remember { Animatable(0f) }
    var viewing by remember { mutableStateOf<Viewing?>(null) }
    // the photo out of its thumbnail: its thumbnail stays empty until it flies back
    var flying by remember { mutableStateOf<String?>(null) }
    val zoom = remember { Animatable(0f) }

    fun showHidden() {
        os.feedback(Haptic.Tick)
        audio.play(Sfx.Reveal)
        hiddenOpen = true
        scope.launch { hiddenIn.animateTo(1f, tween(380, easing = Ease.Emphasized)) }
    }

    fun hideHidden() {
        if (!hiddenOpen || hiddenIn.targetValue == 0f) return
        scope.launch {
            hiddenIn.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            hiddenOpen = false
        }
    }

    fun view(photos: List<Photo>, index: Int) {
        if (viewing != null) return
        os.feedback(Haptic.Tick)
        audio.play(Sfx.AppOpen, 0.35f)
        viewing = Viewing(photos, index)
        flying = photos[index].id
        scope.launch {
            zoom.snapTo(0f)
            zoom.animateTo(1f, tween(380, easing = Ease.Emphasized))
        }
    }

    fun closeViewer() {
        if (viewing == null || zoom.targetValue == 0f) return
        audio.play(Sfx.AppClose, 0.35f)
        scope.launch {
            zoom.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            viewing = null
            flying = null
        }
    }

    DesignScale(fit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.AppBackground),
        ) {
            AllPhotos(
                os,
                thumbnails,
                flying = { flying },
                modifier = Modifier.graphicsLayer {
                    translationX = -0.28f * size.width * hiddenIn.value
                    alpha = 1f - 0.6f * hiddenIn.value
                },
                onHidden = ::showHidden,
                onView = { photos, i -> view(photos, i) },
            )
            if (hiddenOpen) {
                HiddenPhotos(
                    os,
                    thumbnails,
                    flying = { flying },
                    modifier = Modifier.graphicsLayer { translationX = size.width * (1f - hiddenIn.value) },
                    onBack = ::hideHidden,
                    onView = { photos, i -> view(photos, i) },
                )
            }
            viewing?.let { v ->
                Viewer(v, os.owner.case.calendar, zoom, thumbnails, onPage = { flying = it.id }, onClose = ::closeViewer)
            }
        }
    }
    BackHandler(enabled = viewing != null && !os.shadeOpen) { closeViewer() }
    PredictiveBackHandler(enabled = viewing == null && hiddenOpen && !os.shadeOpen) { gesture ->
        try {
            gesture.collect { event -> scope.launch { hiddenIn.snapTo(1f - 0.32f * event.progress) } }
            hideHidden()
        } catch (cancelled: CancellationException) {
            scope.launch { hiddenIn.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 500f)) }
            throw cancelled
        }
    }
    PredictiveBackHandler(enabled = viewing == null && !hiddenOpen && !os.shadeOpen) { gesture ->
        try {
            gesture.collect { os.peekBackFromApp(it.progress) }
            os.closeApp()
        } catch (cancelled: CancellationException) {
            os.restoreApp()
            throw cancelled
        }
    }
}

@Composable
private fun AllPhotos(
    os: PhoneOs,
    thumbnails: Thumbnails,
    flying: () -> String?,
    modifier: Modifier,
    onHidden: () -> Unit,
    onView: (List<Photo>, Int) -> Unit,
) {
    val photos = remember(os.owner) { Albums.photos(os.owner) }
    // rows of three, under the day they were taken
    val days = remember(photos) { photos.withIndex().groupBy { it.value.daysAgo }.toList() }
    Column(
        modifier
            .fillMaxSize()
            .background(PhoneColors.AppBackground),
    ) {
        AppHeader {
            HeaderButton(
                Glyph.EyeOff,
                stringResource(R.string.gallery_hidden),
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
            ) { onHidden() }
            PhoneText(stringResource(galleryTitle(os)), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
            HeaderButton(
                Glyph.Close,
                stringResource(R.string.close),
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            ) { os.closeApp() }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            days.forEach { (daysAgo, shots) ->
                item(key = "day-$daysAgo") {
                    PhoneText(
                        os.owner.case.calendar.longDate(daysAgo),
                        phoneText(16.sp, FontWeight.SemiBold),
                        Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
                    )
                }
                shots.chunked(3).forEachIndexed { row, three ->
                    item(key = "row-$daysAgo-$row") {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            three.forEach { (index, photo) ->
                                Thumbnail(
                                    photo,
                                    os.owner.case.calendar,
                                    thumbnails,
                                    hidden = { flying() == photo.id },
                                    appear = { window(os.appIn.value, 0.35f + index.coerceAtMost(8) * 0.03f, 0.4f, Ease.OutCubic) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                ) { onView(photos, index) }
                            }
                            repeat(3 - three.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenPhotos(
    os: PhoneOs,
    thumbnails: Thumbnails,
    flying: () -> String?,
    modifier: Modifier,
    onBack: () -> Unit,
    onView: (List<Photo>, Int) -> Unit,
) {
    val photos = remember(os.owner) { Albums.hidden(os.owner) }
    Column(
        modifier
            .fillMaxSize()
            .background(PhoneColors.AppBackground),
    ) {
        AppHeader {
            HeaderButton(
                Glyph.Back,
                stringResource(R.string.back),
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
            ) { onBack() }
            PhoneText(stringResource(galleryTitle(os)), phoneText(21.sp, FontWeight.Bold), Modifier.align(Alignment.Center))
            HeaderButton(
                Glyph.Close,
                stringResource(R.string.close),
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            ) { os.closeApp() }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "title") {
                Row(
                    Modifier.padding(top = 20.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(Glyph.Eye, PhoneColors.Accent, Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    PhoneText(
                        stringResource(R.string.gallery_hidden).uppercase(),
                        phoneText(14.sp, FontWeight.Bold, PhoneColors.Accent).copy(letterSpacing = 0.1.em),
                    )
                }
            }
            if (photos.isEmpty()) {
                item(key = "none") {
                    PhoneText(
                        stringResource(R.string.gallery_hidden_none),
                        phoneText(15.sp, color = PhoneColors.TextMuted),
                        Modifier.padding(top = 48.dp),
                    )
                }
            }
            itemsIndexed(photos, key = { _, photo -> photo.id }) { i, photo ->
                Column(Modifier.padding(bottom = 22.dp)) {
                    Thumbnail(
                        photo,
                        os.owner.case.calendar,
                        thumbnails,
                        hidden = { flying() == photo.id },
                        appear = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        corner = 10,
                    ) { onView(photos, i) }
                    Spacer(Modifier.height(8.dp))
                    PhoneText(photo.caption, phoneText(14.sp, FontWeight.SemiBold))
                    PhoneText(os.owner.case.calendar.longDate(photo.daysAgo), phoneText(12.sp, color = PhoneColors.TextFaint))
                }
            }
        }
    }
}

/** A photo in a grid or list. It sits empty while its photo is out in the viewer. */
@Composable
private fun Thumbnail(
    photo: Photo,
    calendar: StoryCalendar,
    thumbnails: Thumbnails,
    hidden: () -> Boolean,
    appear: () -> Float,
    modifier: Modifier,
    corner: Int = 4,
    onTap: () -> Unit,
) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    DisposableEffect(photo.id) { onDispose { thumbnails.remove(photo.id) } }
    Box(
        modifier
            .onGloballyPositioned { thumbnails[photo.id] = it }
            .graphicsLayer {
                val a = appear()
                alpha = if (hidden()) 0f else a
                val s = lerp(0.92f, 1f, a) * (1f - 0.05f * press.value)
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(corner.dp))
            .tactile(
                stringResource(R.string.gallery_photo, photo.caption, calendar.longDate(photo.daysAgo)),
                onPress = { down -> scope.launch { press.animateTo(if (down) 1f else 0f, spring(stiffness = 800f)) } },
                onTap = onTap,
            ),
    ) {
        PlaceholderImage(photo.id, Modifier.fillMaxSize(), glyphSize = 22.dp)
    }
}

/**
 * Photos full size on black. The tapped one grows out of its thumbnail; swipe for the others; back,
 * or a flick up or down, sends the one showing back to its thumbnail (or fades it if it has none).
 */
@Composable
private fun Viewer(
    viewing: Viewing,
    calendar: StoryCalendar,
    zoom: Animatable<Float, AnimationVector1D>,
    thumbnails: Thumbnails,
    onPage: (Photo) -> Unit,
    onClose: () -> Unit,
) {
    val photos = viewing.photos
    val pager = rememberPagerState(initialPage = viewing.start) { photos.size }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val flickPx = with(LocalDensity.current) { 110.dp.toPx() }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { onPage(photos[it]) } }
    val current = photos[pager.currentPage]

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = zoom.value * (1f - (abs(drag.value) / (size.height * 0.5f)).coerceIn(0f, 1f) * 0.7f) }
                .background(Darkroom),
        )
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> scope.launch { drag.snapTo(drag.value + delta) } },
                    onDragStopped = { velocity ->
                        if (abs(drag.value) > flickPx || abs(velocity) > 1600f) {
                            onClose()
                            drag.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                        } else {
                            drag.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 500f))
                        }
                    },
                ),
        ) { page ->
            val photo = photos[page]
            var frame by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlaceholderImage(
                    photo.id,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .onGloballyPositioned { frame = it }
                        .graphicsLayer {
                            val z = zoom.value
                            val showing = page == pager.currentPage
                            if (showing) translationY = drag.value
                            if (z >= 1f) return@graphicsLayer
                            val to = frame?.takeIf { it.isAttached }?.boundsInRoot()
                            val from = thumbnails[photo.id]?.takeIf { it.isAttached && showing }?.boundsInRoot()
                            if (to != null && from != null && to.width > 0f) {
                                // fly between the thumbnail and here
                                val s = lerp(from.width / to.width, 1f, z)
                                scaleX = s
                                scaleY = s
                                translationX = lerp(from.center.x - to.center.x, 0f, z)
                                translationY += lerp(from.center.y - to.center.y, 0f, z)
                            } else {
                                alpha = z
                                val s = lerp(0.9f, 1f, z)
                                scaleX = s
                                scaleY = s
                            }
                        }
                        .semantics { contentDescription = "${photo.caption}, ${calendar.longDate(photo.daysAgo)}" },
                    glyphSize = 56.dp,
                )
            }
        }
        // Back and the day it was taken across the top, what it shows along the bottom.
        val chrome = Modifier.graphicsLayer {
            alpha = window(zoom.value, 0.55f, 0.45f) * (1f - (abs(drag.value) / flickPx).coerceIn(0f, 1f))
        }
        Box(
            chrome
                .fillMaxWidth()
                .height(HeaderHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(76.dp),
            ) {
                HeaderButton(
                    Glyph.Back,
                    stringResource(R.string.back),
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp),
                ) { onClose() }
                PhoneText(calendar.longDate(current.daysAgo), phoneText(18.sp, FontWeight.SemiBold), Modifier.align(Alignment.Center))
            }
        }
        PhoneText(
            current.caption,
            phoneText(15.sp, color = PhoneColors.TextMuted).copy(textAlign = TextAlign.Center),
            chrome
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 56.dp),
        )
    }
}

/** Behind a photo shown full size. */
private val Darkroom = Color(0xFF04070A)

/** The app's name on this phone's home screen: Gallery, or Photos. */
private fun galleryTitle(os: PhoneOs): Int = phoneSpec(os.owner).apps.first { it.opens == PhoneApp.Gallery }.label
