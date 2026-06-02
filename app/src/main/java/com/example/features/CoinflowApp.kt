package com.example.features

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.db.*
import com.example.core.model.Money
import com.example.core.security.BiometricHelper
import com.example.ui.theme.MintAccent
import com.example.ui.theme.WarningRed
import java.text.SimpleDateFormat
import java.util.*

// Custom scale press micro-interaction
fun Modifier.pressScaleEffect() = this

// iOS Native Design Helper Component: Grouped Section (Grouped UITableView Style)
@Composable
fun iOSGroupedColumn(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

// iOS Native Design Helper Component: List Row Item (Grouped Row Style)
@Composable
fun iOSRowItem(
    icon: String? = null,
    iconBg: Color = Color.Transparent,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.outline,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
    isLast: Boolean = false
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (!value.isNullOrEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = valueColor,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Navigate Screen Icon",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    if (!isLast) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
            thickness = 0.5.dp
        )
    }
}

// iOS Native Design Helper Component: Segmented Slider
@Composable
fun iOSSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val bgAnim by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(180)
            )
            val textColAnim by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                animationSpec = tween(180)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgAnim)
                    .clickable { onSelectedIndexChanged(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColAnim
                )
            }
        }
    }
}

// iOS Native Design Helper Component: UIAlertController Style Dialog
@Composable
fun iOSAlertDialog(
    title: String,
    message: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .width(280.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (message != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isDestructive) Color(0xFFFF453A) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// iOS Native Design Helper Component: Cupertino-Like Input Text Field
@Composable
fun iOSFormInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                unfocusedContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

// iOS Native Design Helper Component: UISwitch Composable
@Composable
fun iOSToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "SwitchThumb"
    )
    val bgCol by animateColorAsState(
        targetValue = if (checked) Color(0xFF34C759) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "SwitchBg"
    )
    Box(
        modifier = Modifier
            .size(50.dp, 30.dp)
            .clip(CircleShape)
            .background(bgCol)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinflowApp(
    viewModel: CoinflowViewModel,
    onTriggerBiometrics: (onSuccess: () -> Unit) -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isBiometricsSettingEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Trigger biometric lock overlay on resume
    LaunchedEffect(isAppLocked) {
        if (isAppLocked && isBiometricsSettingEnabled) {
            onTriggerBiometrics {
                viewModel.unlockApp()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            if (!isAppLocked) {
                CoinflowBottomBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { viewModel.currentScreen.value = it },
                    onAddClicked = { } // Center FAB is a direct trigger, we handle in sheet overlay state
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isAppLocked && isBiometricsSettingEnabled) {
                // Security overlay screen
                LockOverlayScreen(
                    onAuthClick = {
                        onTriggerBiometrics {
                            viewModel.unlockApp()
                        }
                    }
                )
            } else {
                // App screens routing
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        val slideDirection = if (targetState == "Home" || (targetState == "Stats" && initialState != "Home")) -1 else 1
                        fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                            initialOffsetX = { it * slideDirection }
                        ) togetherWith fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 305f),
                            targetOffsetX = { -it * slideDirection }
                        )
                    },
                    label = "ScreenSwitch"
                ) { screen ->
                    when (screen) {
                        "Home" -> DashboardTab(viewModel)
                        "Stats" -> StatsTab(viewModel)
                        "Transactions" -> TransactionsTab(viewModel)
                        "Settings" -> SettingsTab(viewModel)
                        else -> DashboardTab(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LockOverlayScreen(onAuthClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.bindDp()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked Icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Coinflow is Locked",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Biometric lock is active. Please authenticate to access your transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = onAuthClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressScaleEffect()
                    .testTag("unlock_button")
            ) {
                Text("Authenticate", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

private fun Int.bindDp(): Dp = this.dp

@Composable
fun CoinflowBottomBar(
    currentScreen: String,
    onScreenSelected: (String) -> Unit,
    onAddClicked: () -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Elegant top line divider identical to iOS TabBar
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab Item: Home
                val isHome = currentScreen == "Home"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { onScreenSelected("Home") },
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        )
                        .pressScaleEffect()
                        .testTag("bottom_nav_home"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isHome) Icons.Default.Home else Icons.Default.Home, // Outlined usually, but Default matches iOS well
                        contentDescription = "Home Icon",
                        tint = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Home",
                        fontSize = 11.sp,
                        fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium,
                        color = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                }

                // Tab Item: Stats
                val isStats = currentScreen == "Stats"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { onScreenSelected("Stats") },
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        )
                        .pressScaleEffect()
                        .testTag("bottom_nav_stats"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Stats Icon",
                        tint = if (isStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Stats",
                        fontSize = 11.sp,
                        fontWeight = if (isStats) FontWeight.Bold else FontWeight.Medium,
                        color = if (isStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                }

                // Tab Item: Custom Center Add (iOS Circular Plus Style)
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable(
                            onClick = { showAddSheet = true },
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        )
                        .pressScaleEffect(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Expense Icon",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Tab Item: Transactions
                val isTransactions = currentScreen == "Transactions"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { onScreenSelected("Transactions") },
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        )
                        .pressScaleEffect()
                        .testTag("bottom_nav_transactions"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Transactions Icon",
                        tint = if (isTransactions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Transactions",
                        fontSize = 11.sp,
                        fontWeight = if (isTransactions) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTransactions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                }

                // Tab Item: Settings
                val isSettings = currentScreen == "Settings"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { onScreenSelected("Settings") },
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        )
                        .pressScaleEffect()
                        .testTag("bottom_nav_settings"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        tint = if (isSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Settings",
                        fontSize = 11.sp,
                        fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddExpenseSheet(
            onDismissRequest = { showAddSheet = false }
        )
    }
}

// ---------------- DASHBOARD TAB ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(viewModel: CoinflowViewModel) {
    val totalBalance by viewModel.repository.globalTotalBalanceMinor.collectAsStateWithLifecycle(0)
    val monthlyTotals by viewModel.repository.monthlyTotals.collectAsStateWithLifecycle(MonthlyTotals(0, 0))
    val budgetProgress by viewModel.repository.budgetProgressList.collectAsStateWithLifecycle(emptyList())
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle(emptyList())
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()

    var showBudgetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Coinflow",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Seamless offline tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Avatar shape
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CF",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Balance Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val formattedBalance = remember(totalBalance, baseCurrency) {
                        Money(totalBalance, baseCurrency).format()
                    }

                    // Count up transition representation
                    Text(
                        text = formattedBalance,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("balance_display")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Monthly Stats division
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Month Income
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Income Arrow",
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "INCOME (MONTH)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Money(monthlyTotals.incomeMinor, baseCurrency).format(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF34C759),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Month Expense
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Expense Arrow",
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SPENT (MONTH)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Money(monthlyTotals.spentMinor, baseCurrency).format(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Budgets progress section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { showBudgetDialog = true },
                    modifier = Modifier.testTag("manage_budgets_button")
                ) {
                    Text("Manage", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (budgetProgress.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No budgets configured yet.\nConfigure category limits by clicking Manage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    budgetProgress.forEach { item ->
                        val ratioAnim by animateFloatAsState(
                            targetValue = item.ratio.toFloat().coerceAtMost(1f),
                            animationSpec = spring(stiffness = 200f),
                            label = "BudgetProgress"
                        )

                        val budgetTypeColor = if (item.ratio >= 0.85) WarningRed else MaterialTheme.colorScheme.primary

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.category?.icon ?: "🏷️",
                                            fontSize = 22.sp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = item.category?.name ?: "Category",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "${item.budget.period.name.lowercase().replaceFirstChar { it.uppercase() }} budget",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${Money(item.spentMinor, baseCurrency).format()} of ${Money(item.limitMinor, baseCurrency).format()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${(item.ratio * 100).toInt()}% spent",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = budgetTypeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Progress Indicator Layout
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(ratioAnim)
                                            .clip(CircleShape)
                                            .background(budgetTypeColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent transactions section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { viewModel.currentScreen.value = "Transactions" }) {
                    Text("View All", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No logs logged yet.\nTap the + button below to log your first transaction!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Limit display to top 5
                    transactions.take(5).forEach { item ->
                        TransactionRowItem(item, baseCurrency) {
                            // Quick deletion action
                            viewModel.deleteTransaction(item.transaction)
                        }
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        ManageBudgetsDialog(
            viewModel = viewModel,
            onDismiss = { showBudgetDialog = false }
        )
    }
}

@Composable
fun TransactionRowItem(
    item: TransactionWithDetails,
    baseCurrency: String,
    onDelete: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleEffect()
            .clickable { showOptions = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category icon bubble
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(item.category?.colorHex ?: "#1E2A38"))
                                    .copy(alpha = 0.12f)
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.category?.icon ?: "🏷️",
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.category?.name ?: "Expense Log",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = item.transaction.note ?: "No note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isIncome = item.transaction.amountMinor > 0
                val amountColor = if (isIncome) Color(0xFF34C759) else Color(0xFFFF3B30)
                val currencyUsed = item.wallet?.currencyCode ?: baseCurrency

                Text(
                    text = Money(item.transaction.amountMinor, currencyUsed).format(showSign = true),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                // Date representation
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(item.transaction.occurredAt))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showOptions) {
        iOSAlertDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this log of \"${item.category?.name ?: ""}\"? This operation cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDelete()
                showOptions = false
            },
            onDismiss = {
                showOptions = false
            }
        )
    }
}

// ---------------- STATS TAB ----------------
@Composable
fun StatsTab(viewModel: CoinflowViewModel) {
    val allTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle(emptyList())
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val categories by viewModel.repository.allCategories.collectAsStateWithLifecycle(emptyList())

    var searchInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }

    // Statistics Calculation
    val groupedSums = remember(allTransactions) {
        val expenseTransactions = allTransactions.filter { it.transaction.amountMinor < 0 }
        val sumMap = mutableMapOf<Long, Int>()
        for (item in expenseTransactions) {
            val catId = item.transaction.categoryId
            val amt = Math.abs(item.transaction.amountMinor)
            sumMap[catId] = (sumMap[catId] ?: 0) + amt
        }
        sumMap
    }

    val totalExpensesSum = remember(groupedSums) {
        groupedSums.values.sum()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Breakdown of monthly spend",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Custom Donut Canvas Graph representing distribution
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SPENDING STRUCTURE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (totalExpensesSum == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expense data for layout structure.\nTry logging expenses to view charts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Donuts Canvas Draw element
                        Box(
                            modifier = Modifier.size(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var currentAngle = -90f
                                for ((catId, amt) in groupedSums) {
                                    val matchCat = categories.find { it.id == catId }
                                    val hex = matchCat?.colorHex ?: "#00F5D4"
                                    val strokePart = (amt.toFloat() / totalExpensesSum.toFloat()) * 360f

                                    drawArc(
                                        color = try {
                                            Color(android.graphics.Color.parseColor(hex))
                                        } catch (e: Exception) {
                                            Color.Cyan
                                        },
                                        startAngle = currentAngle,
                                        sweepAngle = strokePart,
                                        useCenter = false,
                                        style = Stroke(width = 24f, cap = StrokeCap.Round)
                                    )
                                    currentAngle += strokePart
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = Money(-totalExpensesSum, baseCurrency).format(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFFFF3B30)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category Distribution bars list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            groupedSums.forEach { (catId, amt) ->
                                val matchCat = categories.find { it.id == catId }
                                val color = try {
                                    Color(android.graphics.Color.parseColor(matchCat?.colorHex ?: "#00F5D4"))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                val pct = (amt.toDouble() / totalExpensesSum.toDouble() * 100).toInt()

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(matchCat?.icon ?: "🏷️", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = matchCat?.name ?: "Unknown",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "${Money(-amt, baseCurrency).format()} ($pct%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Simple Horizontal Progress representation
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(amt.toFloat() / totalExpensesSum.toFloat())
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

// ---------------- TRANSACTIONS TAB ----------------
@Composable
fun TransactionsTab(viewModel: CoinflowViewModel) {
    val allTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle(emptyList())
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val categories by viewModel.repository.allCategories.collectAsStateWithLifecycle(emptyList())

    var searchInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Search and track income & expense histories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Custom Search and Filtration Layout
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Text search input styled exactly like iOS Search Controller
                TextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.searchQuery.value = it
                    },
                    placeholder = { Text("Search description...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("search_input"),
                    leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                // Filter Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = {
                                selectedCategoryFilter = null
                                viewModel.filterCategory.value = null
                            },
                            label = { Text("All") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter?.id == cat.id,
                            onClick = {
                                selectedCategoryFilter = cat
                                viewModel.filterCategory.value = cat
                            },
                            label = { Text("${cat.icon} ${cat.name}") }
                        )
                    }
                }
            }
        }

        // Historic logs matching filter list
        if (allTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No financial history meets the selected criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(allTransactions) { item ->
                TransactionRowItem(item, baseCurrency) {
                    viewModel.deleteTransaction(item.transaction)
                }
            }
        }
    }
}

// ---------------- SETTINGS TAB ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(viewModel: CoinflowViewModel) {
    val context = LocalContext.current
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val categories by viewModel.repository.allCategories.collectAsStateWithLifecycle(emptyList())

    var showAddCategoryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configure your security and exports",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // iOS Grouped Card Style: App settings & Security
        item {
            iOSGroupedColumn(title = "App Settings & Security") {
                // Row 1: Dark Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = "Dark Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dark mode theme",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Vibrant elements on high contrast background",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    iOSToggle(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )

                // Row 2: Biometrics Lock Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF34C759).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometrics Icon",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Biometric lock protection",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Require authentication on open",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    iOSToggle(
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricLockEnabled(it) }
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )

                // Row 3: Base Display Currency Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF9500).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = "Currency Icon",
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Default display currency",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Set common exchange code",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { showMenu = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = baseCurrency,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            val codes = listOf("INR", "USD", "EUR", "GBP", "JPY", "CAD")
                            codes.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code) },
                                    onClick = {
                                        viewModel.setBaseCurrency(code)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // iOS Grouped Card Style: Data management
        item {
            iOSGroupedColumn(title = "Data Management") {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Export your financial records locally. It generates a standard compliance-compatible spreadsheet which you can instantly share.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.exportCSVAndShare(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .pressScaleEffect()
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Share, "Export Icon", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export & Share CSV File",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Section custom categories management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Manage Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { showAddCategoryDialog = true }) {
                    Text("+ Add Custom", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            iOSGroupedColumn {
                categories.forEachIndexed { index, cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = cat.type.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Do not allow deleting seeded elements (simply safety for UI logic)
                        if (cat.id > 6) {
                            IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                Icon(Icons.Default.Delete, "Delete Icon", tint = Color(0xFFFF3B30))
                            }
                        }
                    }

                    if (index < categories.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            viewModel = viewModel,
            onDismiss = { showAddCategoryDialog = false }
        )
    }
}

// ---------------- DIALOGS AND MODAL BOTTOM SHEETS ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    onDismissRequest: () -> Unit
) {
    val viewModel: CoinflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val categories by viewModel.repository.allCategories.collectAsStateWithLifecycle(emptyList())
    val wallets by viewModel.repository.allWallets.collectAsStateWithLifecycle(emptyList())

    val context = LocalContext.current

    // Modal data state
    var enteringAmountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var optionalNote by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Init defaults when loaded
    LaunchedEffect(categories, wallets) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.firstOrNull { it.type == TransactionType.EXPENSE } ?: categories.firstOrNull()
        }
        if (selectedWallet == null && wallets.isNotEmpty()) {
            selectedWallet = wallets.firstOrNull()
        }
    }

    // Modal Bottom Sheet container
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "LOG FINANCIAL ACTIVITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Centered editable Amount Input Box with proper placeholder "0.00"
            OutlinedTextField(
                value = enteringAmountStr,
                onValueChange = { newValue ->
                    // Correctly allow decimals and digits up to 2 decimal places
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        enteringAmountStr = newValue
                    }
                },
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0.00",
                            style = MaterialTheme.typography.displayMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedCategory?.type == TransactionType.INCOME) Color(0xFF4CAF50) else WarningRed,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input")
            )

            // Category Horizontal Scroll List
            Column {
                Text("Select structural category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory?.id == cat.id,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.icon} ${cat.name}") }
                        )
                    }
                }
            }

            // Note Text Fields & Date Trigger Box Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Note fields
                OutlinedTextField(
                    value = optionalNote,
                    onValueChange = { optionalNote = it },
                    placeholder = { Text("Enter optional memo...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("memo_input")
                )

                // Date triggers chip
                val simpleDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    selectedDateMillis = newCal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        simpleDateStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Submit Primary buttons
            Button(
                onClick = {
                    if (enteringAmountStr.isEmpty() || enteringAmountStr.toDoubleOrNull() == null) {
                        Toast.makeText(context, "Please enter a valid numeric value", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val baseVal = enteringAmountStr.toDouble()
                    val exponent = Money.getExponent(selectedWallet?.currencyCode ?: "INR")
                    val decimalPower = Math.pow(10.0, exponent.toDouble())
                    val finalMinorVal = (baseVal * decimalPower).toInt()

                    viewModel.addTransaction(
                        walletId = selectedWallet?.id ?: 1,
                        categoryId = selectedCategory?.id ?: 1,
                        amountMinor = finalMinorVal,
                        type = selectedCategory?.type ?: TransactionType.EXPENSE,
                        note = optionalNote.ifEmpty { "Cash log" },
                        occurredAt = selectedDateMillis
                    )

                    // Show elastic spring toast feedback
                    Toast.makeText(context, "Log added successfully!", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pressScaleEffect()
                    .testTag("save_expense_button")
            ) {
                Text(
                    text = "Commit Log Immediately",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ManageBudgetsDialog(
    viewModel: CoinflowViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.repository.allCategories.collectAsStateWithLifecycle(emptyList())
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var limitInput by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }

    LaunchedEffect(categories) {
        if (selectedCat == null && categories.isNotEmpty()) {
            selectedCat = categories.firstOrNull()
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure Budgets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Selector label
                Text(
                    text = "SELECT CATEGORY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCat?.id == cat.id,
                            onClick = { selectedCat = cat },
                            label = { Text("${cat.icon} ${cat.name}") }
                        )
                    }
                }

                // Input
                iOSFormInputField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = "Limit major amount (e.g. 150)",
                    label = "BUDGET CAP LIMIT"
                )

                // Period with Segmented control
                Text(
                    text = "PERIOD CYCLE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                val periods = listOf("Monthly", "Weekly")
                val activeIndex = if (selectedPeriod == BudgetPeriod.MONTHLY) 0 else 1
                iOSSegmentedControl(
                    options = periods,
                    selectedIndex = activeIndex,
                    onSelectedIndexChanged = { idx ->
                        selectedPeriod = if (idx == 0) BudgetPeriod.MONTHLY else BudgetPeriod.WEEKLY
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom iOS separator & Buttons
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    )

                    TextButton(
                        onClick = {
                            val cat = selectedCat ?: return@TextButton
                            val parsedVal = limitInput.toDoubleOrNull() ?: return@TextButton
                            val minorVal = (parsedVal * 100).toInt()

                            viewModel.addOrUpdateBudget(
                                categoryId = cat.id,
                                limitMinor = minorVal,
                                period = selectedPeriod
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_budget_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun AddWalletDialog(
    viewModel: CoinflowViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("💵") }
    var colorHex by remember { mutableStateOf("#4CAF50") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Multi-Currency Wallet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                iOSFormInputField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Personal Savings",
                    label = "WALLET NAME"
                )

                iOSFormInputField(
                    value = balance,
                    onValueChange = { balance = it },
                    placeholder = "0.00",
                    label = "OPENING BALANCE AMOUNT",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Simple icon picker row
                Text(
                    text = "CUSTOMIZE ICON",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                val icons = listOf("💵", "💳", "🏦", "💰", "🐖")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    icons.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (icon == i) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { icon = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(i, fontSize = 20.sp)
                        }
                    }
                }

                // Simple color picker row
                Text(
                    text = "COLOR IDENTITY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (colorHex == hex) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    )

                    TextButton(
                        onClick = {
                            if (name.isEmpty() || balance.toDoubleOrNull() == null) return@TextButton
                            val baseAmt = balance.toDouble()
                            val minorVal = (baseAmt * 100).toInt()

                            viewModel.addWallet(
                                name = name,
                                currency = "USD",
                                openingBalanceMinor = minorVal,
                                icon = icon,
                                colorHex = colorHex
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_add_wallet_button")
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    viewModel: CoinflowViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("📦") }
    var colorHex by remember { mutableStateOf("#92A3B5") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Custom Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                iOSFormInputField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Stream Subscriptions",
                    label = "CATEGORY NAME"
                )

                // Type selector with iOS Segmented control
                Text(
                    text = "TRANSACTION FLOW TYPE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                val types = listOf("Expense", "Income")
                val typeIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1
                iOSSegmentedControl(
                    options = types,
                    selectedIndex = typeIndex,
                    onSelectedIndexChanged = { idx ->
                        selectedType = if (idx == 0) TransactionType.EXPENSE else TransactionType.INCOME
                    }
                )

                // Simple icon picker row
                Text(
                    text = "SYMBOL ICON",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                val icons = listOf("📦", "👕", "🧹", "💊", "⛽", "🎓")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    icons.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (icon == i) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { icon = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(i, fontSize = 20.sp)
                        }
                    }
                }

                // Simple color picker row
                Text(
                    text = "AESTHETIC COLOR",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                val colors = listOf("#009688", "#E91E63", "#673AB7", "#FF5722", "#3F51B5")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (colorHex == hex) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    )

                    TextButton(
                        onClick = {
                            if (name.isEmpty()) return@TextButton
                            viewModel.addCategory(name, icon, colorHex, selectedType)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
