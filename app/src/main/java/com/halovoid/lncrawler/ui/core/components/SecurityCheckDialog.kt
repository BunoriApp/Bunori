package com.halovoid.lncrawler.ui.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun SecurityCheckDialog(
    novelName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryAccent) },
        title = { Text("Verify: $novelName", color = PrimaryText) },
        text = {
            Text(
                "This website is protected by Cloudflare. You need to complete a verification check in a browser window.\n\n" +
                "1. A browser window will open.\n" +
                "2. Complete the 'Just a moment' or Captcha check.\n" +
                "3. Once you can see the novel page, click 'DONE' in the browser window.\n\n" +
                "The app will then automatically resume your request.",
                color = SecondaryText
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Proceed")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        },
        containerColor = DarkSurface
    )
}
