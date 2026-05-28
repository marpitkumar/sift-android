package com.example.ui

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.widget.DatePicker
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.ExtractedSubscription
import com.example.data.Category
import com.example.data.Subscription
import java.util.*

// --- MAIN ENTRANCE MANAGER ---

@Composable
fun SubscriptionTrackerApp(
    viewModel: SubscriptionViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf("onboarding") }
    var selectedSubscriptionForEdit by remember { mutableStateOf<Subscription?>(null) }
    var initialExtractedDetails by remember { mutableStateOf<ExtractedSubscription?>(null) }

    val subscriptions by viewModel.subscriptions.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val categoriesList = categoriesState.map { it.name }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            "onboarding" -> {
                OnboardingScreen(
                    onComplete = { currentScreen = "dashboard" }
                )
            }
            "dashboard" -> {
                DashboardScreen(
                    subscriptions = subscriptions,
                    onNavigateToExtract = {
                        viewModel.clearExtractionState()
                        currentScreen = "extract"
                    },
                    onNavigateToAdd = {
                        selectedSubscriptionForEdit = null
                        initialExtractedDetails = null
                        currentScreen = "add_edit"
                    },
                    onNavigateToEdit = { sub ->
                        selectedSubscriptionForEdit = sub
                        initialExtractedDetails = null
                        currentScreen = "add_edit"
                    },
                    onNavigateToOptimize = {
                        viewModel.generateBudgetRecommendations()
                        currentScreen = "optimize"
                    },
                    onNavigateToOnboarding = {
                        currentScreen = "onboarding"
                    },
                    onNavigateToCategories = {
                        currentScreen = "categories"
                    },
                    onDeleteSub = { sub -> viewModel.deleteSubscription(sub.id) }
                )
            }
            "extract" -> {
                AIExtractScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = "dashboard" },
                    onProceedToAdd = { extracted ->
                        initialExtractedDetails = extracted
                        selectedSubscriptionForEdit = null
                        currentScreen = "add_edit"
                    }
                )
            }
            "add_edit" -> {
                AddEditScreen(
                    subscriptionToEdit = selectedSubscriptionForEdit,
                    extractedDetails = initialExtractedDetails,
                    dynamicCategories = categoriesList,
                    onAddCategory = { currentScreen = "categories" },
                    onSave = { sub ->
                        viewModel.saveSubscription(sub)
                        currentScreen = "dashboard"
                    },
                    onCancel = { currentScreen = "dashboard" }
                )
            }
            "optimize" -> {
                OptimizeScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        viewModel.clearOptimizationState()
                        currentScreen = "dashboard"
                    }
                )
            }
            "categories" -> {
                CategoriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = "dashboard" }
                )
            }
        }
    }
}

// --- DASHBOARD CONTAINER SCREEN ---

