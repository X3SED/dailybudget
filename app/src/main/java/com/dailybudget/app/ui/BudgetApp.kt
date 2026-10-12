package com.dailybudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dailybudget.app.Budget
import com.dailybudget.app.Entry
import com.dailybudget.app.EntryType
import com.dailybudget.app.R
import com.dailybudget.app.Settings
import com.dailybudget.app.Store
import com.dailybudget.app.Summary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LocalMoney = staticCompositionLocalOf { Money("EUR") }
private val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault() }

/** Ouvre le clavier sur le champ montant ; désactivé pour les captures d'écran (Robolectric). */
val LocalAutoFocus = staticCompositionLocalOf { true }

@Composable
private fun money(cents: Long) = LocalMoney.current.format(cents)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApp(store: Store) {
    var entries by remember { mutableStateOf(store.loadEntries()) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var input by remember { mutableStateOf<EntryType?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // Recalcule la date à chaque retour dans l'app (passage à minuit, nouveau mois…).
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose { }
    }

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val money = remember(settings.currency, locale) { Money(settings.currency, locale) }
    val summary = Budget.summarize(entries, settings, today)

    fun add(entry: Entry) {
        entries = entries + entry
        store.saveEntries(entries)
        today = LocalDate.now()
    }

    fun delete(entry: Entry) {
        entries = entries.filterNot { it.id == entry.id }
        store.saveEntries(entries)
    }

    CompositionLocalProvider(LocalMoney provides money, LocalAppLocale provides locale) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                            Text(
                                today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
                                    .replaceFirstChar { it.uppercase(locale) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    if (summary == null) {
                        Welcome(onStart = { input = EntryType.BALANCE }, onSettings = { showSettings = true })
                    } else {
                        Hero(summary)
                    }
                }
                if (summary != null) {
                    item { Stats(summary, settings) }
                }
                item {
                    Actions(
                        onBalance = { input = EntryType.BALANCE },
                        onExpense = { input = EntryType.EXPENSE },
                        onIncome = { input = EntryType.INCOME },
                    )
                }
                if (entries.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.history),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                    items(entries.sortedByDescending { it.timestamp }.take(60), key = { it.id }) { entry ->
                        EntryRow(entry, onDelete = { delete(entry) })
                    }
                }
            }
        }

        input?.let { type ->
            AmountDialog(
                type = type,
                initial = if (type == EntryType.BALANCE) summary?.balanceCents else null,
                onDismiss = { input = null },
                onConfirm = { cents, note, category, excluded ->
                    val now = System.currentTimeMillis()
                    add(Entry(now, type, cents, now, note, category, excluded))
                    input = null
                },
            )
        }

        if (showSettings) {
            SettingsDialog(
                settings = settings,
                onDismiss = { showSettings = false },
                onSave = {
                    settings = it
                    store.saveSettings(it)
                    showSettings = false
                },
            )
        }
    }
}

@Composable
private fun Hero(s: Summary) {
    val over = s.remainingTodayCents < 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    if (over) listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D)) else listOf(HeroStart, HeroEnd)
                )
            )
            .padding(24.dp),
    ) {
        Column {
            Text(
                stringResource(if (over) R.string.over_budget_today else R.string.can_spend_today),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                money(if (over) -s.remainingTodayCents else s.remainingTodayCents),
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp,
            )
            Spacer(Modifier.height(16.dp))
            val progress = if (s.dailyCents <= 0) 1f
            else (s.spentTodayCents.toFloat() / s.dailyCents).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.spent_today, money(s.spentTodayCents.coerceAtLeast(0))),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.budget_per_day, money(s.dailyCents)),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (s.excludedTodayCents > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.excluded_today, money(s.excludedTodayCents)),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Stats(s: Summary, settings: Settings) = Column {
    val locale = LocalAppLocale.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.balance),
            value = money(s.balanceCents),
            valueColor = if (s.balanceCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.until_payday),
            value = pluralStringResource(R.plurals.days, s.daysLeft, s.daysLeft),
            sub = stringResource(R.string.payday_on, s.nextPayday.format(DateTimeFormatter.ofPattern("d MMMM", locale))),
            modifier = Modifier.weight(1f),
        )
    }
    val tomorrow = s.tomorrowDailyCents
    if (tomorrow != null || settings.reserveCents > 0) {
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (tomorrow != null) {
                    Text(
                        stringResource(R.string.tomorrow_hint, money(tomorrow)),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (settings.reserveCents > 0) {
                    Text(
                        stringResource(R.string.reserve_hint, money(settings.reserveCents)),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Welcome(onStart: () -> Unit, onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.welcome_title), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.welcome_text),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.welcome_hint),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroEnd),
                ) { Text(stringResource(R.string.welcome_button), fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = onSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) { Text(stringResource(R.string.settings)) }
            }
        }
    }
}

