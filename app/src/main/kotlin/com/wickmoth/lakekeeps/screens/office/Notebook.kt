package com.wickmoth.lakekeeps.screens.office

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wickmoth.lakekeeps.audio.LocalAudio
import com.wickmoth.lakekeeps.audio.Sfx
import com.wickmoth.lakekeeps.game.GameState
import com.wickmoth.lakekeeps.game.Owner
import com.wickmoth.lakekeeps.game.case.CaseProgress
import com.wickmoth.lakekeeps.game.case.case
import com.wickmoth.lakekeeps.game.littlebird.Flag
import com.wickmoth.lakekeeps.game.littlebird.LittleBird
import com.wickmoth.lakekeeps.game.messages.formatClock
import com.wickmoth.lakekeeps.ui.DesignScale
import com.wickmoth.lakekeeps.ui.Fonts
import com.wickmoth.lakekeeps.ui.FrameFit
import com.wickmoth.lakekeeps.ui.tactile

private object Notes {
    val Page = Color(0xFFEFE8D8)
    val Rule = Color(0xFFB9C7D3)
    val Margin = Color(0xFFD9817C)
    val Ink = Color(0xFF26283A)
    val RedInk = Color(0xFFB8343F)
    val Faint = Color(0xFF7A7466)
    val Tab = Color(0xFFDCD3BF)
}

private fun label(size: Float = 10.5f, color: Color = Notes.Faint) =
    TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Bold, fontSize = size.sp, color = color, letterSpacing = 1.2.sp)

/**
 * Sam's notebook, open over the desk: what he's after and his notes so far (with his own margin
 * notes when the player is stuck: a nudge, a pointer, then the answer), the intake, and his
 * voice memos written out.
 */
@Composable
internal fun Notebook(state: GameState, fit: FrameFit, start: NotebookPage, onClose: () -> Unit) {
    val progress = state.progress
    var page by remember { mutableStateOf(start) }
    val audio = LocalAudio.current
    val entries = LittleBird.findings.count { progress.has(it.flag) } + LittleBird.memos.count { progress.has(it.flag) }
    LaunchedEffect(entries) { progress.set(Flag.seen("notebook.$entries")) }

    DesignScale(fit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF14171C)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .padding(top = 30.dp, start = 12.dp, end = 12.dp, bottom = 18.dp)
                    .fillMaxSize(),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val pages = listOf(NotebookPage.Case to "Case", NotebookPage.Intake to "Intake", NotebookPage.Memos to "Memos")
                    pages.forEach { (p, name) ->
                        val enabled = p == NotebookPage.Case || progress.has(Flag.INTAKE_DONE)
                        if (enabled) {
                            BasicText(
                                name.uppercase(),
                                style = label(11f, if (page == p) Notes.Ink else Notes.Faint),
                                modifier = Modifier
                                    .background(if (page == p) Notes.Page else Notes.Tab, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .padding(horizontal = 14.dp, vertical = if (page == p) 10.dp else 7.dp)
                                    .tactile("$name page") {
                                        if (page != p) audio.play(Sfx.Paper, 0.5f)
                                        page = p
                                    },
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        "✕",
                        style = TextStyle(fontFamily = Fonts.Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFB7BEC6)),
                        modifier = Modifier
                            .padding(bottom = 6.dp, end = 6.dp)
                            .tactile("Close the notebook", onTap = onClose),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Notes.Page, RoundedCornerShape(topEnd = 6.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                        .drawBehind {
                            val dp = density
                            var y = 64f * dp
                            while (y < size.height - 8f * dp) {
                                drawLine(Notes.Rule, Offset(0f, y), Offset(size.width, y), 0.7f * dp, alpha = 0.55f)
                                y += 26f * dp
                            }
                            drawLine(Notes.Margin, Offset(30f * dp, 0f), Offset(30f * dp, size.height), 0.9f * dp, alpha = 0.6f)
                        },
                ) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                        label = "notebook page",
                    ) { shown ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 42.dp, end = 18.dp, top = 22.dp, bottom = 30.dp),
                        ) {
                            when (shown) {
                                NotebookPage.Case -> CasePage(state)
                                NotebookPage.Intake -> IntakePage(progress)
                                NotebookPage.Memos -> MemosPage(progress)
                            }
                        }
                    }
                }
            }
        }
    }
    BackHandler(onBack = onClose)
}

