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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import kotlin.time.Duration.Companion.milliseconds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.*
import com.example.myapplication.repository.InspectionRepository
import com.example.myapplication.repository.PoRepository
import com.example.myapplication.repository.CheckerOutputRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabletLayout(
    userSession: UserSession,
    inspectionRepository: InspectionRepository,
    poRepository: PoRepository,
    checkerOutputRepository: CheckerOutputRepository,
    onLogout: () -> Unit,
) {
    var selectedLine by remember { mutableIntStateOf(1) }

    val scope = rememberCoroutineScope()

    var isSubmitting by remember { mutableStateOf(false) }

    fun onSaveInspection(result: String, defectType: String, po: String, poId: Int, onSuccess: (PoProgress) -> Unit) {
        if (isSubmitting) return
        isSubmitting = true
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        scope.launch {
            // 1. Submit to backend
            val request = CheckerOutputRequest(
                userId = userSession.userId,
                line = selectedLine,
                poId = poId,
                fieldName = result.lowercase(), // "pass", "reject", or "alter"
                defectName = defectType,
                actualEventTime = timestamp,
            )
            
            checkerOutputRepository.submitCheckerOutput(request)
                .onSuccess { response ->
                    // 2. Save locally only after backend success
                    inspectionRepository.saveInspection(
                        InspectionEntity(
                            taskId = po,
                            lineNo = selectedLine,
                            result = result,
                            defectType = defectType.ifEmpty { null },
                            checkerId = userSession.userId,
                        ),
                    )
                    
                    // 3. Trigger UI update with fresh PO progress
                    onSuccess(response.po)
                    isSubmitting = false
                }
                .onFailure { error ->
                    // Handle failure if needed (e.g., show a toast or error message)
                    println("InspectionScreen: Failed to submit checker output: ${error.message}")
                    isSubmitting = false
                }
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxHeight().width(120.dp)) {
            NavigationRailLayout(
                selectedLine = selectedLine,
                onLineSelected = { selectedLine = it },
                userName = userSession.username,
                userId = userSession.userId,
                onLogout = onLogout,
            )
        }

        Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
            WorkArea(
                selectedLine = selectedLine,
                inspectionRepository = inspectionRepository,
                poRepository = poRepository,
                onSaveInspection = ::onSaveInspection,
                isSubmitting = isSubmitting,
            ) {
                scope.launch {
                    inspectionRepository.resetAllCountsAndTasks()
                }
            }
        }
    }
}

