package com.example.loradigitalvoice.ui.text

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TextScreen(
    rxText: String,
    onSendText: suspend (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }
    
    // Simple state to hold chat history locally for display
    var messages by remember { mutableStateOf(listOf<String>()) }
    
    // Update local messages when new rxText arrives
    LaunchedEffect(rxText) {
        if (rxText.isNotEmpty() && !messages.contains("RX: $rxText")) {
            messages = messages + "RX: $rxText"
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "LoRa Text Chat", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isRx = msg.startsWith("RX:")
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRx) MaterialTheme.colorScheme.secondaryContainer 
                                         else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f).wrapContentWidth(if (isRx) Alignment.Start else Alignment.End)
                ) {
                    Text(
                        text = if (isRx) msg.removePrefix("RX: ") else msg.removePrefix("TX: "),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                singleLine = true,
                enabled = !isSending
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val textToSend = inputText
                        inputText = ""
                        isSending = true
                        messages = messages + "TX: $textToSend"
                        
                        coroutineScope.launch {
                            val success = onSendText(textToSend)
                            isSending = false
                            if (!success) {
                                messages = messages + "RX: [Error sending message]"
                            }
                        }
                    }
                },
                enabled = !isSending && inputText.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("➤")
            }
        }
    }
}