private val Heading = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Medium, fontSize = 30.sp, color = Notes.Ink)
private val Body = TextStyle(fontFamily = Fonts.Caveat, fontSize = 19.sp, color = Notes.Ink, lineHeight = 26.sp)
private val Margin = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Medium, fontSize = 19.sp, color = Notes.RedInk, lineHeight = 24.sp)

@Composable
private fun CasePage(state: GameState) {
    val progress = state.progress
    val objective = LittleBird.objective(progress)
    val calendar = Owner.Sam.case.calendar
    BasicText(LittleBird.TITLE.uppercase(), style = Heading)
    BasicText(
        "${calendar.fullDate(0)} · ${formatClock(state.messages.clock(Owner.Sam.case))}",
        style = label(10f),
    )
    Spacer(Modifier.height(18.dp))
    BasicText("NEXT", style = label())
    BasicText(objective.text, style = Body.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium))
    Spacer(Modifier.height(6.dp))
    Hints(progress, objective.id, objective.hints)
    Spacer(Modifier.height(20.dp))
    BasicText("NOTES", style = label())
    val findings = LittleBird.findings.filter { progress.has(it.flag) }
    if (findings.isEmpty()) {
        BasicText("Nothing yet. A message from Dana this afternoon. Somebody is about to need me.", style = Body.copy(color = Notes.Faint))
    }
    findings.forEach { finding ->
        Row(Modifier.padding(top = 4.dp)) {
            BasicText("–", style = Body, modifier = Modifier.width(14.dp))
            BasicText(finding.text, style = Body)
        }
    }
}

/** Sam's own margin notes when the player is stuck, one at a time. */
@Composable
private fun Hints(progress: CaseProgress, objective: String, hints: List<String>) {
    val audio = LocalAudio.current
    val shown = progress.hints(objective)
    hints.take(shown).forEach { hint ->
        BasicText(hint, style = Margin, modifier = Modifier.padding(top = 4.dp))
    }
    if (shown < hints.size) {
        val ask = listOf("Stuck? A nudge", "Still stuck? A pointer", "Just tell me").getOrElse(shown) { "" }
        Row(
            Modifier
                .padding(top = 8.dp)
                .background(Color(0x14B8343F), RoundedCornerShape(14.dp))
                .tactile(ask) {
                    audio.play(Sfx.Scribble, 0.45f)
                    progress.showHint(objective)
                }
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(10.dp)) {
                drawLine(Notes.RedInk, Offset(0f, size.height), Offset(size.width, 0f), 1.6f * density, StrokeCap.Round)
            }
            Spacer(Modifier.width(6.dp))
            BasicText(ask, style = label(10.5f, Notes.RedInk))
        }
    }
}

@Composable
private fun IntakePage(progress: CaseProgress) {
    BasicText("Intake: Amy Hart", style = Heading)
    BasicText("BY MESSAGE · SENT BY DANA BROOKS, DESERT LEGAL AID", style = label(9.5f))
    Spacer(Modifier.height(14.dp))
    listOf(
        "Amy Hart, 34. Dental hygienist. Son, Toby, 7.",
        "Says someone is watching her. Sure it's the ex, Ryan Hart, out on parole. He hurt her, years ago.",
        "Her phone dies by noon. People know where she's been. Calls with nobody on the line. Photos of her own house. The lights, the thermostat, the front lock.",
        "Nick, boyfriend of five months, put in the cameras and the locks. He told her to get a protective order.",
        "Wants proof a judge will accept. Can't pay much. Dana covers part.",
        "Rules: she doesn't reset it, delete anything or hand it over. She sends, I read. I never hold the phone.",
    ).forEach { line ->
        Row(Modifier.padding(top = 4.dp)) {
            BasicText("–", style = Body, modifier = Modifier.width(14.dp))
            BasicText(line, style = Body)
        }
    }
    val note = LittleBird.intakeNote(progress)
    if (note.isNotEmpty()) BasicText(note, style = Margin, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun MemosPage(progress: CaseProgress) {
    BasicText("Voice memos", style = Heading)
    Spacer(Modifier.height(10.dp))
    val memos = LittleBird.memos.filter { progress.has(it.flag) }
    if (memos.isEmpty()) BasicText("None yet.", style = Body.copy(color = Notes.Faint))
    memos.forEach { memo ->
        Column(Modifier.padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Notes.RedInk, CircleShape))
                Spacer(Modifier.width(7.dp))
                BasicText(memo.title.uppercase(), style = label(10f))
            }
            BasicText(memo.text, style = Body, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
