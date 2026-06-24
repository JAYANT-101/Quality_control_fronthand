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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
    windowWidthSizeClass: WindowWidthSizeClass,
    onLogout: () -> Unit,
) {
    var selectedLine by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    val isTablet = windowWidthSizeClass != WindowWidthSizeClass.Compact

    fun onSaveInspection(result: String, defectType: String, po: String, poId: Int, onSuccess: (PoProgress) -> Unit) {
        if (isSubmitting) return
        isSubmitting = true
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        scope.launch {
            val request = CheckerOutputRequest(
                userId = userSession.userId,
                line = selectedLine,
                poId = poId,
                fieldName = result.lowercase(),
                defectName = defectType,
                actualEventTime = timestamp,
            )
            
            checkerOutputRepository.submitCheckerOutput(request)
                .onSuccess { response ->
                    inspectionRepository.saveInspection(
                        InspectionEntity(
                            taskId = po,
                            lineNo = selectedLine,
                            result = result,
                            defectType = defectType.ifEmpty { null },
                            checkerId = userSession.userId,
                        ),
                    )
                    onSuccess(response.po)
                    isSubmitting = false
                }
                .onFailure { error ->
                    println("InspectionScreen: Failed to submit checker output: ${error.message}")
                    isSubmitting = false
                }
        }
    }

    if (isTablet) {
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
                    isTablet = true,
                ) {
                    scope.launch {
                        inspectionRepository.resetAllCountsAndTasks()
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                Surface(
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Line $selectedLine",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF1A1A1A),
                    modifier = Modifier.height(80.dp)
                ) {
                    (1..6).forEach { line ->
                        NavigationBarItem(
                            selected = selectedLine == line,
                            onClick = { selectedLine = line },
                            icon = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedLine == line) Color(0xFF6750A4) else Color(0xFF333333),
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("L$line", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            label = { Text("Line $line", color = if (selectedLine == line) Color.White else Color.Gray, fontSize = 9.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                    }
                }
            },
            containerColor = Color.Black
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                WorkArea(
                    selectedLine = selectedLine,
                    inspectionRepository = inspectionRepository,
                    poRepository = poRepository,
                    onSaveInspection = ::onSaveInspection,
                    isSubmitting = isSubmitting,
                    isTablet = false,
                ) {
                    scope.launch {
                        inspectionRepository.resetAllCountsAndTasks()
                    }
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
    isTablet: Boolean,
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTablet) 32.dp else 16.dp, vertical = if (isTablet) 24.dp else 16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    if (isTablet) "Line $selectedLine - Inspection" else "Inspection",
                    style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
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
            Row(horizontalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 8.dp)) {
                CounterHeader("PASS", passCount, Color(0xFF4CAF50), isTablet)
                CounterHeader("ALTER", alterCount, Color(0xFFFFB300), isTablet)
                CounterHeader("REJECT", rejectCount, Color(0xFFF44336), isTablet)
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
        
        Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

        // Filters
        if (isTablet) {
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
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownFilter(
                    label = "Product Type",
                    options = productTypes,
                    selectedOption = selectedProductType,
                    onOptionSelected = { selectedProductType = it },
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (selectedProductType != null) && (!isLoadingPoNumbers),
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp)) {
                InfoRow("Remaining Target:", (selectedPoTarget ?: 0).toString(), Color.Gray, Color(0xFF00BFA5), isTablet)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Selected PO:", selectedPoNumber ?: "Not Selected", Color.Gray, Color(0xFF00BFA5), isTablet)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Big Buttons
        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
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
                    isTablet = true
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
                    isTablet = true
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
                    isTablet = true
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = buttonsEnabled,
                    isTablet = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        isTablet = false
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
                        isTablet = false
                    )
                }
            }
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
fun CounterHeader(label: String, count: Int, color: Color, isTablet: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = if (isTablet) 14.sp else 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(count.toString(), fontSize = if (isTablet) 24.sp else 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun InfoRow(label: String, value: String, labelColor: Color, valueColor: Color, isTablet: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = labelColor, fontSize = if (isTablet) 20.sp else 16.sp, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontSize = if (isTablet) 22.sp else 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, isTablet: Boolean) {
    Button(
        onClick = onClick,
        modifier = modifier.height(if (isTablet) 110.dp else 80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.5f),
        ),
        shape = MaterialTheme.shapes.large,
        enabled = enabled,
    ) {
        Text(text, fontSize = if (isTablet) 36.sp else 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
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
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Defect Type",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 450.dp),
                ) {
                    items(defects) { defect ->
                        Button(
                            onClick = { onDefectSelected(defect) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCF6679),
                                contentColor = Color.White,
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                defect,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CANCEL", color = Color.Gray)
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
