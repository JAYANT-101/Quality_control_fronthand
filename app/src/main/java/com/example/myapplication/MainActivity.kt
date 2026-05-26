package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.work.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InspectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainTabletLayout()
                }
            }
        }
    }
}

@Composable
fun InspectionTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF03DAC6),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        error = Color(0xFFCF6679)
    )

    val typography = Typography(
        bodyLarge = androidx.compose.ui.text.TextStyle(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.5.sp
        ),
        headlineMedium = androidx.compose.ui.text.TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = typography,
        content = content
    )
}

@Composable
fun MainTabletLayout() {
    var selectedLine by remember { mutableIntStateOf(1) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.apiDao()

    val tasksFlow = remember { dao.getAllTasksFlow() }
    val tasks by tasksFlow.collectAsState(initial = emptyList<TaskEntity>())

    LaunchedEffect(tasks) {
        // Ensure we have the full set of dummy tasks with multi-color options
        if (tasks.isEmpty()) {
            val dummyTasks = listOf(
                TaskEntity("PO-2023-0045", "Cotton Jersey", "Deep Navy", 2),
                TaskEntity("PO-2023-0046", "Cotton Jersey", "Pure White", 400),
                TaskEntity("PO-2023-0047", "Pique", "Crimson Red", 300),
                TaskEntity("PO-2023-0048", "Pique", "Midnight Black", 350),
                TaskEntity("PO-2023-0049", "Interlock", "Forest Green", 450),
                TaskEntity("PO-2023-0050", "Fleece", "Pitch Black", 200)
            )
            dao.insertTasks(dummyTasks)
        }
    }

    fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    fun onSaveInspection(result: String, defectType: String?, po: String) {
        scope.launch {
            dao.insertInspection(
                InspectionEntity(
                    task_id = po,
                    line_no = selectedLine,
                    result = result,
                    defect_type = defectType
                )
            )
            triggerSync()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Pane (30% width)
        Box(modifier = Modifier.fillMaxHeight().weight(0.3f)) {
            NavigationRailLayout(
                selectedLine = selectedLine,
                onLineSelected = { selectedLine = it }
            )
        }

        // Right Pane (70% width)
        Box(modifier = Modifier.fillMaxHeight().weight(0.7f)) {
            WorkArea(
                selectedLine = selectedLine,
                tasks = tasks,
                onSaveInspection = ::onSaveInspection
            )
        }
    }
}

@Composable
fun NavigationRailLayout(selectedLine: Int, onLineSelected: (Int) -> Unit) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "User",
                modifier = Modifier.size(48.dp).padding(top = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Inspector ID: 104",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            (1..6).forEach { line ->
                NavigationRailItem(
                    selected = selectedLine == line,
                    onClick = { onLineSelected(line) },
                    icon = {
                        Text(
                            text = "L$line",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    label = { Text("Line $line", fontSize = 16.sp) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun WorkArea(
    selectedLine: Int,
    tasks: List<TaskEntity>,
    onSaveInspection: (String, String?, String) -> Unit
) {
    var selectedCloth by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var selectedPO by remember { mutableStateOf<String?>(null) }

    var passCount by remember { mutableIntStateOf(0) }
    var alterCount by remember { mutableIntStateOf(0) }
    var rejectCount by remember { mutableIntStateOf(0) }
    var showDefectDialog by remember { mutableStateOf(false) }
    var showDoneDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var completedPO by remember { mutableStateOf("") }

    // Derived filters with remember for stability and correct cascading
    val activeTasks = remember(tasks) { tasks.filter { !it.is_completed } }
    val clothTypes = remember(activeTasks) { activeTasks.map { it.cloth_type }.distinct() }
    val colors = remember(activeTasks, selectedCloth) {
        if (selectedCloth == null) emptyList()
        else activeTasks.filter { it.cloth_type == selectedCloth }.map { it.color }.distinct()
    }
    val poNumbers = remember(activeTasks, selectedCloth, selectedColor) {
        if (selectedColor == null) emptyList()
        else activeTasks.filter { it.cloth_type == selectedCloth && it.color == selectedColor }
            .map { it.po_number }.distinct()
    }

    // Reset filters when line changes
    LaunchedEffect(selectedLine) {
        selectedCloth = null
        selectedColor = null
        selectedPO = null
    }

    val currentTask = tasks.find { it.po_number == selectedPO }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Line $selectedLine - Inspection",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                CounterDisplay("PASS", passCount, Color(0xFF4CAF50))
                CounterDisplay("ALTER", alterCount, Color(0xFFFFC107))
                CounterDisplay("REJECT", rejectCount, Color(0xFFF44336))
            }
        }

        Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.secondary)

        // 1. Cascading Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DropdownFilter(
                label = "Cloth Type",
                options = clothTypes,
                selectedOption = selectedCloth,
                onOptionSelected = {
                    selectedCloth = it
                    selectedColor = null
                    selectedPO = null
                },
                modifier = Modifier.weight(1f)
            )

            DropdownFilter(
                label = "Color",
                options = colors,
                selectedOption = selectedColor,
                onOptionSelected = {
                    selectedColor = it
                    selectedPO = null
                },
                enabled = selectedCloth != null,
                modifier = Modifier.weight(1f)
            )

            DropdownFilter(
                label = "PO Number",
                options = poNumbers,
                selectedOption = selectedPO,
                onOptionSelected = { selectedPO = it },
                enabled = selectedColor != null,
                modifier = Modifier.weight(1f)
            )
        }

        // PO Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Current Target:", currentTask?.target?.toString() ?: "N/A")
                if (selectedPO != null) {
                    InfoRow("Selected PO:", selectedPO!!)
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // 2. Action Panel (Massive Buttons)
        Box(modifier = Modifier.weight(0.6f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                LargeButton(
                    text = "PASS",
                    color = Color(0xFF4CAF50),
                    enabled = selectedPO != null && !showDoneDialog && !showCompletionDialog,
                    onClick = {
                        passCount++
                        onSaveInspection("PASS", null, selectedPO!!)
                        if (currentTask != null && passCount >= currentTask.target) {
                            completedPO = selectedPO!!
                            showCompletionDialog = true
                        } else {
                            showDoneDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                LargeButton(
                    text = "ALTER",
                    color = Color(0xFFFFC107), // Yellow
                    enabled = selectedPO != null && !showDoneDialog && !showCompletionDialog,
                    onClick = { showDefectDialog = true },
                    modifier = Modifier.weight(1f)
                )
                LargeButton(
                    text = "REJECT",
                    color = Color(0xFFF44336),
                    enabled = selectedPO != null && !showDoneDialog && !showCompletionDialog,
                    onClick = {
                        rejectCount++
                        onSaveInspection("REJECT", null, selectedPO!!)
                        showDoneDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDefectDialog) {
        DefectDialog(
            onDismiss = { showDefectDialog = false },
            onDefectSelected = { defect ->
                alterCount++
                onSaveInspection("ALTER", defect, selectedPO!!)
                showDefectDialog = false
                showDoneDialog = true
            }
        )
    }

    if (showDoneDialog) {
        DoneDialog(onDismiss = { showDoneDialog = false })
    }

    if (showCompletionDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val database = remember { AppDatabase.getDatabase(context) }
        val dao = database.apiDao()
        val scope = rememberCoroutineScope()

        CompletionDialog(
            poNumber = completedPO,
            onDismiss = {
                scope.launch {
                    dao.markTaskAsCompleted(completedPO)
                    // Trigger a refresh of the tasks list if needed, 
                    // or rely on the next recomposition if tasks is updated elsewhere.
                    // For now, resetting selection:
                    selectedPO = null
                    selectedColor = null
                    selectedCloth = null
                    passCount = 0
                    alterCount = 0
                    rejectCount = 0
                    showCompletionDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        // We wrap the TextField in a Box and use a transparent clickable overlay.
        // This is the most reliable way in Compose to have a field that looks like a 
        // TextField but behaves strictly as a dropdown button (preventing keyboard/focus).
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOption ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(label, fontSize = 18.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                enabled = enabled,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusManager.clearFocus()
                        }
                    },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            // Invisible overlay to intercept clicks and toggle the menu
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = enabled) {
                        expanded = !expanded
                    }
            )
        }

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No options available", color = Color.Gray) },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 20.sp) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        contentPadding = PaddingValues(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DefectDialog(onDismiss: () -> Unit, onDefectSelected: (String) -> Unit) {
    val defects = listOf("Stain", "Wrong Stitching", "Wrong Color", "Fabric Hole")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f).padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Select Defect Type",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(defects) { defect ->
                        Button(
                            onClick = { onDefectSelected(defect) },
                            modifier = Modifier.height(120.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                defect,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CANCEL", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun CompletionDialog(poNumber: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    Icons.Default.AccountCircle, // Placeholder for a completion icon
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "PO COMPLETED",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "PO Number: $poNumber",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CONTINUE", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DoneDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier.padding(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "DONE",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(800) // Show for 800ms
        onDismiss()
    }
}

@Composable
fun CounterDisplay(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(count.toString(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun LargeButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = MaterialTheme.shapes.large,
        enabled = enabled
    ) {
        Text(text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun PreviewMainTabletLayout() {
    val dummyTasks = listOf(
        TaskEntity("PO-2023-0045", "Cotton Jersey", "Deep Navy", 500),
        TaskEntity("PO-2023-0046", "Pique", "Crimson Red", 300)
    )
    InspectionTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxHeight().weight(0.3f)) {
                    NavigationRailLayout(selectedLine = 1, onLineSelected = {})
                }
                Box(modifier = Modifier.fillMaxHeight().weight(0.7f)) {
                    WorkArea(
                        selectedLine = 1,
                        tasks = dummyTasks,
                        onSaveInspection = { _, _, _ -> }
                    )
                }
            }
        }
    }
}