@Composable
fun DashboardScreen(
    subscriptions: List<Subscription>,
    onNavigateToExtract: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Subscription) -> Unit,
    onNavigateToOptimize: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onDeleteSub: (Subscription) -> Unit
) {
    // Math indicators
    val monthlySpend = subscriptions.filter { it.status == "Active" }.sumOf { sub ->
        val amount = sub.billingAmount
        when (sub.billingCycle) {
            "Monthly" -> amount
            "Quarterly" -> amount / 3.0
            "Annual" -> amount / 12.0
            else -> 0.0 // One-time not included in monthly commitments
        }
    }

    val annualEquivalent = monthlySpend * 12.0

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_subscription_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add subscription manually")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Layout
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "My Sift",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Keep digital costs optimized",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Launch Category Manager Icon Button
                        IconButton(
                            onClick = onNavigateToCategories,
                            modifier = Modifier.testTag("btn_manage_categories_top")
                        ) {
                            Icon(Icons.Default.Label, contentDescription = "Manage Categories", colorColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Help/Onboarding re-trigger Icon Button
                        IconButton(
                            onClick = onNavigateToOnboarding,
                            modifier = Modifier.testTag("btn_help_onboarding")
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Show Onboarding Help", colorColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Smart Extract Header Badge
                        Button(
                            onClick = onNavigateToExtract,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_ocr_extract")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "AI Scanner",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Scan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // High-Polish Bento Grid Stats Cards Layout block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bento Box 1: Left larger card with Monthly Burn Stats (weight 1.3)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(175.dp)
                            .testTag("spend_card")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Decorative radial ambient canvas
                            ComposeCanvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(20.dp))
                            ) {
                                val colorBrush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD8E4).copy(alpha = 0.08f), Color.Transparent),
                                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                                    radius = size.minDimension * 0.8f
                                )
                                drawCircle(brush = colorBrush, radius = size.minDimension * 0.8f)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MONTHLY BURN",
                                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )

                                val currencySymbol = if (subscriptions.any { it.currency == "USD" }) "$" else "₹"

                                Text(
                                    text = "$currencySymbol${String.format("%.2f", monthlySpend)}",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )

                                Column {
                                    Text(
                                        text = "Annual Forecast",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "$currencySymbol${String.format("%.1f", annualEquivalent)}/year",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Bento Box 2: Right column with stacked smaller info tiles (weight 0.9)
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .height(175.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stack Tile A: Active Counting
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("ACTIVE COMMITS", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${subscriptions.count { it.status == "Active" }}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Plans", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Stack Tile B: Manage Custom Categories link button
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onNavigateToCategories() }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Label,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Column {
                                    Text("Categories", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("Manage Custom", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }

            // Gemini Budget Optimization Catalyst Banner (Sophisticated Dark Left-Accent style)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToOptimize() }
                        .testTag("recommendation_trigger_banner")
                        .drawBehind {
                            // Left-border accent strip
                            drawRect(
                                color = Color(0xFFD0BCFF),
                                size = size.copy(width = 12f)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Advice icon",
                                colorColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Optimization Opportunity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "AI detects redundancies & unused accounts to save funds immediately.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Navigate to insights",
                            colorColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Subscriptions list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Subscriptions (${subscriptions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (subscriptions.isNotEmpty()) {
                        Text(
                            text = "Swipe/Press item to Edit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Subscriptions Render
            if (subscriptions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Paid,
                            contentDescription = "Empty icon",
                            modifier = Modifier.size(60.dp),
                            colorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Subscriptions Registered",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'AI Scan' or the floating '+' to record Netflix, Spotify, or Gym memberships easily.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(subscriptions) { sub ->
                    SubscriptionListItem(
                        sub = sub,
                        onEdit = { onNavigateToEdit(sub) },
                        onDelete = { onDeleteSub(sub) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// Helper to handle material-icons coloring safely as single custom arg
@Composable
private fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    colorColor: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = colorColor,
        modifier = modifier
    )
}

// --- SUBSCRIPTION ITEM VIEW COMPONENT ---

@Composable
fun SubscriptionListItem(
    sub: Subscription,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("subscription_item_${sub.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatar representing logo
            val categoryColor = when (sub.category) {
                "Entertainment" -> Color(0xFFEF5350)
                "Fitness" -> Color(0xFF66BB6A)
                "Utilities" -> Color(0xFF29B6F6)
                "SaaS" -> Color(0xFFAB47BC)
                else -> Color(0xFFFFA726)
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sub.serviceName.take(1).uppercase(),
                    color = categoryColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Sub details column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sub.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (sub.status == "Paused") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PAUSED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    } else if (sub.usageFrequency == "Unused") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFEAEE), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("UNUSED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = sub.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Next: ${sub.nextRenewalDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val currencySymbol = if (sub.currency == "USD") "$" else "₹"
                Text(
                    text = "$currencySymbol${String.format("%.2f", sub.billingAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = sub.billingCycle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Overflow Options Menu
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit details") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            expandedMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

// --- AI EXTRACTION & OCR SCREEN ---

@Composable
fun AIExtractScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    onProceedToAdd: (ExtractedSubscription) -> Unit
) {
    val extractionState by viewModel.extractionState.collectAsState()
    var pastedText by remember { mutableStateOf("") }
    var selectedPhotoMock by remember { mutableStateOf<Bitmap?>(null) }
    var helperPhotoText by remember { mutableStateOf("") }

    val context = LocalContext.current

    val presetSmsSamples = listOf(
        "Your pack of Rs.299.00 for Netflix Entertainment will renew automatically on 12-Jun-2026. Txn ref: UPI98234.",
        "Spotify premium billing confirmation of $9.99 processed on card Visa 1112. Renewal date: 2026-06-25.",
        "Gold's Gym monthly subscription of INR 1499 debited from HDFC credit card. Net renewing date: 15/06/2026."
    )

    Scaffold(
        topBar = {
            OptSentryTopBar(
                title = "AI Bill & SMS Extract",
                onNavClicked = onNavigateBack,
                widgetTestTag = "back_from_extract"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Save time with Gemini AI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Paste text from a confirmation email, SMS, bank debit notification, or let our AI read detail from a receipt visual directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- OPTION 1: TEXT PASTE ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Option A: Paste Unstructured Text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            label = { Text("Paste SMS, Email alert or receipt texts...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("paste_sms_input"),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preset quick tester logs
                        Text(
                            text = "Tap sample alerts to try instantly:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            presetSmsSamples.forEachIndexed { idx, sample ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            pastedText = sample 
                                            selectedPhotoMock = null
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = sample,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.extractSubscriptionFromText(pastedText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_ocr_text_submit"),
                            enabled = pastedText.isNotBlank() && extractionState !is ExtractionState.Loading
                        ) {
                            Text("Analyze Copied Text")
                        }
                    }
                }
            }

            // --- OPTION 2: RECEIPT OCR VISUALS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Option B: Visual OCR Reader (Gemini Vision)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Allows uploading invoice screenshots. To play instantly on emulator, compile a simulated digital invoice receipt below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Trigger visual mock generation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    selectedPhotoMock = createSimulatedInvoiceBitmap("Netflix Premium", "INR 649.00", "2026-06-12")
                                    helperPhotoText = "Mock visual invoice compiled successfully."
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Text("Load Netflix Card", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    selectedPhotoMock = createSimulatedInvoiceBitmap("Gym Fitness Co", "$45.00", "2026-06-20")
                                    helperPhotoText = "Gym simulated visual invoice compiled successfully."
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Text("Load Gym Digital", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        selectedPhotoMock?.let { bitmap ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SIMULATED RECEIPT CREATED:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                // Render visual preview
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Simulated receipt photo",
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.extractSubscriptionFromImage(bitmap, helperPhotoText)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = extractionState !is ExtractionState.Loading
                                ) {
                                    Text("Analyze Image with Gemini Vision")
                                }
                            }
                        }
                    }
                }
            }

            // STATE DISPLAY
            item {
                AnimatedVisibility(
                    visible = extractionState !is ExtractionState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (extractionState) {
                                is ExtractionState.Success -> Color(0xFFE8F5E9)
                                is ExtractionState.Error -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (val state = extractionState) {
                                is ExtractionState.Loading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Gemini is reading unstructured content...", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                is ExtractionState.Error -> {
                                    Text(
                                        text = "Scan Mismatch",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFC62828)
                                    )
                                }
                                is ExtractionState.Success -> {
                                    val extracted = state.extracted
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Verified details",
                                            tint = Color(0xFF2E7D32)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Extracted Details Successfully!",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("• Service Name: ${extracted.serviceName ?: "Unable to read"}")
                                    Text("• Amount: ${extracted.currency ?: ""} ${extracted.billingAmount ?: "--"}")
                                    Text("• Frequency: ${extracted.billingCycle ?: "Monthly"}")
                                    Text("• Next Renewal: ${extracted.nextRenewalDate ?: "Calculated"}")
                                    Text("• Payment Details: ${extracted.paymentMethod ?: "Unnotified"}")

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { onProceedToAdd(extracted) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("submit_extracted_subscription"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("Proceed & Save Subscription", color = Color.White)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// Programmatic Simulated Digitized Invoice creator for visual OCR
private fun createSimulatedInvoiceBitmap(service: String, amount: String, date: String): Bitmap {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Background card (light green/yellow tint invoice style)
    val bgPaint = Paint().apply {
        color = AndroidColor.parseColor("#FCFCF9")
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    // Invoice Border
    val borderPaint = Paint().apply {
        color = AndroidColor.parseColor("#4A7A56")
        strokeWidth = 10f
        style = Paint.Style.STROKE
    }
    canvas.drawRect(20f, 20f, size.toFloat() - 20f, size.toFloat() - 20f, borderPaint)

    val paint = Paint().apply {
        color = AndroidColor.BLACK
        isAntiAlias = true
    }

    // Title
    paint.textSize = 34f
    paint.isFakeBoldText = true
    canvas.drawText("SUBSCRIPTION INVOICE", 60f, 80f, paint)

    // Divider line
    paint.strokeWidth = 3f
    paint.color = AndroidColor.LTGRAY
    canvas.drawLine(50f, 110f, size.toFloat() - 50f, 110f, paint)

    paint.color = AndroidColor.BLACK
    paint.isFakeBoldText = false
    
    // Content data
    paint.textSize = 24f
    canvas.drawText("TAX INVOICE RECIPIENT", 50f, 160f, paint)
    
    paint.textSize = 28f
    paint.isFakeBoldText = true
    canvas.drawText("Vendor: $service Ltd.", 50f, 220f, paint)
    
    paint.isFakeBoldText = false
    paint.textSize = 24f
    canvas.drawText("Billing Frequency: Monthly", 50f, 270f, paint)
    canvas.drawText("Auto-Renewal Period: 30 days", 50f, 320f, paint)
    canvas.drawText("Valid To Date: $date", 50f, 370f, paint)
    
    // Total cost highlight
    paint.strokeWidth = 2f
    paint.color = AndroidColor.DKGRAY
    canvas.drawLine(50f, 410f, size.toFloat() - 50f, 410f, paint)
    
    paint.color = AndroidColor.parseColor("#1B5E20")
    paint.isFakeBoldText = true
    paint.textSize = 32f
    canvas.drawText("TOTAL CHARGED: $amount", 50f, 460f, paint)

    return bitmap
}


// --- FORM COMPILATION ADD & EDIT SCREEN ---

@Composable
fun AddEditScreen(
    subscriptionToEdit: Subscription?,
    extractedDetails: ExtractedSubscription?,
    dynamicCategories: List<String>,
    onAddCategory: () -> Unit,
    onSave: (Subscription) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Determine default parameters preloading states
    val isEditing = subscriptionToEdit != null
    val defaultName = subscriptionToEdit?.serviceName ?: extractedDetails?.serviceName ?: ""
    val defaultAmount = subscriptionToEdit?.billingAmount?.toString() ?: extractedDetails?.billingAmount?.toString() ?: ""
    val defaultCurrency = subscriptionToEdit?.currency ?: extractedDetails?.currency ?: "INR"
    val defaultCategory = subscriptionToEdit?.category ?: dynamicCategories.firstOrNull() ?: "Entertainment"
    val defaultCycle = subscriptionToEdit?.billingCycle ?: extractedDetails?.billingCycle ?: "Monthly"
    val defaultRenewal = subscriptionToEdit?.nextRenewalDate ?: extractedDetails?.nextRenewalDate ?: "2026-06-01"
    val defaultPayment = subscriptionToEdit?.paymentMethod ?: extractedDetails?.paymentMethod ?: "UPI"
    val defaultFreq = subscriptionToEdit?.usageFrequency ?: "High"
    val defaultAuto = subscriptionToEdit?.isAutoRenew ?: true
    val defaultStatus = subscriptionToEdit?.status ?: "Active"

    var serviceName by remember { mutableStateOf(defaultName) }
    var billingAmount by remember { mutableStateOf(defaultAmount) }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var category by remember { mutableStateOf(defaultCategory) }
    var billingCycle by remember { mutableStateOf(defaultCycle) }
    var nextRenewalDate by remember { mutableStateOf(defaultRenewal) }
    var paymentMethod by remember { mutableStateOf(defaultPayment) }
    var usageFrequency by remember { mutableStateOf(defaultFreq) }
    var isAutoRenew by remember { mutableStateOf(defaultAuto) }
    var status by remember { mutableStateOf(defaultStatus) }

    // Dropdown expansion markers
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showCycleMenu by remember { mutableStateOf(false) }
    var showFreqMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    val categories = dynamicCategories
    val billingCycles = listOf("Monthly", "Quarterly", "Annual", "One-Time")
    val usageFrequencies = listOf("High", "Medium", "Low", "Unused")
    val statuses = listOf("Active", "Paused", "Cancelled")

    // DatePicker trigger dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            nextRenewalDate = formattedDate
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            OptSentryTopBar(
                title = if (isEditing) "Edit Membership" else "Record Subscription",
                onNavClicked = onCancel,
                widgetTestTag = "back_from_record"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Provider Input Field
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service Provider *") },
                    placeholder = { Text("e.g. Netflix, Audible, Google Drive") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_service_name"),
                    singleLine = true
                )
            }

            item {
                // Cost inputs row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Currency Selection
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase() },
                        label = { Text("Currency") },
                        modifier = Modifier
                            .width(90.dp)
                            .testTag("input_currency"),
                        singleLine = true
                    )

                    // Pricing input
                    OutlinedTextField(
                        value = billingAmount,
                        onValueChange = { billingAmount = it },
                        label = { Text("Cost *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_billing_amount"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            item {
                // Category choice select dropdown list in Outlined box
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryMenu = true }
                            .testTag("input_category"),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showCategoryMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("+ Manage Categories") },
                            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showCategoryMenu = false
                                onAddCategory()
                            }
                        )
                    }
                }
            }

            item {
                // Billing cycles Select option
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = billingCycle,
                        onValueChange = {},
                        label = { Text("Billing Cycle") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCycleMenu = true }
                            .testTag("input_billing_cycle"),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showCycleMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showCycleMenu,
                        onDismissRequest = { showCycleMenu = false }
                    ) {
                        billingCycles.forEach { bcy ->
                            DropdownMenuItem(
                                text = { Text(bcy) },
                                onClick = {
                                    billingCycle = bcy
                                    showCycleMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Date picker trigger Outlined box
                OutlinedTextField(
                    value = nextRenewalDate,
                    onValueChange = {},
                    label = { Text("Next Renewal Date *") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                        .testTag("input_renewal_date"),
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick calendar date")
                        }
                    }
                )
            }

            item {
                // Payment Gateway tag line
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("Payment Method (Credit Card, UPI, etc.)") },
                    placeholder = { Text("e.g. Visa 1234, Apple Pay") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_payment_method"),
                    singleLine = true
                )
            }

            item {
                // Usage metric category list dropdown choice
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = usageFrequency,
                        onValueChange = {},
                        label = { Text("My Usage Frequency") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFreqMenu = true }
                            .testTag("input_usage_frequency"),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showFreqMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showFreqMenu,
                        onDismissRequest = { showFreqMenu = false }
                    ) {
                        usageFrequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    usageFrequency = freq
                                    showFreqMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Membership Status selector dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        label = { Text("Subscription Status") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStatusMenu = true }
                            .testTag("input_status"),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showStatusMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        statuses.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    status = st
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Auto renew switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-Renewing Membership",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Alters you prior to transaction date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isAutoRenew,
                        onCheckedChange = { isAutoRenew = it },
                        modifier = Modifier.testTag("input_is_auto_renew")
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Row action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val finalAmount = billingAmount.toDoubleOrNull() ?: 0.0
                            if (serviceName.isNotBlank() && finalAmount >= 0.0) {
                                val savedSub = Subscription(
                                    id = subscriptionToEdit?.id ?: 0,
                                    serviceName = serviceName.trim(),
                                    category = category,
                                    billingAmount = finalAmount,
                                    currency = currency.trim(),
                                    billingCycle = billingCycle,
                                    nextRenewalDate = nextRenewalDate,
                                    paymentMethod = paymentMethod.trim(),
                                    usageFrequency = usageFrequency,
                                    isAutoRenew = isAutoRenew,
                                    status = status
                                )
                                onSave(savedSub)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_subscription_button"),
                        enabled = serviceName.isNotBlank() && billingAmount.toDoubleOrNull() != null
                    ) {
                        Text("Save details")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- GEMINI BUDGET RECOMMENDATIONS SCREEN ---

@Composable
fun OptimizeScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit
) {
    val optimizationState by viewModel.optimizationState.collectAsState()

    Scaffold(
        topBar = {
            OptSentryTopBar(
                title = "AI Financial Sentry",
                onNavClicked = onNavigateBack,
                widgetTestTag = "back_from_optimize"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI icon badge",
                        colorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Sift AI Optimization",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gemini scan insights & redundancy alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = optimizationState) {
                    is OptimizationState.Idle -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No recommendations processed yet.")
                        }
                    }
                    is OptimizationState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Gemini is crunching subscription statistics...", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    is OptimizationState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Recommendation failure: ${state.message}",
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                    is OptimizationState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(
                                            text = "ADVISOR RECOMMENDATIONS:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Beautifully formatted Markdown text
                                        Text(
                                            text = state.recommendations,
                                            fontSize = 14.sp,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (optimizationState is OptimizationState.Success) {
                Button(
                    onClick = { viewModel.generateBudgetRecommendations() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("btn_recalculate_optimization"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recalculate Savings")
                }
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}


// --- GENERAL ACCESSIBILITY SUB-HEADER NAVIGATION BAR ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptSentryTopBar(
    title: String,
    onNavClicked: () -> Unit,
    widgetTestTag: String
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavClicked,
                modifier = Modifier.testTag(widgetTestTag)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}


// --- ONBOARDING COMPOSABLE FLOW ---
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header branding with progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sift",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Text(
                    text = "$step of 4",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            
            // Core content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (step) {
                    1 -> Icons.Outlined.Devices
                    2 -> Icons.Outlined.DocumentScanner
                    3 -> Icons.Outlined.AutoAwesome
                    else -> Icons.Outlined.Label
                }
                
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        colorColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                val title = when (step) {
                    1 -> "Welcome to Sift"
                    2 -> "Instant Smart Scanning"
                    3 -> "AI Budget Optimizer"
                    else -> "Tailored Categories"
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                val desc = when (step) {
                    1 -> "Sift is a modern offline-ready native Android utility. We combine beautiful, sophisticated aesthetics with secure local database storage to keep you in total control."
                    2 -> "No typing required. Simply paste copy-texts from SMS notifications, bills, or bank alerts. Better yet, snap/upload invoice screen-shots, and Gemini AI will perfectly extract the details in a second."
                    3 -> "The high-performance Gemini cognitive optimizer parses your subscription commits list to flag unused accounts, redundancies, or potential family plans and immediately recover your funds."
                    else -> "Create, edit and manage custom categories. Organise your subscriptions under customized tags to suit your exact tracking style and get personalized reporting instantly."
                }
                
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            
            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val isActive = i == step
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isActive) 24.dp else 8.dp)
                            .background(
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Button
                if (step > 1) {
                    TextButton(onClick = { step-- }) {
                        Text("BACK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    TextButton(onClick = onComplete) {
                        Text("SKIP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
                
                // Content trigger next
                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            onComplete()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    val btnText = if (step == 4) "GET STARTED" else "CONTINUE"
                    Text(btnText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }
        }
    }
}


// --- CATEGORIES MANAGEMENT SCREEN ---
@Composable
fun CategoriesScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editName by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            OptSentryTopBar(
                title = "Manage Custom Categories",
                onNavClicked = onNavigateBack,
                widgetTestTag = "back_from_categories"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Section: Add Category Form
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create Custom Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            placeholder = { Text("e.g. Wellness, SaaS") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_new_category"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        
                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    viewModel.saveCategory(Category(name = newCategoryName.trim()))
                                    newCategoryName = ""
                                }
                            },
                            enabled = newCategoryName.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_save_new_category")
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
            
            // Middle section list header
            Text(
                text = "Registered Categories (${categories.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // List of existing categories
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editingCategory?.id == cat.id) {
                                // Editing mode
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (editName.isNotBlank()) {
                                                viewModel.saveCategory(cat.copy(name = editName.trim()))
                                                editingCategory = null
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept edit", tint = Color.Green)
                                    }
                                    IconButton(onClick = { editingCategory = null }) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Cancel edit", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            } else {
                                // Normal display mode
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            editName = cat.name
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Category",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteCategory(cat.id) },
                                        enabled = categories.size > 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Category",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
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
