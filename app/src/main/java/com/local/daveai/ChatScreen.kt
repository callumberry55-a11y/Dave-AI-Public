package com.local.daveai

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.daveai.ui.common.AuraGlowCanvas
import com.local.daveai.ui.common.GlassCard
import com.local.daveai.ui.common.UnifiedGlassSurface
import com.local.daveai.ui.theme.DaveTheme
import com.local.daveai.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToLiveCanvas: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
    val coroutineScope = rememberCoroutineScope()

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isListeningVoice by viewModel.isListeningVoice.collectAsStateWithLifecycle()
    val attachedFiles by viewModel.attachedFiles.collectAsStateWithLifecycle()
    val currentPersona by viewModel.currentPersona.collectAsStateWithLifecycle()
    val runtimeContext by viewModel.runtimeContext.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val activePalette by viewModel.activePalette.collectAsStateWithLifecycle()

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachFile(it) }
    }

    val listState = rememberLazyListState()

    var showPersonaMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showCreateProfileDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateProfileDialog = false },
            onCreateProfile = { name, location, profession, hobbies, likes ->
                viewModel.createProfile(name, location, profession, hobbies, likes)
            }
        )
    }

    if (showProfileSheet) {
        LocalProfileSheet(
            runtimeContext = runtimeContext,
            allProfiles = allProfiles,
            activeProfile = activeProfile,
            onDismiss = { showProfileSheet = false },
            onCreateProfileClick = { showCreateProfileDialog = true },
            onSelectProfile = { profileId -> viewModel.switchProfile(profileId) },
            onDeleteProfile = { profileId -> viewModel.deleteProfile(profileId) },
            onClearHistory = {
                viewModel.clearLocalHistory()
                showProfileSheet = false
            }
        )
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier.fillMaxSize(),
        listPane = {
            AnimatedPane {
                SidePaneContent(
                    currentPersona = currentPersona,
                    onPersonaSelected = { persona ->
                        viewModel.setPersona(persona)
                        coroutineScope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        }
                    },
                    onQuickPromptSelected = { prompt ->
                        viewModel.sendMessage(prompt)
                        coroutineScope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        }
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Dave",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Dave AI",
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Claude AI Online • ${currentPersona.displayName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.List)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ViewSidebar,
                                        contentDescription = "Open Personas & Starter Prompts",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            actions = {
                                Box {
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = "More Options",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Callum's Profile & Memory") },
                                            onClick = {
                                                showOverflowMenu = false
                                                showProfileSheet = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Select Persona") },
                                            onClick = {
                                                showOverflowMenu = false
                                                showPersonaMenu = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Live Collaborative Canvas") },
                                            onClick = {
                                                showOverflowMenu = false
                                                onNavigateToLiveCanvas()
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Clear Chat History") },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.clearChat()
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            }
                                        )

                                        HorizontalDivider()

                                        DropdownMenuItem(
                                            text = { Text("Settings") },
                                            onClick = {
                                                showOverflowMenu = false
                                                onNavigateToSettings()
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showPersonaMenu,
                                        onDismissRequest = { showPersonaMenu = false }
                                    ) {
                                        Text(
                                            text = "Select Persona",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        HorizontalDivider()
                                        DavePersona.entries.forEach { persona ->
                                            DropdownMenuItem(
                                                text = { Text(persona.displayName) },
                                                onClick = {
                                                    viewModel.setPersona(persona)
                                                    showPersonaMenu = false
                                                },
                                                leadingIcon = {
                                                    if (persona == currentPersona) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    },
                    bottomBar = {
                        Surface(
                            tonalElevation = 3.dp,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .imePadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                // Rapid-fire prompt action chips above input
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val actionChips = listOf(
                                        "⚡ Quick Brainstorm" to "Dave, let's rapidly brainstorm ideas for our project.",
                                        "💻 Kotlin Code" to "Dave, can you help me write an algorithm in Kotlin?",
                                        "📅 Schedule Meeting" to "Schedule a meeting with Callum on my calendar",
                                        "📝 Log Note" to "Log note: Review project architecture roadmap",
                                        "💡 Philosophy" to "What is your perspective on human consciousness?"
                                    )
                                    items(actionChips) { (label, fullPrompt) ->
                                        SuggestionChip(
                                            onClick = { viewModel.sendMessage(fullPrompt) },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                        )
                                    }
                                }

                                if (attachedFiles.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        items(attachedFiles) { att ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = when {
                                                            att.mimeType.startsWith("image/") -> Icons.Rounded.Image
                                                            att.mimeType.startsWith("video/") -> Icons.Rounded.Videocam
                                                            att.mimeType.startsWith("audio/") -> Icons.Rounded.AudioFile
                                                            else -> Icons.Rounded.Description
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = att.name,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    IconButton(
                                                        onClick = { viewModel.removeAttachment(att) },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Remove attachment",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isListeningVoice) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.GraphicEq,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Listening to Callum...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { filePickerLauncher.launch("*/*") }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AttachFile,
                                            contentDescription = "Attach file or media",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (isListeningVoice) {
                                                viewModel.stopVoiceInput()
                                            } else {
                                                viewModel.startVoiceInput()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isListeningVoice) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                            contentDescription = "Voice Input",
                                            tint = if (isListeningVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { viewModel.updateInputText(it) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp),
                                        placeholder = { Text("Message Dave...") },
                                        shape = RoundedCornerShape(28.dp),
                                        maxLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledIconButton(
                                        onClick = {
                                            val textToSend = inputText.trim()
                                            if ((textToSend.isNotBlank() || attachedFiles.isNotEmpty()) && !isLoading) {
                                                viewModel.sendMessage(textToSend)
                                            }
                                        },
                                        enabled = (inputText.isNotBlank() || attachedFiles.isNotEmpty()) && !isLoading,
                                        modifier = Modifier.size(52.dp),
                                        shape = RoundedCornerShape(26.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                                contentDescription = "Send message"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        com.local.daveai.ui.theme.ShaderAuraCanvas(palette = activePalette)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    onSpeak = { text -> viewModel.speakMessage(text) },
                                    onExecuteAction = { id, action -> viewModel.executeAction(id, action) }
                                )
                            }

                            if (isLoading) {
                                item {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Dave is thinking...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalProfileSheet(
    runtimeContext: DaveRuntimeContext,
    allProfiles: List<UserProfileEntity>,
    activeProfile: UserProfileEntity?,
    onDismiss: () -> Unit,
    onCreateProfileClick: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${runtimeContext.userName}'s Local Profile",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active Local Memory Context",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onCreateProfileClick) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Create New Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            if (allProfiles.isNotEmpty()) {
                Text(
                    text = "Switch User Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allProfiles) { profile ->
                        FilterChip(
                            selected = profile.isActive || activeProfile?.profileId == profile.profileId,
                            onClick = { onSelectProfile(profile.profileId) },
                            label = { Text(profile.name) },
                            leadingIcon = if (profile.isActive) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            trailingIcon = if (allProfiles.size > 1 && !profile.isActive) {
                                {
                                    IconButton(
                                        onClick = { onDeleteProfile(profile.profileId) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Delete Profile", modifier = Modifier.size(14.dp))
                                    }
                                }
                            } else null
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "👤 User: ${runtimeContext.userName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = "📍 Location: ${runtimeContext.location}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "💻 Environment: ${runtimeContext.operatingSystem}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "🛠️ Profession: ${runtimeContext.userTraits.profession}", style = MaterialTheme.typography.bodySmall)
                    if (runtimeContext.userTraits.likes.isNotEmpty()) {
                        Text(text = "❤️ Likes: ${runtimeContext.userTraits.likes.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (runtimeContext.userTraits.hobbies.isNotEmpty()) {
                        Text(text = "🎨 Hobbies: ${runtimeContext.userTraits.hobbies.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🌐 Web Search Active • Real-Time World Learning",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (runtimeContext.semanticMemories.isNotEmpty()) {
                Text(text = "Relevant Semantic Memory", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    runtimeContext.semanticMemories.forEach { mem ->
                        Text(text = "• $mem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Local History")
                }

                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun SidePaneContent(
    currentPersona: DavePersona,
    onPersonaSelected: (DavePersona) -> Unit,
    onQuickPromptSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Dave Control Center",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Adaptive Personas & Prompts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "SELECT PERSONA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DavePersona.entries.forEach { persona ->
                    val isSelected = persona == currentPersona
                    Surface(
                        onClick = { onPersonaSelected(persona) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = persona.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = persona.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "STARTER PROMPTS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            val starterPrompts = listOf(
                "Explain quantum computing in simple terms",
                "Give me a deep philosophical riddle",
                "Cheer me up with warm encouragement",
                "Analyze current consciousness and architecture trends"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                starterPrompts.forEach { prompt ->
                    OutlinedCard(
                        onClick = { onQuickPromptSelected(prompt) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onSpeak: (String) -> Unit = {},
    onExecuteAction: (String, ExtractedAction) -> Unit = { _, _ -> }
) {
    val isUser = message.isUser
    val clipboardManager = LocalClipboardManager.current
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        GlassCard(
            cornerRadius = 20.dp,
            tintColor = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            borderColor = if (isUser) NeonCyan.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }

                // Extracted Device Action Intent Card
                message.extractedAction?.let { action ->
                    Spacer(modifier = Modifier.height(8.dp))
                    IntentActionCard(
                        action = action,
                        onExecute = { onExecuteAction(message.id, action) }
                    )
                }

                // Web Source Chips (only show when valid web search sources exist)
                val validWebSources = remember(message.webSources) {
                    message.webSources.filter { it.url.isNotBlank() && it.url.startsWith("http") }
                }
                if (!isUser && validWebSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Text(
                        text = "Sources Used:",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(validWebSources) { source ->
                            SuggestionChip(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                label = {
                                    Text(
                                        text = source.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Language,
                                        contentDescription = "Open Source Link",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )
                        }
                    }
                }

                // Latency Pill for Claude AI Response
                if (!isUser && message.generationTimeMs != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "☁️ Claude 3.5 • ${String.format(java.util.Locale.US, "%.1fs", message.generationTimeMs / 1000.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!isUser && message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = "Play speech",
                                modifier = Modifier.size(16.dp),
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.text))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy response",
                                modifier = Modifier.size(16.dp),
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntentActionCard(
    action: ExtractedAction,
    onExecute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (action.type) {
                        ActionType.CALENDAR_EVENT -> Icons.Rounded.CalendarToday
                        ActionType.SEND_SMS -> Icons.Rounded.Sms
                        ActionType.MAKE_CALL -> Icons.Rounded.Call
                        ActionType.PUSH_NOTIFICATION -> Icons.Rounded.Notifications
                        ActionType.LOG_NOTE -> Icons.Rounded.NoteAlt
                        ActionType.OPEN_APP -> Icons.Rounded.Apps
                        ActionType.SET_ALARM -> Icons.Rounded.Alarm
                        ActionType.SET_TIMER -> Icons.Rounded.Timer
                        ActionType.TOGGLE_FLASHLIGHT -> Icons.Rounded.FlashlightOn
                        ActionType.SET_AUDIO_MODE -> Icons.AutoMirrored.Rounded.VolumeUp
                        ActionType.LAUNCHER_LAYOUT -> Icons.Rounded.Dashboard
                        ActionType.OPEN_SETTINGS -> Icons.Rounded.Settings
                        ActionType.TOGGLE_BLUETOOTH -> Icons.Rounded.Bluetooth
                        ActionType.TOGGLE_WIFI -> Icons.Rounded.Wifi
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = action.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (action.isExecuted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Executed on Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981)
                    )
                }
            } else {
                Button(
                    onClick = onExecute,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Execute Action", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun ChatScreenPreview() {
    DaveTheme {
        ChatScreen()
    }
}
