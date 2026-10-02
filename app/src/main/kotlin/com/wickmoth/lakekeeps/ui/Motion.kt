package com.wickmoth.lakekeeps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object Ease {
    val OutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
    val InCubic = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
    val InOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val OutBack = Easing { x ->
        val c1 = 1.70158f
        1f + (c1 + 1f) * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
    }
}

/** Eased progress (0..1) of time [t] through the window [start, start + duration], all in ms. */
fun window(t: Float, start: Float, duration: Float, easing: Easing = LinearEasing): Float =
    easing.transform(((t - start) / duration).coerceIn(0f, 1f))

fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

/** A smooth loop in -1..1 with the given period, for idle motion driven by a clock in ms. */
fun wave(t: Float, periodMs: Float, phase: Float = 0f): Float =
    sin((t / periodMs + phase) * 2f * PI.toFloat())

/**
 * A scripted, one-shot clock that runs from 0 to [length] ms and then reports [onEnd].
 * [skipTo] only ever jumps forward, e.g. straight to the exit beat when the player taps.
 */
@Stable
class Sequence internal constructor(
    val length: Float,
    private val scope: CoroutineScope,
    private val onEnd: () -> Unit,
) {
    private val clock = Animatable(0f)
    private var job: Job? = null

    /** Elapsed time in ms. Read it in draw or layer lambdas where possible. */
    val t: Float get() = clock.value

    internal fun start() = run(0f)

    fun skipTo(ms: Float) {
        if (ms > clock.value) run(ms)
    }

    private fun run(from: Float) {
        job?.cancel()
        job = scope.launch {
            clock.snapTo(from)
            clock.animateTo(length, tween((length - from).roundToInt().coerceAtLeast(1), easing = LinearEasing))
            onEnd()
        }
    }
}

@Composable
fun rememberSequence(length: Float, startAt: Float = 0f, onEnd: () -> Unit = {}): Sequence {
    val scope = rememberCoroutineScope()
    val end by rememberUpdatedState(onEnd)
    val sequence = remember { Sequence(length, scope) { end() } }
    LaunchedEffect(sequence) {
        if (startAt > 0f) sequence.skipTo(startAt) else sequence.start()
    }
    return sequence
}

/**
 * A free-running clock in ms for idle motion (steam, flicker). It holds still while [running]
 * is false, e.g. while the scene is fully covered, so nothing redraws off-screen.
 */
@Composable
fun rememberIdleClock(running: Boolean = true): State<Float> {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val origin = withFrameMillis { it } - clock.floatValue.toLong()
        while (true) {
            withFrameMillis { clock.floatValue = (it - origin).toFloat() }
        }
    }
    return clock
}
