package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.navigation.navArgument
import com.example.data.model.Friend
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.data.model.DailyGoal
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.StreamComment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// High-Contrast Theme Color Tokens
val DarkBackground = Color(0xFF1A1C1E)
val SurfaceCard = Color(0xFF24292E)
val SurfaceAccent = Color(0xFF44474E)
val BrandCard = Color(0xFF3D4758)
val NeonPrimary = Color(0xFFD1E1FF)   // Steel Accent Blue (Alex Rivera Theme)
val NeonSecondary = Color(0xFFBAC7E3) // Cool Muted Slate Blue
val NeonAccent = Color(0xFFFFB4AB)    // Delicate Coral Accent and Star Points
val LiveRed = Color(0xFFBA1A1A)       // Elegant Crimson Red

// Simple Data class for Floating Points
data class FloatingPointItem(
    val id: Long,
    val xOffset: Float,
    val text: String = "+1 Point! 🌟"
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ChatViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isLoggedIn = userProfile?.isLoggedIn == true
    
    if (!isLoggedIn) {
        LoginRegisterScreen(viewModel)
    } else {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        
        // Track floating point triggers
        var floatingPoints by remember { mutableStateOf<List<FloatingPointItem>>(emptyList()) }
        var pointCounter by remember { mutableLongStateOf(0L) }
        val context = LocalContext.current

        LaunchedEffect(viewModel) {
            viewModel.showPointAnimation.collectLatest {
                val randomOffset = Random.nextFloat() * 160f - 80f // horizontal spreading
                val newItem = FloatingPointItem(
                    id = pointCounter++,
                    xOffset = randomOffset
                )
                floatingPoints = floatingPoints + newItem
            }
        }

        LaunchedEffect(viewModel) {
            viewModel.hapticEvents.collectLatest { event ->
                when(event) {
                    "message" -> com.example.util.HapticUtil.triggerVibration(context, 100)
                    "success" -> com.example.util.HapticUtil.triggerVibration(context, 200)
                }
            }
        }

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "chats"
                
                // Hide bottom bar on chat detail or stream screens
                val showBottomBar = currentRoute in listOf("chats", "updates", "communities", "profile")
                
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = DarkBackground,
                        tonalElevation = 8.dp,
                        modifier = Modifier.border(0.5.dp, SurfaceAccent, RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "chats",
                            onClick = { navController.navigate("chats") { popUpTo("chats"); launchSingleTop = true } },
                            icon = { Icon(if (currentRoute == "chats") Icons.Filled.Forum else Icons.Outlined.Forum, contentDescription = "Chats") },
                            label = { Text("Chats") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonPrimary,
                                indicatorColor = SurfaceAccent,
                                unselectedIconColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "updates",
                            onClick = { navController.navigate("updates") { popUpTo("chats"); launchSingleTop = true } },
                            icon = { Icon(if (currentRoute == "updates") Icons.Filled.Update else Icons.Outlined.Update, contentDescription = "Updates") },
                            label = { Text("Updates") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonPrimary,
                                indicatorColor = SurfaceAccent,
                                unselectedIconColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "communities",
                            onClick = { navController.navigate("communities") { popUpTo("chats"); launchSingleTop = true } },
                            icon = { Icon(if (currentRoute == "communities") Icons.Filled.Groups else Icons.Outlined.Groups, contentDescription = "Communities") },
                            label = { Text("Communities") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonPrimary,
                                indicatorColor = SurfaceAccent,
                                unselectedIconColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "profile",
                            onClick = { navController.navigate("profile") { popUpTo("chats"); launchSingleTop = true } },
                            icon = { Icon(if (currentRoute == "profile") Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                            label = { Text("Profile") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonPrimary,
                                indicatorColor = SurfaceAccent,
                                unselectedIconColor = Color.Gray
                            )
                        )
                    }
                }
            },
            containerColor = DarkBackground
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                NavHost(navController = navController, startDestination = "chats") {
                    composable("chats") {
                        ChatsTab(viewModel, navController)
                    }
                    composable("find_friends") {
                        FindFriendsScreen(viewModel, navController)
                    }
                    composable(
                        route = "chat_detail/{friendId}",
                        arguments = listOf(navArgument("friendId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val friendId = backStackEntry.arguments?.getInt("friendId") ?: 0
                        ChatDetailScreen(viewModel, friendId, navController)
                    }
                    composable("updates") {
                        VideosTab(viewModel, navController)
                    }
                    composable("live_preview") {
                        LivePreviewScreen(navController)
                    }
                    composable(
                        route = "stream_watch/{friendId}",
                        arguments = listOf(navArgument("friendId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val friendId = backStackEntry.arguments?.getInt("friendId") ?: 0
                        StreamWatchScreen(viewModel, friendId, navController)
                    }
                    composable("stream_host") {
                        StreamHostScreen(viewModel, navController)
                    }
                    composable("communities") {
                        // Placeholder
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Communities", color = Color.White)
                        }
                    }
                    composable("profile") {
                        ProfileTab(viewModel, navController)
                    }
                    composable("profile_edit") {
                        ProfileScreen(viewModel)
                    }
                    composable("leaderboard") {
                        LeaderboardTab(viewModel = viewModel, onBackClick = { navController.popBackStack() })
                    }
                }

                // Floating "+1 Point!" animations on sending messages
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    floatingPoints.forEach { point ->
                        key(point.id) {
                            FloatingPointChip(
                                point = point,
                                onAnimationFinished = {
                                    floatingPoints = floatingPoints.filterNot { it.id == point.id }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingPointChip(
    point: FloatingPointItem,
    onAnimationFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val yOffset = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            yOffset.animateTo(
                targetValue = -350f,
                animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            delay(800)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700)
            )
            isVisible = false
            onAnimationFinished()
        }
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .offset { IntOffset(point.xOffset.toInt(), yOffset.value.toInt() - 100) }
                .graphicsLayer(alpha = alpha.value)
                .background(
                    Brush.horizontalGradient(listOf(NeonPrimary, NeonAccent)),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("floating_point_toast")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🌟",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = point.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ==========================================
// CHATS TAB
// ==========================================
@Composable
fun ChatsTab(viewModel: ChatViewModel, navController: NavController) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // Simplified handling: Just log for now. 
            // "Send anywhere" would require UI to select chat.
            android.util.Log.d("ChatsTab", "Photo captured: ${bitmap.width}x${bitmap.height}")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            // Show toast or snackbar
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // WhatsApp-like Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PineChat",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = { navController.navigate("leaderboard") }) {
                    Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard", tint = Color.White)
                }
                IconButton(onClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                }
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
        }
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E2428),
                unfocusedContainerColor = Color(0xFF1E2428),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(friends.filter { it.name.contains(searchQuery, ignoreCase = true) }) { friend ->
                    FriendChatRow(friend) {
                        viewModel.selectFriend(friend.id)
                        navController.navigate("chat_detail/${friend.id}")
                    }
                }
            }
            
            // FAB
            FloatingActionButton(
                onClick = { navController.navigate("find_friends") },
                containerColor = NeonPrimary,
                contentColor = DarkBackground,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Friend")
            }
        }
    }
}

@Composable
fun StreakTracker(streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24292E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFFF6600))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Participation Streak: $streak days", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DailyGoalsSection(viewModel: ChatViewModel) {
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Goals", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            goals.forEach { goal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (goal.isAiGenerated) {
                        Icon(Icons.Default.Star, contentDescription = "AI Quest", tint = NeonPrimary, modifier = Modifier.size(24.dp).padding(end = 8.dp))
                    } else {
                        Checkbox(checked = goal.isCompleted, onCheckedChange = { viewModel.toggleGoal(goal) }, colors = CheckboxDefaults.colors(checkedColor = NeonPrimary))
                    }
                    Text(goal.task, color = if (goal.isCompleted) Color.Gray else Color.White, modifier = Modifier.padding(start = if (goal.isAiGenerated) 0.dp else 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun UserProfileHeader(user: UserProfile?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant user avatar with border from design
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandCard)
                    .border(1.dp, SurfaceAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = NeonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back,",
                    color = Color(0xFFC4C6CF),
                    fontSize = 12.sp
                )
                Text(
                    text = user?.name ?: "Alex Rivera",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Highlighting point count using payments/scores styling
            Box(
                modifier = Modifier
                    .background(BrandCard, shape = CircleShape)
                    .border(0.75.dp, SurfaceAccent, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = "Points",
                        tint = NeonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${user?.points ?: 0}",
                        color = NeonPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FriendChatRow(friend: Friend, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("friend_chat_row_${friend.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored avatar view
            AvatarView(name = friend.name, avatarId = friend.avatar, isOnline = friend.isOnline, size = 48.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friend.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Streaming indicator
                    if (friend.isStreaming) {
                        Box(
                            modifier = Modifier
                                .background(LiveRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, LiveRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LivePulsatingCircle(size = 6.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    color = LiveRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (friend.isStreaming) "Started stream: ${friend.streamTitle}" else friend.bio,
                    color = if (friend.isStreaming) NeonSecondary else Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Score tag
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🏆",
                    fontSize = 12.sp
                )
                Text(
                    text = "${friend.points} pts",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AvatarView(name: String, avatarId: String, isOnline: Boolean, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val char = name.firstOrNull()?.uppercase() ?: "?"
    val isCustomImage = avatarId.startsWith("http") || avatarId.startsWith("content")
    
    // Choose beautiful "Elegant Dark" muted pastel gradient patterns depending on avatarId
    val gradientColors = when (avatarId) {
        "avatar_1" -> listOf(Color(0xFFFFB4AB), Color(0xFF9E7C79)) // Coral Slate
        "avatar_2" -> listOf(Color(0xFFBAC7E3), Color(0xFF3D4758)) // Steel Slate
        "avatar_3" -> listOf(Color(0xFFD1E1FF), Color(0xFF35485E)) // Deep Blue Slate
        "avatar_4" -> listOf(Color(0xFFFFDBCB), Color(0xFFD1E1FF)) // Gold/Blue Slate (Jordan Smith style)
        "avatar_5" -> listOf(Color(0xFFA2F7B7), Color(0xFF003922)) // Forest Slate
        "avatar_6" -> listOf(Color(0xFFC4C6CF), Color(0xFF44474E)) // Classic Gray Slate
        else -> listOf(NeonPrimary, NeonSecondary)
    }

    val isLightGradients = avatarId == "avatar_4" || avatarId == "avatar_1" || avatarId == "avatar_3"

    Box(
        modifier = Modifier
            .size(size)
    ) {
        if (isCustomImage) {
            coil.compose.AsyncImage(
                model = avatarId,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, SurfaceAccent, CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                error = coil.compose.rememberAsyncImagePainter(
                    model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150"
                )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    color = if (isLightGradients) Color(0xFF002E69) else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.42).sp
                )
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, DarkBackground, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, DarkBackground, CircleShape)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
    }
}

@Composable
fun LivePulsatingCircle(size: androidx.compose.ui.unit.Dp) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(LiveRed)
    )
}

// ==========================================
// CHAT DETAIL SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatDetailScreen(viewModel: ChatViewModel, friendId: Int, navController: NavController) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    val friend = friends.firstOrNull { it.id == friendId }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val voiceRecorder = remember { com.example.util.VoiceRecorder(context) }
    val recordPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<java.io.File?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachmentPicker by remember { mutableStateOf(false) }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    if (friend == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Friend not found", color = Color.Gray)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(friend.name, friend.avatar, friend.isOnline, size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = friend.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (friend.isStreaming) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LiveRed))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Live Watching Screen", color = NeonSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text(
                                    text = if (friend.isOnline) "Active now" else "Offline",
                                    color = if (friend.isOnline) Color.Green else Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Points Counter
                    val points = userProfile?.points ?: 0
                    val context = LocalContext.current
                    var isAnimating by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isAnimating) 1.2f else 1.0f,
                        animationSpec = tween(durationMillis = 300)
                    )
                    var previousPoints by remember { mutableIntStateOf(points) }

                    LaunchedEffect(points) {
                        if (previousPoints != 0 && points > previousPoints) {
                            // Play sound
                            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP)
                            toneGen.release()
                            
                            isAnimating = true
                            delay(300)
                            isAnimating = false
                        }
                        previousPoints = points
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .scale(scale)
                    ) {
                        Text("💰", fontSize = 16.sp)
                        Text(
                            text = points.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Camera Button
                    IconButton(onClick = { /* TODO: Implement camera action */ }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                    }

                    if (friend.isStreaming) {
                        IconButton(
                            onClick = {
                                viewModel.startWatchingStream(friend)
                                navController.navigate("stream_watch/${friend.id}")
                            }
                        ) {
                            Icon(Icons.Filled.LiveTv, contentDescription = "Watch Live Stream", tint = NeonSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Earning points notice banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonPrimary.copy(alpha = 0.08f))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💰", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                    Text(
                        text = "1 Message = +1 Point! Chat details live-earn points.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Message scrolling list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .animateContentSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(activeMessages) { message ->
                    val isMe = message.senderId == "user"
                    Box(modifier = Modifier.animateContentSize()) {
                        MessageBubble(message, isMe)
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Typing indicator
            AnimatedVisibility(
                visible = textInput.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "Typing...",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Chat input bar
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    if (showEmojiPicker) {
                        EmojiPickerPanel(onEmojiSelected = { textInput += it })
                    }
                    if (showAttachmentPicker) {
                        AttachmentPickerPanel(onAttachmentSelected = { type, path -> 
                            viewModel.sendAttachmentMessage(friendId, type, path)
                            showAttachmentPicker = false
                        })
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF24292E)) // Dark Gray
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            showEmojiPicker = !showEmojiPicker
                            showAttachmentPicker = false
                        }) {
                            Icon(
                                if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.EmojiEmotions, 
                                contentDescription = "Emoji Picker", 
                                tint = Color.LightGray
                            )
                        }

                        IconButton(onClick = { 
                            showAttachmentPicker = !showAttachmentPicker
                            showEmojiPicker = false
                        }) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Attachment", 
                                tint = Color.LightGray
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Message...", color = Color.Gray, fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (recordPermissionState.status.isGranted) {
                                    if (isRecording) {
                                        voiceRecorder.stopRecording()
                                        isRecording = false
                                        audioFile?.let { viewModel.sendVoiceMessage(friendId, it.absolutePath) }
                                    } else {
                                        audioFile = java.io.File(context.cacheDir, "voice_${System.currentTimeMillis()}.mp4")
                                        voiceRecorder.startRecording(audioFile!!)
                                        isRecording = true
                                    }
                                } else {
                                    recordPermissionState.launchPermissionRequest()
                                }
                            }
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Filled.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isRecording) Color.Red else Color.LightGray
                            )
                        }

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(friendId, textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(0xFFFF6600), // Orange
                                    shape = CircleShape
                                )
                                .testTag("chat_send_btn")
                        ) {
                            // Assuming a waveform icon, I'll use a close match from Icons
                            Icon(
                                Icons.Filled.GraphicEq, 
                                contentDescription = "Send Message",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPickerPanel(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf("😀", "😂", "🥰", "😍", "🤩", "🥳", "😎", "🤔", "😲", "😭", "😡", "👍", "🔥", "🙏")
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(SurfaceCard).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(emojis) { emoji ->
            Text(
                text = emoji,
                modifier = Modifier
                    .clickable { onEmojiSelected(emoji) }
                    .padding(8.dp),
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun AttachmentPickerPanel(onAttachmentSelected: (String, String) -> Unit) {
    val attachments = listOf(
        Pair("Photo", Icons.Default.Photo),
        Pair("Video", Icons.Default.Videocam),
        Pair("Location", Icons.Default.LocationOn),
        Pair("Doc", Icons.Default.Description),
        Pair("File", Icons.Default.AttachFile)
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(SurfaceCard).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments) { attachment ->
            Column(
                modifier = Modifier
                    .clickable { onAttachmentSelected(attachment.first, "path/to/${attachment.first.lowercase()}") }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(attachment.second, contentDescription = attachment.first, tint = Color.LightGray)
                Text(text = attachment.first, color = Color.LightGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // Text Content Box
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isMe) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.dp,
                        if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.isVoiceMessage) {
                    Row(
                        modifier = Modifier.width(140.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                         Spacer(modifier = Modifier.width(8.dp))
                         Row(
                             modifier = Modifier.weight(1f),
                             horizontalArrangement = Arrangement.spacedBy(2.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             repeat(12) {
                                  Box(modifier = Modifier.width(3.dp).height((Random.nextInt(8, 24)).dp).background(Color.White.copy(alpha = 0.5f)))
                             }
                         }
                    }
                } else if (message.attachmentType != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attachment", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = message.attachmentType, color = Color.White)
                    }
                } else {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Name/Indicator/Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMe) "You • +1pt" else message.senderName,
                    color = if (isMe) NeonAccent else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                if (message.senderIsVerified) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF1E88E5), // Blue tick color
                        modifier = Modifier.size(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)) },
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

// ==========================================
// STREAMS TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LivePreviewScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val cameraController = remember {
                LifecycleCameraController(context).apply {
                    setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                }
            )
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Go Live", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.size(48.dp))
            }
            // "Go Live" trigger
            Button(
                onClick = { /* Handle actual broadcasting */ },
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).fillMaxWidth().height(56.dp)
            ) {
                Text("Start Broadcasting")
            }
        }
    } else {
        // Permission required UI
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission required", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

// Video model
data class Video(
    val id: String,
    val title: String,
    val creatorName: String,
    val thumbnail: String,
    val videoUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosTab(viewModel: ChatViewModel, navController: NavController) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    val videos = remember {
        listOf(
            Video("1", "Building a Rocket", "SpaceX Fan", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            Video("2", "Android Tips", "Kotlin Expert", "https://i.ytimg.com/vi/o8p_1s213h4/hqdefault.jpg", "https://www.youtube.com/watch?v=o8p_1s213h4"),
            Video("3", "Meditation Music", "Zen Vibes", "https://i.ytimg.com/vi/t8b1g3s7e8f/hqdefault.jpg", "https://www.youtube.com/watch?v=t8b1g3s7e8f"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video & Live Hub", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Stream Section
            item {
                Text(
                    text = "Live Broadcasts",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                UserProfileGoLiveCard(userProfile, viewModel, navController) {
                    navController.navigate("live_preview")
                }
            }
            
            val streamingFriends = friends.filter { it.isStreaming }
            if (streamingFriends.isNotEmpty()) {
                item {
                    Text(
                        text = "Currently Streaming",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                items(streamingFriends) { friend ->
                    LiveStreamCard(friend = friend) {
                        viewModel.startWatchingStream(friend)
                        navController.navigate("stream_watch/${friend.id}")
                    }
                }
            }

            // Video Section
            item {
                Text(
                    text = "Suggested Videos",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(videos) { video ->
                VideoCard(video)
            }
        }
    }
}

@Composable
fun VideoCard(video: Video) {
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder for thumbnail
                Text("Thumbnail: ${video.thumbnail}", color = Color.White)
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(video.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(video.creatorName, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun UserProfileGoLiveCard(
    profile: UserProfile?,
    viewModel: ChatViewModel,
    navController: NavController,
    onGoLive: () -> Unit
) {
    val isStreaming = profile?.isStreaming == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("host_stream_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isStreaming) LiveRed.copy(alpha = 0.12f) else SurfaceCard),
        border = BorderStroke(1.dp, if (isStreaming) LiveRed else SurfaceAccent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isStreaming) LiveRed else NeonPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isStreaming) Icons.Filled.Stop else Icons.Filled.Videocam,
                        contentDescription = "Stream Icon",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isStreaming) "Your Live Stream is ACTIVE!" else "Broadcast to Everyone",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isStreaming) "Title: ${profile?.streamTitle}" else "Host your own live room & gain visitors",
                        color = if (isStreaming) NeonAccent else Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (isStreaming) {
                        navController.navigate("stream_host")
                    } else {
                        onGoLive()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("go_live_fab"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStreaming) LiveRed else NeonPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isStreaming) "View My Live Stream Control Panel 🔴" else "Setup & GO LIVE NOW! 🔴",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun LiveStreamCard(friend: Friend, onWatchClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWatchClick() }
            .testTag("stream_card_${friend.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, SurfaceAccent)
    ) {
        Column {
            // Elegant top visualizer block that looks like video frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPrimary.copy(alpha = 0.35f), DarkBackground),
                            center = Offset(200f, 100f)
                        )
                    )
                    .drawBehind {
                        // Drawing static decorative waveform background for beauty
                        val width = size.width
                        val height = size.height
                        val pathBrush = Brush.linearGradient(listOf(NeonSecondary, NeonAccent))
                        for (i in 0..10 step 2) {
                            val ratio = i / 10f
                            val waveY = height * 0.5f + sin(ratio * 9.2f) * 40f
                            drawCircle(
                                color = NeonPrimary.copy(alpha = 0.1f * ratio),
                                radius = 20f * (1f - ratio) + 10f,
                                center = Offset(width * ratio, waveY)
                            )
                        }
                    }
                    .padding(12.dp)
            ) {
                // LIVE Badge (From Elegant Dark design HTML: bg-[#FFB4AB], text-[#690005], crimson dot #BA1A1A)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color(0xFFFFB4AB), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFBA1A1A)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        color = Color(0xFF690005),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Viewer Count Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Group, contentDescription = "Viewers", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${friend.viewersCount} watching",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Stream Overlay Text
                Text(
                    text = friend.streamTitle,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 4f)),
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            // Streamer details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(friend.name, friend.avatar, isOnline = true, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Score: ${friend.points} points",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { onWatchClick() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPrimary,
                        contentColor = Color(0xFF002E69)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Watch Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// STREAM WATCH SCREEN (Simulated live spectator)
// ==========================================
@Composable
fun StreamWatchScreen(viewModel: ChatViewModel, friendId: Int, navController: NavController) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val friend = friends.firstOrNull { it.id == friendId }
    val streamComments by viewModel.streamComments.collectAsStateWithLifecycle()
    val heartsCount by viewModel.floatingHeartsCount.collectAsStateWithLifecycle()
    
    var commentInput by remember { mutableStateOf("") }
    val commentsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll stream comments
    LaunchedEffect(streamComments.size) {
        if (streamComments.isNotEmpty()) {
            commentsListState.animateScrollToItem(streamComments.size - 1)
        }
    }

    if (friend == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Stream unavailable", color = Color.Gray)
        }
        return
    }

    // High quality live visual rendering based on who is streaming
    val streamerTheme = when (friend.avatar) {
        "avatar_1" -> listOf(Color(0xFFDB2777), Color(0xFF311042)) // magenta/wine
        "avatar_2" -> listOf(Color(0xFF4F46E5), Color(0xFF131032)) // violet/indigo
        "avatar_5" -> listOf(Color(0xFF059669), Color(0xFF0B2418)) // dark emerald
        else -> listOf(NeonSecondary, Color(0xFF0D1C2A))
    }

    // Live waves animated vectors state
    val infiniteTransition = rememberInfiniteTransition()
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Stream Header Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.stopWatchingStream()
                navController.popBackStack()
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Exit Stream", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(6.dp))

            AvatarView(friend.name, friend.avatar, isOnline = true, size = 38.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🏆 ${friend.points} pts",
                    color = NeonSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .background(LiveRed, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${friend.viewersCount + (heartsCount / 3)} spectator",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Simulated Screen Canvas (gorgeous visual interactive layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Brush.verticalGradient(streamerTheme))
                .border(1.dp, SurfaceAccent)
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    
                    // Draw nice glowing visualizer waves
                    val waveStroke = Stroke(width = 3.dp.toPx())
                    for (i in 1..3) {
                        val amplitudeMultiplier = 35f * i
                        val frequency = 0.005f * i
                        val phase = animatedProgress + (i * 1.5f)
                        
                        val pointsList = mutableListOf<Offset>()
                        for (x in 0..width.toInt() step 5) {
                            val y = height * 0.55f + sin(x * frequency + phase) * amplitudeMultiplier
                            pointsList.add(Offset(x.toFloat(), y))
                        }
                        
                        // Draw lines between adjacent points
                        for (idx in 0 until pointsList.size - 1) {
                            drawLine(
                                color = if (i == 1) NeonAccent.copy(alpha = 0.7f) else if (i == 2) NeonSecondary.copy(alpha = 0.5f) else NeonPrimary.copy(alpha = 0.3f),
                                start = pointsList[idx],
                                end = pointsList[idx + 1],
                                strokeWidth = waveStroke.width
                            )
                        }
                    }
                }
                .padding(16.dp)
        ) {
            // Title overlay
            Text(
                text = friend.streamTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 8f))
            )

            // Dynamic live streaming overlays (cam status etc)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("🎵 1080p60 • Stereo Audio", color = Color.White, fontSize = 10.sp)
            }

            // Spectators like feedback icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(NeonAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("💖 Likes: $heartsCount", color = NeonAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Rising Floating Hearts component!
            RisingHeartsCanvas(heartsCount)
        }

        // Live point alert banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeonSecondary.copy(alpha = 0.12f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💬 Chat in Stream Feed to Earn Points! 1 Comment = +1 Point! 💎",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Stream Comments stream list block
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            state = commentsListState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(streamComments) { comment ->
                StreamChatRow(comment)
            }
        }

        // Stream spectator footer actions
        Surface(
            color = SurfaceCard,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().border(0.5.dp, SurfaceAccent, RoundedCornerShape(0.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = { Text("Comment & earn points...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stream_comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonSecondary,
                        unfocusedBorderColor = SurfaceAccent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentInput.isNotBlank()) {
                                viewModel.sendStreamChatMessage(friend.id, commentInput)
                                commentInput = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Send comment button
                IconButton(
                    onClick = {
                        if (commentInput.isNotBlank()) {
                            viewModel.sendStreamChatMessage(friend.id, commentInput)
                            commentInput = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(NeonSecondary, shape = CircleShape)
                        .testTag("send_comment_btn")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send Comment", tint = DarkBackground, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // React heart button with beautiful splash feedback
                IconButton(
                    onClick = { viewModel.addHeart() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(NeonAccent, shape = CircleShape)
                        .testTag("stream_heart_btn")
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Send Love Reaction", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun StreamChatRow(comment: StreamComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (comment.isSystem) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.text,
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(comment.timestamp)),
                        color = Color.Yellow.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        } else {
            // Avatar small
            if (comment.avatarId == "avatar_user") {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonPrimary, NeonSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Y", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            } else {
                AvatarView(name = comment.senderName, avatarId = comment.avatarId, isOnline = false, size = 26.dp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.senderName,
                        color = if (comment.isMe) NeonAccent else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    if (comment.isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(NeonAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("Spectator", color = NeonAccent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(comment.timestamp)),
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light
                    )
                }
                Text(
                    text = comment.text,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Draw Floating Rising Hearts animation dynamically inside stream!
@Composable
fun RisingHeartsCanvas(heartsTrigger: Int) {
    val heartPoints = remember { mutableStateListOf<HeartAnimationModel>() }

    LaunchedEffect(heartsTrigger) {
        if (heartsTrigger > 0) {
            // Spawn a new floating heart model
            heartPoints.add(
                HeartAnimationModel(
                    startX = Random.nextFloat() * 100f + 650f,
                    speed = Random.nextFloat() * 3f + 4f,
                    swayFreq = Random.nextFloat() * 0.05f + 0.02f,
                    size = Random.nextFloat() * 10f + 15f
                )
            )
        }
    }

    // Canvas rendering based on tick frame state
    val infiniteTransition = rememberInfiniteTransition()
    val frameCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val count = frameCount // ticks updates the draw block
        val iterator = heartPoints.iterator()
        while (iterator.hasNext()) {
            val h = iterator.next()
            h.currentY -= h.speed
            h.currentX += sin(h.currentY * h.swayFreq) * 1.5f
            h.alpha -= 0.015f

            if (h.alpha <= 0f || h.currentY < 0f) {
                iterator.remove()
            } else {
                // Draw a beautiful glowing round heart shape
                drawCircle(
                    color = NeonAccent.copy(alpha = h.alpha),
                    radius = h.size,
                    center = Offset(h.currentX, h.currentY)
                )
            }
        }
    }
}

class HeartAnimationModel(
    val startX: Float,
    val speed: Float,
    val swayFreq: Float,
    val size: Float,
    var currentY: Float = 550f,
    var currentX: Float = startX,
    var alpha: Float = 1.0f
)


// ==========================================
// STREAM HOST SCREEN (Broadcasting ourselves!)
// ==========================================
@Composable
fun StreamHostScreen(viewModel: ChatViewModel, navController: NavController) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ownComments by viewModel.ownStreamComments.collectAsStateWithLifecycle()
    val activeViewers by viewModel.ownStreamViewerCount.collectAsStateWithLifecycle()

    var streamTitleInput by remember { mutableStateOf("") }
    val commentsListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val isStreaming = userProfile?.isStreaming == true

    LaunchedEffect(ownComments.size) {
        if (ownComments.isNotEmpty()) {
            commentsListState.animateScrollToItem(ownComments.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isStreaming) {
                    viewModel.stopOwnStream()
                }
                navController.popBackStack()
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Close Page", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Stream Control Panel",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            
            if (isStreaming) {
                Box(
                    modifier = Modifier
                        .background(LiveRed, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LivePulsatingCircle(size = 6.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (!isStreaming) {
            // GO LIVE SETUP VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🔴", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Launch Your Stream",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Broadcast live to all friends globally in the app! Setup are simple.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = streamTitleInput,
                    onValueChange = { streamTitleInput = it },
                    label = { Text("Enter Live Stream Topic/Title", color = NeonPrimary) },
                    placeholder = { Text("e.g., Coding Kotlin & Chill 💻🎹", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_topic_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPrimary,
                        unfocusedBorderColor = SurfaceAccent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val title = streamTitleInput.ifBlank { "Unscheduled Live Chat Session 🗣️" }
                        viewModel.startOwnStream(title)
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("host_go_live_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("START LIVE BROADCAST NOW", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        } else {
            // LIVE STREAMING PANEL ACTIVE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEF4444).copy(alpha = 0.4f), Color(0xFF161824))
                        )
                    )
                    .border(1.dp, SurfaceAccent)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STREAM ACTIVE", color = LiveRed, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        // Real-time viewer counter
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("$activeViewers watching you", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = userProfile?.streamTitle ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hosting camera feed simulator active...",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { viewModel.stopOwnStream() },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp).testTag("end_stream_btn")
                        ) {
                            Text("End Stream 🟥", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Chat input for streamer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonPrimary.copy(alpha = 0.12f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆 Chat to your viewers below! Every streamer msg also = +1 Point! 💎",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Scrolling comments from friends watching
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                state = commentsListState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ownComments) { comment ->
                    StreamChatRow(comment)
                }
            }

            // Broadcaster message field
            Surface(
                color = SurfaceCard,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().border(0.5.dp, SurfaceAccent, RoundedCornerShape(0.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var streamerMsgInput by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = streamerMsgInput,
                        onValueChange = { streamerMsgInput = it },
                        placeholder = { Text("Acknowledge stream audience...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("streamer_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LiveRed,
                            unfocusedBorderColor = SurfaceAccent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (streamerMsgInput.isNotBlank()) {
                                    viewModel.sendCommentOnOwnStream(streamerMsgInput)
                                    streamerMsgInput = ""
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (streamerMsgInput.isNotBlank()) {
                                viewModel.sendCommentOnOwnStream(streamerMsgInput)
                                streamerMsgInput = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(LiveRed, shape = CircleShape)
                            .testTag("streamer_send_btn")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}


// ==========================================
// LEADERBOARD TAB
// ==========================================
@Composable
fun LeaderboardTab(viewModel: ChatViewModel, onBackClick: (() -> Unit)? = null) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Aggregate User + Friends into a consolidated score list
    val leaderboardList = remember(friends, userProfile) {
        val userRow = userProfile?.let {
            Friend(
                id = -1, // placeholder
                name = "${it.name}",
                avatar = "avatar_user",
                points = it.points,
                isOnline = true,
                isStreaming = it.isStreaming,
                streamTitle = it.streamTitle
            )
        }
        val all = mutableListOf<Friend>()
        if (userRow != null) all.add(userRow)
        all.addAll(friends)
        
        // Sort descending
        all.sortedByDescending { it.points }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            Text(
                text = "Leaderboard",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Competitive real-time rankings • 1 message = 1 pt",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (leaderboardList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonAccent)
            }
        } else {
            // Custom Visual Podium for Top 3 rankings
            val podium1st = leaderboardList.getOrNull(0)
            val podium2nd = leaderboardList.getOrNull(1)
            val podium3rd = leaderboardList.getOrNull(2)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place Podium
                if (podium2nd != null) {
                    PodiumColumn(
                        friend = podium2nd,
                        rank = 2,
                        podiumHeight = 70.dp,
                        podiumColor = Color(0xFF94A3B8) // silver
                    )
                }

                // 1st Place Podium (stands tallest in center!)
                if (podium1st != null) {
                    PodiumColumn(
                        friend = podium1st,
                        rank = 1,
                        podiumHeight = 100.dp,
                        podiumColor = Color(0xFFFBBF24) // gold
                    )
                }

                // 3rd Place Podium
                if (podium3rd != null) {
                    PodiumColumn(
                        friend = podium3rd,
                        rank = 3,
                        podiumHeight = 55.dp,
                        podiumColor = Color(0xFFCD7F32) // bronze
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Remaining players in list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val subList = if (leaderboardList.size > 3) leaderboardList.subList(3, leaderboardList.size) else emptyList()
                items(subList) { ranker ->
                    val isCurrentUser = ranker.id == -1
                    val index = leaderboardList.indexOf(ranker) + 1

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentUser) NeonPrimary.copy(alpha = 0.12f) else SurfaceCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isCurrentUser) NeonPrimary.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank Number badge
                            Text(
                                text = "#$index",
                                color = if (isCurrentUser) NeonAccent else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.width(36.dp)
                            )

                            // Avatar View
                            if (ranker.avatar == "avatar_user") {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(NeonPrimary, NeonSecondary))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Y", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            } else {
                                AvatarView(name = ranker.name, avatarId = ranker.avatar, isOnline = false, size = 32.dp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = if (ranker.avatar == "avatar_user") "${ranker.name} (You)" else ranker.name,
                                color = Color.White,
                                fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )

                            // Points display (aligning with the theme's core payments indicator style)
                            Row(
                                modifier = Modifier
                                    .background(BrandCard, shape = CircleShape)
                                    .border(0.5.dp, SurfaceAccent, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Payments,
                                    contentDescription = "Points",
                                    tint = if (isCurrentUser) NeonAccent else NeonPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${ranker.points}",
                                    color = if (isCurrentUser) NeonAccent else NeonPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumColumn(
    friend: Friend,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    podiumColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(100.dp)
    ) {
        // Crown marker for rank 1
        if (rank == 1) {
            Text("👑", fontSize = 20.sp, modifier = Modifier.padding(bottom = 2.dp))
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Avatar
        if (friend.avatar == "avatar_user") {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPrimary, NeonSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Text("Y", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            AvatarView(name = friend.name, avatarId = friend.avatar, isOnline = false, size = 48.dp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Name
        Text(
            text = if (friend.avatar == "avatar_user") "You" else friend.name.split(" ").firstOrNull() ?: friend.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Points
        Text(
            text = "${friend.points} pts",
            color = if (friend.avatar == "avatar_user") NeonAccent else NeonSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Visual Podium Pillar Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.5.dp, podiumColor)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = podiumColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_bg")
    
    // Swirling background orbs
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1"
    )

    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Neon Primary (swirling coordinates)
            val rad1 = Math.toRadians(animOffset1.toDouble())
            val x1 = (width / 2) + (width / 3) * Math.cos(rad1).toFloat()
            val y1 = (height / 2) + (height / 4) * Math.sin(rad1).toFloat()
            drawCircle(
                color = NeonPrimary.copy(alpha = 0.14f),
                radius = width * 0.5f,
                center = Offset(x1, y1)
            )

            // Orb 2: Neon Secondary
            val rad2 = Math.toRadians(animOffset2.toDouble())
            val x2 = (width * 0.4f) + (width * 0.25f) * Math.cos(rad2).toFloat()
            val y2 = (height * 0.6f) + (height * 0.2f) * Math.sin(rad2).toFloat()
            drawCircle(
                color = NeonSecondary.copy(alpha = 0.12f),
                radius = width * 0.45f,
                center = Offset(x2, y2)
            )

            // Orb 3: Accent Swirl
            val x3 = (width * 0.6f) + (width * 0.2f) * Math.sin(rad2).toFloat()
            val y3 = (height * 0.3f) + (height * 0.15f) * Math.cos(rad1).toFloat()
            drawCircle(
                color = NeonAccent.copy(alpha = 0.11f),
                radius = width * 0.4f,
                center = Offset(x3, y3)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1219).copy(alpha = 0.72f))
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(viewModel: ChatViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    
    // Inputs
    var nameInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("Ready to chat & stream! 🚀") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var customAvatarUrl by remember { mutableStateOf("") }
    var useUrlInput by remember { mutableStateOf(false) }
    
    // Selected predefined avatar ID (default "avatar_1")
    var selectedAvatarId by remember { mutableStateOf("avatar_1") }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }

    val predefinedAvatars = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5", "avatar_6")

    LiquidGlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonPrimary, NeonSecondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forum,
                            contentDescription = "App Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pine Chat",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // End-to-End Cryptography Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = NeonPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AES-256 E2E Secured Encryption 🔒",
                            color = NeonPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Modern frosted Slider Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isSignUp) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { isSignUp = false; errorMsg = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            color = if (!isSignUp) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSignUp) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { isSignUp = true; errorMsg = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign Up",
                            color = if (isSignUp) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Inputs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isSignUp) {
                        // User Nickname
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Choose Username", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                cursorColor = NeonPrimary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Email
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                cursorColor = NeonPrimary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Short Bio
                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { bioInput = it },
                            label = { Text("Your Short Bio", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                cursorColor = NeonPrimary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Avatar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Character Avatar",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { useUrlInput = !useUrlInput }
                                ) {
                                    Icon(
                                        imageVector = if (useUrlInput) Icons.Filled.Link else Icons.Filled.Face,
                                        contentDescription = null,
                                        tint = NeonSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (useUrlInput) "Use Presets" else "Enter Image URL",
                                        color = NeonSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (useUrlInput) {
                                OutlinedTextField(
                                    value = customAvatarUrl,
                                    onValueChange = { customAvatarUrl = it },
                                    label = { Text("Paste Avatar Image URL", color = Color.Gray) },
                                    placeholder = { Text("https://images.unsplash.com/...", color = Color.DarkGray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonSecondary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        cursorColor = NeonSecondary
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(predefinedAvatars) { avatarKey ->
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (selectedAvatarId == avatarKey) 3.dp else 1.dp,
                                                    color = if (selectedAvatarId == avatarKey) NeonSecondary else Color.White.copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedAvatarId = avatarKey }
                                                .padding(3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AvatarView(
                                                name = "A",
                                                avatarId = avatarKey,
                                                isOnline = false,
                                                size = 36.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Security Password
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("App Security Password Key", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            cursorColor = NeonPrimary
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action button
                Button(
                    onClick = {
                        if (passwordInput.isBlank()) {
                            errorMsg = "Password key cannot be empty."
                            return@Button
                        }
                        if (isSignUp && nameInput.isBlank()) {
                            errorMsg = "Username cannot be empty."
                            return@Button
                        }
                        processing = true
                        
                        val avatarToSave = if (isSignUp) {
                            if (useUrlInput && customAvatarUrl.isNotBlank()) customAvatarUrl else selectedAvatarId
                        } else "avatar_user"

                        if (isSignUp) {
                            viewModel.signUp(
                                name = nameInput,
                                bio = bioInput,
                                avatar = avatarToSave,
                                email = emailInput,
                                password = passwordInput
                            ) {
                                processing = false
                            }
                        } else {
                            viewModel.signIn(emailInput, passwordInput) { success ->
                                processing = false
                                if (!success) {
                                    errorMsg = "Authentication failed. Check credentials."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignUp) NeonSecondary else NeonPrimary
                    ),
                    enabled = !processing
                ) {
                    if (processing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = if (isSignUp) "Create Encrypted Wallet & Join" else "Secure Authentication",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (isSignUp) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            processing = true
                            viewModel.skipSignUp {
                                processing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !processing
                    ) {
                        Text("Skip - Join as Guest", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(viewModel: ChatViewModel, navController: NavController) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()
    val user = userProfile ?: return

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF111B21))) {
        // Settings Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
        }

        // Profile Section (Clickable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { navController.navigate("profile_edit") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            ) {
                AvatarView(user.name, user.avatar, false, 60.dp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.name, color = Color.White, fontSize = 18.sp)
                Text("Today in emojis...", color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = Color(0xFF00A884))
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Default.AddCircleOutline, contentDescription = "Add", tint = Color(0xFF00A884))
        }

        Divider(color = Color(0xFF1F2C33))
        
        // Settings Items
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item { SettingsRow(Icons.Default.Key, "Account", "Security notifications, change number") }
            item { SettingsRow(Icons.Default.Lock, "Privacy", "Blocked accounts, disappearing messages") }
            item { SettingsRow(Icons.Default.List, "Lists", "Manage people and groups") }
            item { SettingsRow(Icons.Default.Forum, "Chats", "Theme, wallpapers, chat history") }
            item { SettingsRow(Icons.Default.VolumeUp, "Broadcasts", "Manage lists and send broadcasts") }
            item { SettingsRow(Icons.Default.Notifications, "Notifications", "Message, group & call tones") }
            item { SettingsRow(Icons.Default.Storage, "Storage and data", "Network usage, auto-download") }
            item { SettingsRow(Icons.Default.Accessibility, "Accessibility", "Increase contrast, animation") }
            
            item {
                Text("Recent Activity Log", color = Color.Gray, modifier = Modifier.padding(16.dp))
            }
            items(recentActivities) { activity ->
                Text(
                    text = "${activity.description} - ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp))}",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp)
            Text(subtitle, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun FindFriendsScreen(viewModel: ChatViewModel, navController: NavController) {
    val suggestions by viewModel.friendSuggestions.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showCustomFriendDialog by remember { mutableStateOf(false) }
    var customFriendName by remember { mutableStateOf("") }
    var customFriendBio by remember { mutableStateOf("") }

    val filteredSuggestions = remember(suggestions, searchQuery) {
        if (searchQuery.isBlank()) {
            suggestions
        } else {
            suggestions.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Find Real Friends",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Add verified profiles as your real chat partners.",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_friend_input"),
            placeholder = { Text("Search by username...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonPrimary,
                unfocusedBorderColor = SurfaceAccent,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Can't find your friend?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add any custom username as a direct contact.",
                        color = NeonSecondary,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = { showCustomFriendDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Custom", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Recommended Suggestions",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (filteredSuggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No suggestions",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Suggestions Found",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSuggestions) { candidate ->
                    SuggestionRow(
                        friend = candidate,
                        onAdd = {
                            viewModel.addFriend(candidate)
                        }
                    )
                }
            }
        }
    }

    if (showCustomFriendDialog) {
        AlertDialog(
            onDismissRequest = { showCustomFriendDialog = false },
            containerColor = SurfaceCard,
            title = {
                Text("Add Custom Friend", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customFriendName,
                        onValueChange = { customFriendName = it },
                        label = { Text("Display Name", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPrimary,
                            unfocusedBorderColor = SurfaceAccent
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customFriendBio,
                        onValueChange = { customFriendBio = it },
                        label = { Text("Short Bio", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPrimary,
                            unfocusedBorderColor = SurfaceAccent
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customFriendName.isNotBlank()) {
                            viewModel.addCustomFriend(customFriendName, customFriendBio.ifBlank { "Pine Chat User" })
                            customFriendName = ""
                            customFriendBio = ""
                            showCustomFriendDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent)
                ) {
                    Text("Add Contact", color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFriendDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun SuggestionRow(friend: Friend, onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_suggestion_${friend.name.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, SurfaceAccent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarRes = when (friend.avatar) {
                "avatar_1" -> Icons.Default.Star
                "avatar_2" -> Icons.Default.Favorite
                "avatar_3" -> Icons.Default.Build
                "avatar_4" -> Icons.Default.Home
                "avatar_5" -> Icons.Default.Settings
                "avatar_6" -> Icons.Default.Call
                else -> Icons.Default.Person
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = avatarRes,
                    contentDescription = "Avatar",
                    tint = NeonPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = friend.bio,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Friend",
                    tint = DarkBackground,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
