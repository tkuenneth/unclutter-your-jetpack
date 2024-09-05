package eu.thomaskuenneth.emojipickerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.emoji2.emojipicker.EmojiPickerView

class EmojiPickerDemoActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = defaultColorScheme()) {
                Scaffold(topBar = {
                    TopAppBar(title = {
                        Text(text = stringResource(id = R.string.app_name))
                    })
                }) { paddingValues ->
                    val initialMessage = stringResource(id = R.string.no_emoji_picked)
                    var isOpen by remember { mutableStateOf(false) }
                    var pickedEmoji by remember { mutableStateOf(initialMessage) }
                    Column(
                        modifier = Modifier
                            .padding(paddingValues = paddingValues)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center,
                            text = pickedEmoji
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isOpen = !isOpen
                        }) {
                            Text(text = stringResource(id = R.string.open))
                        }
                    }
                    if (isOpen) {
                        EmojiPicker(paddingValues = paddingValues, onDismiss = { isOpen = false }) {
                            pickedEmoji = it
                            isOpen = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPicker(
    paddingValues: PaddingValues,
    onDismiss: () -> Unit,
    onEmojiPicked: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.End
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .background(color = MaterialTheme.colorScheme.background),
                factory = { context ->
                    val picker = EmojiPickerView(context = context)
                    picker.setOnEmojiPickedListener { onEmojiPicked(it.emoji) }
                    picker
                },
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dismiss))
            }
        }
    }
}
