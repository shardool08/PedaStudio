package com.tippingpoint.pedastudio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tippingpoint.pedastudio.data.CloudSyncRepository
import com.tippingpoint.pedastudio.data.FlashcardRepository
import com.tippingpoint.pedastudio.i18n.LocalAppLanguage
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.components.RegisterScaffold
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.components.InfoBannerCard
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import androidx.compose.foundation.BorderStroke

@Composable
fun FlashcardsScreen(
    lessonId: String,
    flashcards: FlashcardRepository,
    cloud: CloudSyncRepository,
    onBack: () -> Unit,
) {
    val s = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    var overlayRevision by remember { mutableIntStateOf(0) }

    LaunchedEffect(lessonId) {
        cloud.loadFlashcardImageUrls(lessonId).let { urls ->
            flashcards.applyImageOverlay(lessonId, urls)
            overlayRevision++
        }
    }

    val pack = remember(lessonId, overlayRevision) { flashcards.getLessonFlashcards(lessonId) }

    RegisterScaffold(
        title = s.flashcardsTitle,
        stepLabel = pack?.title ?: lessonId,
        buttonText = s.continueBtn,
        canContinue = true,
        onBack = onBack,
        onContinue = onBack,
    ) {
        if (pack == null || pack.cards.isEmpty()) {
            InfoBannerCard(
                title = s.flashcardsTitle,
                body = "No flashcards for this lesson yet.",
            )
            return@RegisterScaffold
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            pack.cards.chunked(2).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowCards.forEach { card ->
                        FlashcardTile(card = card, lang = lang, modifier = Modifier.weight(1f))
                    }
                    if (rowCards.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardTile(
    card: com.tippingpoint.pedastudio.data.FlashcardItem,
    lang: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgTint),
        border = BorderStroke(1.dp, SeasideBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (card.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = card.imageUrl,
                    contentDescription = card.word,
                    modifier = Modifier
                        .size(72.dp)
                        .height(72.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(card.emoji, fontSize = 36.sp)
            }
            Text(card.word, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryDark)
            Text(
                card.meaning(lang),
                fontSize = 13.sp,
                color = AccentTeal,
                textAlign = TextAlign.Center,
            )
        }
    }
}
