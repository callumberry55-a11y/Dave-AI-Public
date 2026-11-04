package com.local.daveai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.local.daveai.ui.theme.DaveTheme
import kotlinx.coroutines.launch

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "attachment"
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToLiveCanvas: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
    val coroutineScope = rememberCoroutineScope()

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val currentPersona by viewModel.currentPersona.collectAsStateWithLifecycle()
    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var showPersonaMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.IMAGE, getFileName(context, it)) }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.VIDEO, getFileName(context, it)) }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.PDF, getFileName(context, it)) }
    }

    val codePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.CODE, getFileName(context, it)) }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val type = viewModel.determineAttachmentType(it)
            viewModel.addAttachment(it, type, getFileName(context, it))
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
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
                                            text = "Persona: ${currentPersona.displayName}",
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
                                IconButton(onClick = onNavigateToLiveCanvas) {
                                    Icon(
                                        imageVector = Icons.Rounded.EditNote,
                                        contentDescription = "Live Collaborative Canvas",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showPersonaMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Psychology,
                                            contentDescription = "Select Emotional Persona",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showPersonaMenu,
                                        onDismissRequest = { showPersonaMenu = false }
                                    ) {
                                        Text(
                                            text = "Select Sentient Persona",
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
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Horizontal scrolling multi-attachment preview carousel above the input bar
                                if (pendingAttachments.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(pendingAttachments, key = { it.uri }) { att ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.width(130.dp)
                                            ) {
                                                Box(modifier = Modifier.padding(8.dp)) {
                                                    Column {
                                                        if (att.type == AttachmentType.IMAGE) {
                                                            AsyncImage(
                                                                model = Uri.parse(att.uri),
                                                                contentDescription = att.name,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(64.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(64.dp)
                                                                    .background(
                                                                        MaterialTheme.colorScheme.primaryContainer,
                                                                        RoundedCornerShape(8.dp)
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = when (att.type) {
                                                                        AttachmentType.VIDEO -> Icons.Rounded.Videocam
                                                                        AttachmentType.PDF -> Icons.Rounded.PictureAsPdf
                                                                        AttachmentType.CODE -> Icons.Rounded.Code
                                                                        AttachmentType.DOCUMENT -> Icons.Rounded.Description
                                                                        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
                                                                    },
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = att.name,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.removeAttachment(att.uri) },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .size(24.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Remove attachment",
                                                            modifier = Modifier.size(14.dp),
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
                                        IconButton(
                                            onClick = { showAttachmentMenu = true },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AttachFile,
                                                contentDescription = "Attach file, image, video or document",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showAttachmentMenu,
                                            onDismissRequest = { showAttachmentMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Attach Image") },
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    imagePickerLauncher.launch("image/*")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Rounded.Image, contentDescription = null)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Attach Video") },
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    videoPickerLauncher.launch("video/*")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Rounded.Videocam, contentDescription = null)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Attach PDF") },
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    pdfPickerLauncher.launch("application/pdf")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Attach Code File") },
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    codePickerLauncher.launch("*/*")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Rounded.Code, contentDescription = null)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Attach Document / File") },
                                                onClick = {
                                                    showAttachmentMenu = false
                                                    documentPickerLauncher.launch("*/*")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, contentDescription = null)
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { viewModel.updateInputText(it) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp),
                                        placeholder = { Text("Ask Dave anything...") },
                                        shape = RoundedCornerShape(28.dp),
                                        maxLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledIconButton(
                                        onClick = { viewModel.sendMessage(inputText) },
                                        enabled = (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) && !isLoading,
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
                                    onSpeak = { text -> viewModel.speakMessage(text) }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
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
                        text = "Adaptive Layout & Personas",
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
    onSpeak: (String) -> Unit = {}
) {
    val isUser = message.isUser
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
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
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val resolvedAttachments = message.getResolvedAttachments()
                if (resolvedAttachments.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        resolvedAttachments.forEach { att ->
                            if (att.type == AttachmentType.IMAGE) {
                                AsyncImage(
                                    model = Uri.parse(att.uri),
                                    contentDescription = att.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (att.type) {
                                                AttachmentType.VIDEO -> Icons.Rounded.Videocam
                                                AttachmentType.PDF -> Icons.Rounded.PictureAsPdf
                                                AttachmentType.CODE -> Icons.Rounded.Code
                                                AttachmentType.DOCUMENT -> Icons.Rounded.Description
                                                else -> Icons.AutoMirrored.Rounded.InsertDriveFile
                                            },
                                            contentDescription = null,
                                            tint = contentColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = att.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
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
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.text))
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

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun ChatScreenPreview() {
    DaveTheme {
        ChatScreen()
    }
}
