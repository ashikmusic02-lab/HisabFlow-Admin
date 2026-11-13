package com.example.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.auth.AdminAuthManager
import com.example.data.auth.AuthGateState
import com.example.ui.ads.AdsScreen
import com.example.ui.ads.AdsViewModel
import com.example.ui.auth.SignInScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.notifications.NotificationsViewModel
import kotlinx.coroutines.launch

enum class AdminTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        labelRes = R.string.tab_dashboard,
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    ADS(
        labelRes = R.string.tab_ads,
        selectedIcon = Icons.Rounded.Campaign,
        unselectedIcon = Icons.Outlined.Campaign,
        testTag = "nav_ads"
    ),
    NOTIFICATIONS(
        labelRes = R.string.tab_notifications,
        selectedIcon = Icons.Rounded.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        testTag = "nav_notifications"
    )
}

@Composable
fun AdminAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authState by remember { mutableStateOf<AuthGateState>(AuthGateState.Checking) }
    var isSigningIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authState = AdminAuthManager.evaluateCurrentSession(context)
    }

    when (val state = authState) {
        is AuthGateState.Checking -> {
            SignInScreen(
                isLoading = true,
                errorMessage = null,
                isFirebaseMissing = false,
                onGoogleSignInClick = {}
            )
        }

        is AuthGateState.FirebaseNotConfigured -> {
            SignInScreen(
                isLoading = false,
                errorMessage = state.message,
                isFirebaseMissing = true,
                onGoogleSignInClick = {}
            )
        }

        is AuthGateState.SignedOut -> {
            SignInScreen(
                isLoading = isSigningIn,
                errorMessage = state.errorMessage,
                isFirebaseMissing = false,
                onGoogleSignInClick = {
                    val activity = context as? Activity ?: return@SignInScreen
                    isSigningIn = true
                    scope.launch {
                        authState = AdminAuthManager.signInWithGoogle(activity)
                        isSigningIn = false
                    }
                }
            )
        }

        is AuthGateState.SignedIn -> {
            key(state.uid) {
                AdminAuthenticatedShell(
                    userUid = state.uid,
                    userEmail = state.email,
                    onSignOut = {
                        scope.launch {
                            AdminAuthManager.signOut(context)
                            authState = AuthGateState.SignedOut()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAuthenticatedShell(
    userUid: String,
    userEmail: String,
    onSignOut: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(AdminTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    val dashboardViewModel: DashboardViewModel = viewModel(key = "dash_$userUid")
    val adsViewModel: AdsViewModel = viewModel(key = "ads_$userUid")
    val notificationsViewModel: NotificationsViewModel = viewModel(key = "notif_$userUid")

    LaunchedEffect(adsViewModel) {
        adsViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(notificationsViewModel) {
        notificationsViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = selectedTab.labelRes),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = stringResource(id = R.string.sign_out)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        AdminTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        AdminTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        AdminTab.DASHBOARD -> {
                            val dashState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                            val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
                            DashboardScreen(
                                uiState = dashState,
                                isRefreshing = isRefreshing,
                                onRefresh = dashboardViewModel::refresh,
                                onRetry = dashboardViewModel::startListening
                            )
                        }

                        AdminTab.ADS -> {
                            val adsState by adsViewModel.uiState.collectAsStateWithLifecycle()
                            AdsScreen(
                                uiState = adsState,
                                onToggleSwitch = adsViewModel::toggleAdSwitch,
                                onIntervalInputChange = adsViewModel::onIntervalInputChanged,
                                onSaveInterval = adsViewModel::saveInterstitialInterval,
                                onRetry = adsViewModel::startListening
                            )
                        }

                        AdminTab.NOTIFICATIONS -> {
                            val formState by notificationsViewModel.formState.collectAsStateWithLifecycle()
                            val historyState by notificationsViewModel.historyState.collectAsStateWithLifecycle()
                            NotificationsScreen(
                                formState = formState,
                                historyState = historyState,
                                onTitleChange = notificationsViewModel::onTitleChange,
                                onBodyChange = notificationsViewModel::onBodyChange,
                                onImageUrlChange = notificationsViewModel::onImageUrlChange,
                                onScreenSelected = notificationsViewModel::onScreenSelected,
                                onSendTestClick = notificationsViewModel::sendTest,
                                onSendAllClick = notificationsViewModel::requestSendAllConfirmation,
                                onConfirmSendAll = notificationsViewModel::confirmSendAll,
                                onDismissConfirmAll = notificationsViewModel::dismissSendAllConfirmation,
                                onRetryHistory = notificationsViewModel::observeHistory
                            )
                        }
                    }
                }
            }
        }
    }
}
