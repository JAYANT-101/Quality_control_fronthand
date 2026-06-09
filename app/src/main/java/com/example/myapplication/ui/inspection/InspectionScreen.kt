package com.example.myapplication.ui.inspection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabletLayout(userSession: UserSession) {
    var selectedLine by remember { mutableIntStateOf(1) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.apiDao()

    val tasksFlow = remember { dao.getAllTasksFlow() }
    val allTasks by tasksFlow.collectAsState(initial = emptyList())
    val activeTasks = allTasks.filter { !it.is_completed }

    LaunchedEffect(allTasks) {
        if (allTasks.isEmpty()) {
            val dummyTasks = listOf(
                TaskEntity("PO-2023-0045", "Cotton Jersey", "Deep Navy", 2),
                TaskEntity("PO-2023-0046", "Cotton Jersey", "Pure White", 15),
                TaskEntity("PO-2023-0047", "Pique", "Crimson Red", 300),
                TaskEntity("PO-2023-0048", "Pique", "Midnight Black", 350),
                TaskEntity("PO-2023-0049", "Interlock", "Forest Green", 450),
                TaskEntity("PO-2023-0050", "Fleece", "Pitch Black", 200)
            )
            dao.insertTasks(dummyTasks)
        }
    }

    fun onSaveInspection(result: String, defectType: String?, po: String) {
        scope.launch {
            dao.insertInspection(
                InspectionEntity(
                    task_id = po,
                    line_no = selectedLine,
                    result = result,
                    defect_type = defectType,
                    checker_id = userSession.userId
                )
            )
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxHeight().width(120.dp)) {
            NavigationRailLayout(
                selectedLine = selectedLine,
                onLineSelected = { selectedLine = it },
                userName = userSession.username,
                userId = userSession.userId
            )
        }

        Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
            WorkArea(
                selectedLine = selectedLine,
                tasks = activeTasks,
                dao = dao,
                onSaveInspection = ::onSaveInspection,
                onResetData = {
                    scope.launch {
                        dao.resetAllTasks()
                        dao.deleteAllInspections()
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationRailLayout(
    selectedLine: Int,
    onLineSelected: (Int) -> Unit,
    userName: String,
    userId: Int
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = Color(0xFF1A1A1A),
        header = {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "User",
                modifier = Modifier.size(48.dp).padding(top = 16.dp),
                tint = Color(0xFFBB86FC)
            )
            Text(
                userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "ID: $userId",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    ) {
        (1..6).forEach { line ->
            val isSelected = selectedLine == line
            NavigationRailItem(
                selected = isSelected,
                onClick = { onLineSelected(line) },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFF333333),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "L$line",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Text(
                            "Line $line",
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WorkArea(
    selectedLine: Int,
    tasks: List<TaskEntity>,
    dao: ApiDao,
    onSaveInspection: (String, String?, String) -> Unit,
    onResetData: () -> Unit
) {
    var selectedPO by remember { mutableStateOf<String?>(null) }
    var selectedFabric by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableIntStateOf(0) }

    val passCount by if (selectedPO != null) {
        dao.getInspectionCountFlow(selectedPO!!, selectedLine, "PASS").collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }
    val alterCount by if (selectedPO != null) {
        dao.getInspectionCountFlow(selectedPO!!, selectedLine, "ALTER").collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }
    val rejectCount by if (selectedPO != null) {
        dao.getInspectionCountFlow(selectedPO!!, selectedLine, "REJECT").collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    var showDefectDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showDoneDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Completion Check
    LaunchedEffect(passCount) {
        if (selectedPO != null && quantity > 0 && passCount >= quantity) {
            dao.markTaskAsCompleted(selectedPO!!)
            showCompletionDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Line $selectedLine - Inspection",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFBB86FC),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Reset All Data",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onResetData() }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                CounterHeader("PASS", passCount, Color(0xFF4CAF50))
                CounterHeader("ALTER", alterCount, Color(0xFFFFB300))
                CounterHeader("REJECT", rejectCount, Color(0xFFF44336))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color(0xFF00BFA5), thickness = 2.dp)
        Spacer(modifier = Modifier.height(24.dp))

        // Filters
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DropdownFilter(
                label = "Cloth Type",
                options = tasks.map { it.cloth_type }.distinct(),
                selectedOption = selectedFabric,
                onOptionSelected = { selectedFabric = it },
                modifier = Modifier.weight(1f)
            )
            DropdownFilter(
                label = "Color",
                options = tasks.map { it.color }.distinct(),
                selectedOption = selectedColor,
                onOptionSelected = { selectedColor = it },
                modifier = Modifier.weight(1f)
            )
            DropdownFilter(
                label = "PO Number",
                options = tasks.map { it.po_number },
                selectedOption = selectedPO,
                onOptionSelected = { po ->
                    selectedPO = po
                    val task = tasks.find { it.po_number == po }
                    selectedFabric = task?.cloth_type
                    selectedColor = task?.color
                    quantity = task?.target ?: 0
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                InfoRow("Current Target:", quantity.toString(), Color.Gray, Color(0xFF00BFA5))
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Selected PO:", selectedPO ?: "Not Selected", Color.Gray, Color(0xFF00BFA5))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Big Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ActionButton(
                text = "PASS",
                color = Color(0xFF4CAF50),
                onClick = {
                    selectedPO?.let {
                        onSaveInspection("PASS", null, it)
                        if (passCount + 1 < quantity) {
                            showDoneDialog = true
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedPO != null
            )
            ActionButton(
                text = "ALTER",
                color = Color(0xFFFFB300),
                onClick = {
                    if (selectedPO != null) {
                        showDefectDialog = true
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedPO != null
            )
            ActionButton(
                text = "REJECT",
                color = Color(0xFFF44336),
                onClick = {
                    selectedPO?.let {
                        onSaveInspection("REJECT", null, it)
                        showDoneDialog = true
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedPO != null
            )
        }
    }

    if (showDefectDialog) {
        DefectDialog(
            onDismiss = { showDefectDialog = false },
            onDefectSelected = { defect ->
                selectedPO?.let {
                    onSaveInspection("ALTER", defect, it)
                }
                showDefectDialog = false
                showDoneDialog = true
            }
        )
    }

    if (showCompletionDialog) {
        CompletionDialog(
            poNumber = selectedPO ?: "",
            onDismiss = {
                showCompletionDialog = false
                selectedPO = null
                selectedFabric = null
                selectedColor = null
                quantity = 0
            }
        )
    }

    if (showDoneDialog && !showCompletionDialog) {
        DoneDialog(onDismiss = { showDoneDialog = false })
    }
}

@Composable
fun CounterHeader(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun InfoRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = labelColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large,
        enabled = enabled
    ) {
        Text(text, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
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

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.Gray) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = Color.Gray,
                focusedBorderColor = Color.White,
                unfocusedLabelColor = Color.Gray,
                focusedLabelColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF333333))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DefectDialog(onDismiss: () -> Unit, onDefectSelected: (String) -> Unit) {
    val defects = listOf("Stain", "Hole", "Shading", "Skewing", "Measurement", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Defect Type", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(defects) { defect ->
                        Button(
                            onClick = { onDefectSelected(defect) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679), contentColor = Color.White)
                        ) {
                            Text(defect, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionDialog(poNumber: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Po Completed!", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("PO: $poNumber has been marked as completed.", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun DoneDialog(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(240.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "DONE",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun PreviewMainTabletLayout() {
    MaterialTheme {
        MainTabletLayout(userSession = UserSession(104, "checker"))
    }
}