@Composable
private fun Actions(onBalance: () -> Unit, onExpense: () -> Unit, onIncome: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onBalance,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.update_balance), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = onExpense,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.expense))
            }
            FilledTonalButton(
                onClick = onIncome,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.income))
            }
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, onDelete: () -> Unit) {
    val category = Category.of(entry.category)
    val (icon, color) = when (entry.type) {
        EntryType.BALANCE -> Icons.Filled.Edit to MaterialTheme.colorScheme.primary
        EntryType.EXPENSE -> Icons.Filled.ShoppingCart to Negative
        EntryType.INCOME -> Icons.Filled.Add to Positive
    }
    val label = when {
        entry.note.isNotBlank() -> entry.note
        category != null -> stringResource(category.label)
        entry.type == EntryType.BALANCE -> stringResource(R.string.balance_updated)
        entry.type == EntryType.EXPENSE -> stringResource(R.string.expense)
        else -> stringResource(R.string.income)
    }
    val amount = when (entry.type) {
        EntryType.BALANCE -> money(entry.cents)
        EntryType.EXPENSE -> "− " + money(entry.cents)
        EntryType.INCOME -> "+ " + money(entry.cents)
    }
    val date = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM · HH:mm", LocalAppLocale.current))
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                if (category != null) Text(category.emoji, fontSize = 18.sp)
                else Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                val sub = if (entry.excluded) "$date · ${stringResource(R.string.not_in_daily_budget)}" else date
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                amount,
                fontWeight = FontWeight.SemiBold,
                color = if (entry.type == EntryType.BALANCE) MaterialTheme.colorScheme.onSurface else color,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountDialog(
    type: EntryType,
    initial: Long?,
    onDismiss: () -> Unit,
    onConfirm: (cents: Long, note: String, category: String?, excluded: Boolean) -> Unit,
) {
    val m = LocalMoney.current
    var text by remember { mutableStateOf(initial?.let { m.editable(it) } ?: "") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var excluded by remember { mutableStateOf(false) }
    val cents = Budget.parseCents(text, allowNegative = type == EntryType.BALANCE)
    val focus = remember { FocusRequester() }
    val autoFocus = LocalAutoFocus.current
    LaunchedEffect(Unit) {
        if (!autoFocus) return@LaunchedEffect
        // Attendre une frame que le champ soit attaché, sinon requestFocus() lève une exception.
        withFrameNanos { }
        runCatching { focus.requestFocus() }
    }

    val (title, hint) = when (type) {
        EntryType.BALANCE -> R.string.balance_title to R.string.balance_hint
        EntryType.EXPENSE -> R.string.expense_title to R.string.expense_hint
        EntryType.INCOME -> R.string.income_title to R.string.income_hint
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    suffix = { Text(m.symbol) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.End),
                    isError = text.isNotBlank() && cents == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                )
                if (type == EntryType.EXPENSE) {
                    Text(
                        stringResource(R.string.category_optional),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Category.entries.forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = {
                                    category = if (category == c) null else c
                                    excluded = category?.fixed ?: false
                                },
                                label = { Text("${c.emoji} ${stringResource(c.label)}") },
                            )
                        }
                    }
                }
                if (type == EntryType.EXPENSE || (type == EntryType.BALANCE && initial != null)) {
                    ToggleRow(
                        title = stringResource(if (type == EntryType.EXPENSE) R.string.exclude_expense else R.string.exclude_balance),
                        help = stringResource(if (type == EntryType.EXPENSE) R.string.exclude_expense_help else R.string.exclude_balance_help),
                        checked = excluded,
                        onChange = { excluded = it },
                    )
                }
                if (type != EntryType.BALANCE) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.note_optional)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { cents?.let { onConfirm(it, note.trim(), category?.key, excluded) } },
                enabled = cents != null,
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ToggleRow(title: String, help: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(settings: Settings, onDismiss: () -> Unit, onSave: (Settings) -> Unit) {
    val locale = LocalAppLocale.current
    var payday by remember { mutableStateOf(settings.payday.toString()) }
    var currency by remember { mutableStateOf(settings.currency) }
    val m = remember(currency, locale) { Money(currency, locale) }
    var reserve by remember { mutableStateOf(if (settings.reserveCents == 0L) "" else m.editable(settings.reserveCents)) }
    var pickCurrency by remember { mutableStateOf(false) }
    val paydayValue = payday.trim().toIntOrNull()?.takeIf { it in 1..31 }
    val reserveValue = if (reserve.isBlank()) 0L else Budget.parseCents(reserve)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    onClick = { pickCurrency = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = CardDefaults.outlinedCardBorder(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${m.currency.currencyCode} · ${m.currency.getDisplayName(locale)}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                OutlinedTextField(
                    value = payday,
                    onValueChange = { payday = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.payday_label)) },
                    supportingText = { Text(stringResource(R.string.payday_help)) },
                    isError = paydayValue == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reserve,
                    onValueChange = { reserve = it },
                    label = { Text(stringResource(R.string.reserve_label)) },
                    supportingText = { Text(stringResource(R.string.reserve_help)) },
                    suffix = { Text(m.symbol) },
                    isError = reserveValue == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (paydayValue != null && reserveValue != null) onSave(Settings(paydayValue, reserveValue, currency))
                },
                enabled = paydayValue != null && reserveValue != null,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    if (pickCurrency) {
        CurrencyDialog(
            selected = currency,
            onDismiss = { pickCurrency = false },
            onPick = {
                currency = it
                pickCurrency = false
            },
        )
    }
}

@Composable
private fun CurrencyDialog(selected: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val locale = LocalAppLocale.current
    var query by remember { mutableStateOf("") }
    val all = remember { Money.all() }
    val shown = all.filter {
        query.isBlank() || it.currencyCode.contains(query.trim(), ignoreCase = true) ||
            it.getDisplayName(locale).contains(query.trim(), ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.currencyCode }) { c ->
                        val isSelected = c.currencyCode == selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onPick(c.currencyCode) }
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                c.currencyCode,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(52.dp),
                            )
                            Text(c.getDisplayName(locale), modifier = Modifier.weight(1f), maxLines = 1)
                            Text(c.getSymbol(locale), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (c.currencyCode == Money.popular.last() && query.isBlank()) {
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
