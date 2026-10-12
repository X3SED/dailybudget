package com.dailybudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.dailybudget.app.Settings
import com.dailybudget.app.Store
import com.dailybudget.app.Summary
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Fr = Locale.FRANCE
private val money: NumberFormat = NumberFormat.getCurrencyInstance(Fr)
private val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Fr)
private val entryFormat = DateTimeFormatter.ofPattern("d MMM · HH:mm", Fr)

fun formatCents(cents: Long): String = money.format(cents / 100.0)

private fun editableCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = Math.abs(cents)
    return if (abs % 100 == 0L) "$sign${abs / 100}" else "$sign${abs / 100},${"%02d".format(abs % 100)}"
}

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

    val summary = Budget.summarize(entries, settings, today)

    fun add(type: EntryType, cents: Long, note: String) {
        val now = System.currentTimeMillis()
        entries = entries + Entry(id = now, type = type, cents = cents, timestamp = now, note = note)
        store.saveEntries(entries)
        today = LocalDate.now()
    }

    fun delete(entry: Entry) {
        entries = entries.filterNot { it.id == entry.id }
        store.saveEntries(entries)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Budget du jour", fontWeight = FontWeight.Bold)
                        Text(
                            today.format(dayFormat).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Réglages")
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
                if (summary == null) Welcome(onStart = { input = EntryType.BALANCE })
                else Hero(summary)
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
                        "Historique",
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
            onConfirm = { cents, note ->
                add(type, cents, note)
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
                if (over) "Dépassement aujourd'hui" else "Tu peux encore dépenser aujourd'hui",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatCents(if (over) -s.remainingTodayCents else s.remainingTodayCents),
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 50.sp,
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
                    "Dépensé : ${formatCents(s.spentTodayCents.coerceAtLeast(0))}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Budget : ${formatCents(s.dailyCents)} / jour",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun Stats(s: Summary, settings: Settings) = Column {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = "Solde du compte",
            value = formatCents(s.balanceCents),
            valueColor = if (s.balanceCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Avant le salaire",
            value = if (s.daysLeft == 1) "1 jour" else "${s.daysLeft} jours",
            sub = "le " + s.nextPayday.format(DateTimeFormatter.ofPattern("d MMMM", Fr)),
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
                        "Si tu ne dépenses plus rien aujourd'hui, tu auras ${formatCents(tomorrow)} par jour à partir de demain.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (settings.reserveCents > 0) {
                    Text(
                        "${formatCents(settings.reserveCents)} mis de côté ne sont pas comptés.",
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
private fun Welcome(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bienvenue 👋", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Indique combien il te reste sur ton compte et l'app calcule combien tu peux dépenser chaque jour jusqu'à ta prochaine paie.",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Pense à régler ton jour de paie dans les réglages ⚙️",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroEnd),
            ) { Text("Entrer mon solde", fontWeight = FontWeight.SemiBold) }
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
            Text("Mettre à jour mon solde", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = onExpense,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Dépense")
            }
            FilledTonalButton(
                onClick = onIncome,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Revenu")
            }
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, onDelete: () -> Unit) {
    val (icon, label, amount, color) = when (entry.type) {
        EntryType.BALANCE -> Quad(Icons.Filled.Edit, "Solde mis à jour", formatCents(entry.cents), MaterialTheme.colorScheme.primary)
        EntryType.EXPENSE -> Quad(Icons.Filled.ShoppingCart, "Dépense", "− " + formatCents(entry.cents), Negative)
        EntryType.INCOME -> Quad(Icons.Filled.Add, "Revenu", "+ " + formatCents(entry.cents), Positive)
    }
    val date = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(entryFormat)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.note.ifBlank { label }, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(amount, fontWeight = FontWeight.SemiBold, color = if (entry.type == EntryType.BALANCE) MaterialTheme.colorScheme.onSurface else color)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

private data class Quad(val icon: ImageVector, val label: String, val amount: String, val color: Color)

@Composable
private fun AmountDialog(
    type: EntryType,
    initial: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit,
) {
    var text by remember { mutableStateOf(initial?.let { editableCents(it) } ?: "") }
    var note by remember { mutableStateOf("") }
    val cents = Budget.parseCents(text, allowNegative = type == EntryType.BALANCE)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val (title, hint) = when (type) {
        EntryType.BALANCE -> "Mon solde actuel" to "Combien reste-t-il sur ton compte ?"
        EntryType.EXPENSE -> "Nouvelle dépense" to "Montant dépensé"
        EntryType.INCOME -> "Nouveau revenu" to "Salaire, remboursement…"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    suffix = { Text("€") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.End),
                    isError = text.isNotBlank() && cents == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = if (type == EntryType.BALANCE) ImeAction.Done else ImeAction.Next,
                    ),
                )
                if (type != EntryType.BALANCE) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Note (facultatif)") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { cents?.let { onConfirm(it, note.trim()) } }, enabled = cents != null) {
                Text("Valider")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun SettingsDialog(settings: Settings, onDismiss: () -> Unit, onSave: (Settings) -> Unit) {
    var payday by remember { mutableStateOf(settings.payday.toString()) }
    var reserve by remember { mutableStateOf(if (settings.reserveCents == 0L) "" else editableCents(settings.reserveCents)) }
    val paydayValue = payday.trim().toIntOrNull()?.takeIf { it in 1..31 }
    val reserveValue = if (reserve.isBlank()) 0L else Budget.parseCents(reserve)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réglages") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = payday,
                    onValueChange = { payday = it.filter(Char::isDigit).take(2) },
                    label = { Text("Jour de paie (1 à 31)") },
                    supportingText = { Text("Le budget est calculé jusqu'à la veille de ce jour.") },
                    isError = paydayValue == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reserve,
                    onValueChange = { reserve = it },
                    label = { Text("Somme à garder de côté") },
                    supportingText = { Text("Facultatif : exclue du budget journalier.") },
                    suffix = { Text("€") },
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
                    if (paydayValue != null && reserveValue != null) onSave(Settings(paydayValue, reserveValue))
                },
                enabled = paydayValue != null && reserveValue != null,
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
