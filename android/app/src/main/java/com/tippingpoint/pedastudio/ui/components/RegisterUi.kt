package com.tippingpoint.pedastudio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippingpoint.pedastudio.i18n.LocalAppStrings
import com.tippingpoint.pedastudio.ui.theme.AccentTeal
import com.tippingpoint.pedastudio.ui.theme.BgTint
import com.tippingpoint.pedastudio.ui.theme.NavBg
import com.tippingpoint.pedastudio.ui.theme.PrimaryDark
import com.tippingpoint.pedastudio.ui.theme.PrimarySteel
import com.tippingpoint.pedastudio.ui.theme.SeasideBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScaffold(
    title: String,
    stepLabel: String,
    buttonText: String,
    canContinue: Boolean,
    onBack: (() -> Unit)? = null,
    onContinue: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = NavBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, color = PrimaryDark, fontSize = 18.sp)
                        Text(stepLabel, fontSize = 12.sp, color = PrimarySteel.copy(alpha = 0.75f))
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
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp, tonalElevation = 2.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onContinue,
                        enabled = canContinue,
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentTeal,
                            disabledContainerColor = AccentTeal.copy(alpha = 0.35f),
                        ),
                    ) {
                        Text(buttonText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    content()
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }
}

@Composable
fun FormSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SeasideBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgTint.copy(alpha = 0.35f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = PrimarySteel, fontSize = 15.sp)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = PrimaryDark.copy(alpha = 0.55f), lineHeight = 16.sp)
                }
            }
            HorizontalDivider(color = SeasideBorder)
            content()
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDark.copy(alpha = 0.85f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownPicker(
    label: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    searchable: Boolean = options.size > 5,
) {
    val s = LocalAppStrings.current
    val resolvedPlaceholder = placeholder ?: s.tapToSelect
    var showSheet by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val display = options.find { it.first == selectedValue }?.second ?: resolvedPlaceholder
    val isPlaceholder = options.none { it.first == selectedValue }

    LaunchedEffect(showSheet) {
        if (!showSheet) query = ""
    }

    val filteredOptions = remember(options, query) {
        if (query.isBlank()) options
        else options.filter { (_, text) -> text.contains(query, ignoreCase = true) }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimarySteel,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = pickerFieldColors(isPlaceholder),
            singleLine = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showSheet = true },
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                color = PrimarySteel,
                fontSize = 16.sp,
            )
            Text(
                text = s.chooseOne,
                modifier = Modifier.padding(horizontal = 20.dp),
                fontSize = 12.sp,
                color = PrimaryDark.copy(alpha = 0.55f),
            )
            if (searchable) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(s.search, fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            }
            HorizontalDivider(
                color = SeasideBorder,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (filteredOptions.isEmpty()) {
                Text(
                    text = s.noMatches,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    fontSize = 14.sp,
                    color = PrimaryDark.copy(alpha = 0.5f),
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                items(filteredOptions, key = { it.first }) { (value, text) ->
                    val selected = value == selectedValue
                    ListItem(
                        headlineContent = {
                            Text(
                                text = text,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) AccentTeal else PrimaryDark,
                            )
                        },
                        modifier = Modifier.clickable {
                            onSelect(value)
                            showSheet = false
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (selected) AccentTeal.copy(alpha = 0.1f) else Color.Transparent,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipMultiSelect(
    label: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FieldLabel(label)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, text) ->
            val isSelected = id in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(id) },
                label = { Text(text, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipSingleSelect(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FieldLabel(label)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { (value, text) ->
            val isSelected = selected == value
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(value) },
                label = {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
fun RadioOptionList(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .background(
                        if (isSelected) AccentTeal.copy(alpha = 0.12f) else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) AccentTeal else Color.Transparent,
                    border = BorderStroke(2.dp, if (isSelected) AccentTeal else SeasideBorder),
                ) {
                    if (isSelected) {
                        BoxCenterDot()
                    }
                }
                Text(
                    label,
                    fontSize = 14.sp,
                    color = PrimaryDark,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun BoxCenterDot() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(4.dp), color = Color.White) {}
    }
}

@Composable
fun OutlinedFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentTeal,
    unfocusedBorderColor = SeasideBorder,
    focusedLabelColor = AccentTeal,
    cursorColor = AccentTeal,
)

@Composable
private fun pickerFieldColors(isPlaceholder: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentTeal,
    unfocusedBorderColor = SeasideBorder,
    focusedLabelColor = AccentTeal,
    unfocusedTextColor = if (isPlaceholder) PrimaryDark.copy(alpha = 0.45f) else PrimaryDark,
    focusedTextColor = if (isPlaceholder) PrimaryDark.copy(alpha = 0.45f) else PrimaryDark,
)
