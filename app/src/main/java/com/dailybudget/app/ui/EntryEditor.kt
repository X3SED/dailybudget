package com.dailybudget.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailybudget.app.Budget
import com.dailybudget.app.Entry
import com.dailybudget.app.EntryType
import com.dailybudget.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Ce que l'écran d'édition doit ouvrir : une nouvelle saisie d'un type donné, ou une saisie existante. */
data class EditorRequest(val type: EntryType, val entry: Entry? = null)

/**
 * Écran plein écran pour ajouter ou modifier une saisie (dépense, revenu, solde) :
 * montant, date, catégorie, dépense fixe, note, suppression.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryEditor(
    request: EditorRequest,
    currentBalance: Long?,
    onClose: () -> Unit,
    onSave: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
) {
    val m = LocalMoney.current
    val locale = LocalAppLocale.current
    val zone = ZoneId.systemDefault()
    val original = request.entry
    val originalDateTime = original?.let { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDateTime() }
    val today = LocalDate.now()

    var type by remember { mutableStateOf(original?.type ?: request.type) }
    var text by remember {
        mutableStateOf(
            when {
                original != null -> m.editable(original.cents)
                request.type == EntryType.BALANCE && currentBalance != null -> m.editable(currentBalance)
                else -> ""
            }
        )
    }
    var note by remember { mutableStateOf(original?.note ?: "") }
    var category by remember { mutableStateOf(Category.of(original?.category)) }
    var excluded by remember { mutableStateOf(original?.excluded ?: false) }
    var date by remember { mutableStateOf(originalDateTime?.toLocalDate() ?: today) }
    var pickDate by remember { mutableStateOf(false) }
    val cents = Budget.parseCents(text, allowNegative = type == EntryType.BALANCE)

    BackHandler(onBack = onClose)

    val focus = remember { FocusRequester() }
    val autoFocus = LocalAutoFocus.current && original == null
    LaunchedEffect(Unit) {
        if (!autoFocus) return@LaunchedEffect
        // Attendre une frame que le champ soit attaché, sinon requestFocus() lève une exception.
        withFrameNanos { }
        runCatching { focus.requestFocus() }
    }

    fun save() {
        val value = cents ?: return
        // On garde l'heure d'origine ; une nouvelle saisie du jour prend l'heure actuelle, une date passée midi.
        val time = when {
            originalDateTime != null -> originalDateTime.toLocalTime()
            date == today -> LocalTime.now()
            else -> LocalTime.NOON
        }
        onSave(
            Entry(
                id = original?.id ?: System.currentTimeMillis(),
                type = type,
                cents = value,
                timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli(),
                note = if (type == EntryType.BALANCE) "" else note.trim(),
                category = if (type == EntryType.EXPENSE) category?.key else null,
                excluded = excluded && type != EntryType.INCOME,
            )
        )
    }

    val title = when {
        original == null && type == EntryType.EXPENSE -> R.string.expense_title
        original == null && type == EntryType.INCOME -> R.string.income_title
        original == null -> R.string.balance_title
        type == EntryType.EXPENSE -> R.string.edit_expense
        type == EntryType.INCOME -> R.string.edit_income
        else -> R.string.edit_balance
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    TextButton(onClick = ::save, enabled = cents != null) { Text(stringResource(R.string.save)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val types = listOf(
                EntryType.EXPENSE to R.string.expense,
                EntryType.INCOME to R.string.income,
                EntryType.BALANCE to R.string.type_balance,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                types.forEachIndexed { i, (t, label) ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = {
                            type = t
                            if (t != EntryType.EXPENSE) category = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, types.size),
                    ) { Text(stringResource(label), maxLines = 1) }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                label = {
                    Text(
                        stringResource(
                            when (type) {
                                EntryType.BALANCE -> R.string.balance_hint
                                EntryType.EXPENSE -> R.string.expense_hint
                                EntryType.INCOME -> R.string.income_hint
                            }
                        )
                    )
                },
                singleLine = true,
                suffix = { Text(m.symbol) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.End),
                isError = text.isNotBlank() && cents == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            )

            Section(stringResource(R.string.date)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = date == today,
                        onClick = { date = today },
                        label = { Text(stringResource(R.string.today)) },
                    )
                    FilterChip(
                        selected = date == today.minusDays(1),
                        onClick = { date = today.minusDays(1) },
                        label = { Text(stringResource(R.string.yesterday)) },
                    )
                    val other = date.isBefore(today.minusDays(1))
                    FilterChip(
                        selected = other,
                        onClick = { pickDate = true },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = {
                            Text(
                                if (other) date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
                                else stringResource(R.string.other_date),
                                maxLines = 1,
                            )
                        },
                    )
                }
            }

            if (type == EntryType.EXPENSE) {
                Section(stringResource(R.string.category_optional)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Category.entries.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { c ->
                                    FilterChip(
                                        selected = category == c,
                                        onClick = {
                                            category = if (category == c) null else c
                                            excluded = category?.fixed ?: false
                                        },
                                        label = {
                                            Text(
                                                "${c.emoji}  ${stringResource(c.label)}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (type != EntryType.INCOME) {
                val isExpense = type == EntryType.EXPENSE
                ToggleRow(
                    title = stringResource(if (isExpense) R.string.exclude_expense else R.string.exclude_balance),
                    help = stringResource(if (isExpense) R.string.exclude_expense_help else R.string.exclude_balance_help),
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

            Button(
                onClick = ::save,
                enabled = cents != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold) }

            if (original != null) {
                OutlinedButton(
                    onClick = { onDelete(original) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.delete))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (pickDate) {
        // Le DatePicker travaille en millisecondes UTC à minuit ; on interdit les dates futures.
        val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
internal fun ToggleRow(title: String, help: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
