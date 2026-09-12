package com.tippingpoint.pedastudio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.data.TierConfig
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.theme.NavBg
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder
import com.tippingpoint.pedastudio.ui.theme.WarmPeach

@Composable
fun AppBrandHeader(
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentTeal),
            contentAlignment = Alignment.Center,
        ) {
            Text("P", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "PedaStudio",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = PrimarySteel,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = PrimarySteel.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun OnboardingScreenLayout(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
fun OnboardingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleScreenScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = NavBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, color = PrimaryDark, fontSize = 18.sp)
                        if (subtitle != null) {
                            Text(subtitle, fontSize = 12.sp, color = PrimarySteel.copy(alpha = 0.75f))
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentTeal,
            disabledContainerColor = AccentTeal.copy(alpha = 0.35f),
        ),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionHeaderLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = PrimarySteel.copy(alpha = 0.55f),
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun InfoBannerCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 15.sp)
            Text(body, color = PrimaryDark.copy(0.65f), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp), lineHeight = 20.sp)
        }
    }
}

@Composable
fun HighlightLessonCard(
    label: String,
    title: String,
    subtitle: String,
    meta: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlight) WarmPeach else BgTint),
        border = BorderStroke(1.dp, SeasideBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 0.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryDark.copy(0.55f))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PrimaryDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 13.sp, color = PrimaryDark.copy(0.65f))
            Text(meta, fontSize = 12.sp, color = PrimaryDark.copy(0.5f))
        }
    }
}

@Composable
fun ProfileHeroHeader(
    initials: String,
    name: String,
    phone: String,
    school: String,
    tier: TierConfig.TierId? = null,
    photoUri: String? = null,
    onEditPhoto: (() -> Unit)? = null,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = NavBg) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            if (tier != null) {
                TierBadge(
                    tier = tier,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .then(
                                if (onEditPhoto != null) Modifier.clickable(onClick = onEditPhoto)
                                else Modifier
                            )
                            .background(AccentTeal),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(initials, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onEditPhoto != null) {
                        IconButton(
                            onClick = onEditPhoto,
                            modifier = Modifier
                                .offset(x = 4.dp, y = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (phone.isNotBlank()) {
                    Text("+91 $phone", fontSize = 14.sp, color = PrimarySteel.copy(0.75f))
                }
                if (school.isNotBlank()) {
                    Text(
                        school,
                        fontSize = 14.sp,
                        color = PrimarySteel.copy(0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeaderLabel(title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SeasideBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true,
) {
    Column {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(label, fontSize = 12.sp, color = PrimarySteel.copy(0.65f))
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (showDivider) HorizontalDivider(color = SeasideBorder.copy(0.7f), thickness = 0.5.dp)
    }
}

@Composable
fun ProfileActionRow(
    label: String,
    value: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Column {
        if (showDivider) HorizontalDivider(color = SeasideBorder.copy(0.7f), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = PrimaryDark)
                if (value != null) {
                    Text(value, fontSize = 13.sp, color = PrimarySteel.copy(0.65f), modifier = Modifier.padding(top = 2.dp))
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimarySteel.copy(0.4f), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun ProfileChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text, fontSize = 13.sp) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = BgTint,
            disabledLabelColor = PrimaryDark,
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = false, borderColor = SeasideBorder),
    )
}

fun profileInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@Composable
fun PlanGeneratingOverlay(
    progress: Int,
    quote: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "$progress%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = AccentTeal,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AccentTeal,
                trackColor = SeasideBorder,
            )
            Text(
                text = statusLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimarySteel.copy(0.75f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "\"$quote\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = PrimaryDark.copy(0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