@Composable
fun NavigationRailLayout(
    selectedLine: Int,
    onLineSelected: (Int) -> Unit,
    userName: String,
    userId: Int,
    onLogout: () -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = Color(0xFF1A1A1A),
        header = {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "User",
                modifier = Modifier.size(48.dp).padding(top = 16.dp),
                tint = Color(0xFFBB86FC),
            )
            Text(
                userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "ID: $userId",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        },
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
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "L$line",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                )
                            }
                        }
                        Text(
                            "Line $line",
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        NavigationRailItem(
            selected = false,
            onClick = onLogout,
            icon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color.Gray,
                    )
                    Text(
                        "Logout",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            colors = NavigationRailItemDefaults.colors(
                indicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun WorkArea(
    selectedLine: Int,
    inspectionRepository: InspectionRepository,
    poRepository: PoRepository,
    onSaveInspection: (result: String, defectType: String, po: String, poId: Int, onSuccess: (PoProgress) -> Unit) -> Unit,
    isSubmitting: Boolean,
    onResetData: () -> Unit,
) {
    var productTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedProductType by remember { mutableStateOf<String?>(null) }
    var poNumbers by remember { mutableStateOf<List<PoNumberItem>>(emptyList()) }
    var selectedPoNumber by remember { mutableStateOf<String?>(null) }
    var selectedPoTarget by remember { mutableStateOf<Int?>(null) }
    var selectedPoId by remember { mutableStateOf<Int?>(null) }

    var isLoadingProductTypes by remember { mutableStateOf(value = false) }
    var isLoadingPoNumbers by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedProductType) {
        if (selectedProductType != null) {
            isLoadingPoNumbers = true
            selectedPoNumber = null
            selectedPoTarget = null
            selectedPoId = null

            poRepository.fetchPoNumbers(selectedProductType!!)
                .onSuccess {
                    poNumbers = it
                    errorMessage = null
                }
                .onFailure {
                    errorMessage = it.message
                    poNumbers = emptyList()
                }
            isLoadingPoNumbers = false
        }
    }

    LaunchedEffect(selectedPoNumber, selectedLine) {
        if (selectedPoNumber != null) {
            inspectionRepository.clearCountsForPo(selectedPoNumber!!, selectedLine)
        }
    }

    val passCount by if (selectedPoNumber != null) {
        inspectionRepository.getCount(selectedPoNumber!!, selectedLine, "PASS").collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }
    val alterCount by if (selectedPoNumber != null) {
        inspectionRepository.getCount(selectedPoNumber!!, selectedLine, "ALTER").collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }
    val rejectCount by if (selectedPoNumber != null) {
        inspectionRepository.getCount(selectedPoNumber!!, selectedLine, "REJECT").collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    var showDefectDialog by remember { mutableStateOf(value = false) }
    var showCompletionDialog by remember { mutableStateOf(value = false) }
    var showDoneDialog by remember { mutableStateOf(value = false) }
    var doneDialogColor by remember { mutableStateOf(Color(0xFF4CAF50)) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Line $selectedLine - Inspection",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFBB86FC),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Reset All Data",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onResetData() },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                CounterHeader("PASS", passCount, Color(0xFF4CAF50))
                CounterHeader("ALTER", alterCount, Color(0xFFFFB300))
                CounterHeader("REJECT", rejectCount, Color(0xFFF44336))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF00BFA5), thickness = 2.dp)
        
        if (errorMessage != null) {
            Text(
                errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Filters
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DropdownFilter(
                label = "Product Type",
                options = productTypes,
                selectedOption = selectedProductType,
                onOptionSelected = { selectedProductType = it },
                modifier = Modifier.weight(1f),
                enabled = !isLoadingProductTypes,
                onClick = {
                    scope.launch {
                        isLoadingProductTypes = true
                        poRepository.fetchProductTypes()
                            .onSuccess {
                                productTypes = it
                                errorMessage = null
                            }
                            .onFailure {
                                errorMessage = it.message
                            }
                        isLoadingProductTypes = false
                    }
                },
            )
            DropdownFilter(
                label = "PO Number",
                options = poNumbers.map { "${it.poNumber} (target: ${it.target})" },
                selectedOption = if (selectedPoNumber != null) "$selectedPoNumber (target: $selectedPoTarget)" else null,
                onOptionSelected = { selectedLabel ->
                    val selectedItem = poNumbers.find { "${it.poNumber} (target: ${it.target})" == selectedLabel }
                    selectedPoNumber = selectedItem?.poNumber
                    selectedPoTarget = selectedItem?.target
                    selectedPoId = selectedItem?.poId
                },
                modifier = Modifier.weight(1f),
                enabled = (selectedProductType != null) && (!isLoadingPoNumbers),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                InfoRow("Remaining Target:", (selectedPoTarget ?: 0).toString(), Color.Gray, Color(0xFF00BFA5))
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Selected PO:", selectedPoNumber ?: "Not Selected", Color.Gray, Color(0xFF00BFA5))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Big Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val passColor = Color(0xFF4CAF50)
            val alterColor = Color(0xFFFFB300)
            val rejectColor = Color(0xFFF44336)

            val buttonsEnabled = (selectedPoNumber != null) && 
                                (selectedPoId != null) && 
                                !showDoneDialog && 
                                !showDefectDialog && 
                                !showCompletionDialog &&
                                !isSubmitting

            ActionButton(
                text = "PASS",
                color = passColor,
                onClick = {
                    if (buttonsEnabled) {
                        onSaveInspection("PASS", "", selectedPoNumber!!, selectedPoId!!) { poProgress ->
                            selectedPoTarget = poProgress.remainingTarget
                            if (poProgress.completed) {
                                showCompletionDialog = true
                            } else {
                                doneDialogColor = passColor
                                showDoneDialog = true
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = buttonsEnabled,
            )
            ActionButton(
                text = "ALTER",
                color = alterColor,
                onClick = {
                    if (buttonsEnabled) {
                        showDefectDialog = true
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = buttonsEnabled,
            )
            ActionButton(
                text = "REJECT",
                color = rejectColor,
                onClick = {
                    if (buttonsEnabled) {
                        onSaveInspection("REJECT", "", selectedPoNumber!!, selectedPoId!!) { poProgress ->
                            selectedPoTarget = poProgress.remainingTarget
                            doneDialogColor = rejectColor
                            showDoneDialog = true
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = buttonsEnabled,
            )
        }
    }

    if (showDefectDialog) {
        DefectDialog(
            onDismiss = { showDefectDialog = false },
        ) { defect ->
            if ((selectedPoNumber != null) && (selectedPoId != null)) {
                onSaveInspection("ALTER", defect, selectedPoNumber!!, selectedPoId!!) { poProgress ->
                    selectedPoTarget = poProgress.remainingTarget
                    showDefectDialog = false
                    doneDialogColor = Color(0xFFFFB300) // ALTER color
                    showDoneDialog = true
                }
            }
        }
    }

    if (showCompletionDialog) {
        CompletionDialog(
            poNumber = selectedPoNumber ?: "",
        ) {
            showCompletionDialog = false
            selectedProductType = null
            selectedPoNumber = null
            selectedPoTarget = null
            selectedPoId = null
        }
    }

    if (showDoneDialog && !showCompletionDialog) {
        DoneDialog(
            color = doneDialogColor,
        ) {
            showDoneDialog = false
        }
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
            disabledContainerColor = color.copy(alpha = 0.5f),
        ),
        shape = MaterialTheme.shapes.large,
        enabled = enabled,
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
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(value = false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedOption ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.Gray) },
            trailingIcon = { 
                IconButton(onClick = { 
                    if (enabled) {
                        onClick?.invoke()
                        expanded = true 
                    }
                }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Gray,
                focusedBorderColor = Color.White,
                unfocusedLabelColor = Color.Gray,
                focusedLabelColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().clickable { 
                if (enabled) {
                    onClick?.invoke()
                    expanded = true 
                }
            },
            enabled = enabled,
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF333333))
                .fillMaxWidth(0.4f), // Adjust width as needed
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No items (loading...)", color = Color.Gray) },
                    onClick = { expanded = false },
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                    )
                }
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Defect Type", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp),
                ) {
                    items(defects) { defect ->
                        Button(
                            onClick = { onDefectSelected(defect) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCF6679),
                                contentColor = Color.White,
                            ),
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50),
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
fun DoneDialog(color: Color, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500.milliseconds)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(240.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = color),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "DONE",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun PreviewMainTabletLayout() {
    // In preview we can't easily create a real repository, so this might fail or need a mock
    // For now I'll just comment out the content or provide a dummy if possible
    /*
    MaterialTheme {
        MainTabletLayout(
            userSession = UserSession(104, "checker"),
            inspectionRepository = ..., 
            onLogout = {}
        )
    }
    */
}
