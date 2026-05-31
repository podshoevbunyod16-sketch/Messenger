package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.agon.app.data.ChatMessage
import com.agon.app.data.STATUS_CONNECTED
import com.agon.app.data.novaProviders
import com.agon.app.viewmodel.NovaMindViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: NovaMindViewModel, onOpenKeys: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val activeProvider = novaProviders.firstOrNull { it.id == settings.activeProviderId } ?: novaProviders.first()
    val activeModel = settings.selectedModels[activeProvider.id] ?: activeProvider.models.first()
    val connected = settings.statuses[activeProvider.id] == STATUS_CONNECTED

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070A12), MaterialTheme.colorScheme.background)))
            .imePadding(),
    ) {
        ChatHeader(activeProvider.name, activeProvider.shortName, Color(activeProvider.accent), activeModel, connected, onOpenKeys)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages) { message -> ChatBubble(message) }
            item {
                AnimatedVisibility(isThinking) { TypingBubble(activeProvider.shortName, Color(activeProvider.accent)) }
                Spacer(Modifier.height(8.dp))
            }
        }
        Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (connected) "Ask ${activeProvider.name}..." else "Connect API key first...") },
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.sendMessage(input)
                        input = ""
                    }),
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendMessage(input); input = "" },
                    enabled = input.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (input.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(Icons.Default.Send, null, tint = if (input.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(provider: String, shortName: String, accent: Color, model: String, connected: Boolean, onOpenKeys: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xE60D1324)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProviderAvatar(shortName, accent, 46)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Chat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("$provider · $model", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB7C2DD), maxLines = 1)
            }
            AssistChip(onClick = onOpenKeys, label = { Text(if (connected) "Connected" else "Setup") }, leadingIcon = { Icon(Icons.Default.SmartToy, null, Modifier.size(16.dp)) })
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (message.fromUser) 22.dp else 6.dp,
                bottomEnd = if (message.fromUser) 6.dp else 22.dp,
            ),
            color = if (message.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(if (message.fromUser) 0.82f else 0.90f),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (message.fromUser) "You" else "${message.providerName} · ${message.modelName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (message.fromUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(message.text, color = if (message.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun TypingBubble(shortName: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProviderAvatar(shortName, accent, 34)
        Spacer(Modifier.width(10.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text("Thinking…", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
