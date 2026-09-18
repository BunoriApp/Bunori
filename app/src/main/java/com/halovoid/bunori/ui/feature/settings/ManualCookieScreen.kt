package com.halovoid.bunori.ui.feature.settings

import android.net.Uri
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.bunori.ui.core.components.AppTopBar
import com.halovoid.bunori.ui.core.theme.*

@Composable
fun ManualCookieScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var siteUrl by remember { mutableStateOf("https://www.novelupdates.com") }
    var rawCookies by remember { mutableStateOf("") }
    var activeCookiesText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manual Cookie Injection",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Guidance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "How this works",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You can solve Cloudflare on your PC browser and copy your cookies (especially cf_clearance). Paste them here along with the website URL to authenticate the app immediately without solving captchas on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input: Base Site URL
            Text(
                text = "Target Website",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = siteUrl,
                onValueChange = { siteUrl = it },
                label = { Text("Base Site URL") },
                placeholder = { Text("https://www.novelupdates.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    focusedLabelColor = BrandAccent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input: Cookies
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw Cookie String",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                TextButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            rawCookies = clip
                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", color = BrandAccent, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = rawCookies,
                onValueChange = { rawCookies = it },
                label = { Text("Cookies") },
                placeholder = { Text("cf_clearance=abcd1234...; _ga=GA1.1...;") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    focusedLabelColor = BrandAccent
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Format: name=value; name2=value2; or one pair per line.",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val target = normalizeUrl(siteUrl)
                        if (target == null) {
                            Toast.makeText(context, "Please enter a valid website URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val count = injectCookies(target, rawCookies)
                        if (count > 0) {
                            Toast.makeText(context, "Injected $count cookie(s) successfully!", Toast.LENGTH_LONG).show()
                            val cm = CookieManager.getInstance()
                            activeCookiesText = cm.getCookie(target) ?: "No cookies found"
                        } else {
                            Toast.makeText(context, "No valid cookies found (must contain name=value)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.Cookie, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inject Cookies")
                }

                OutlinedButton(
                    onClick = {
                        val target = normalizeUrl(siteUrl)
                        if (target == null) {
                            Toast.makeText(context, "Please enter a valid website URL", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        val cm = CookieManager.getInstance()
                        val cookies = cm.getCookie(target)
                        activeCookiesText = if (cookies.isNullOrBlank()) "No active cookies for this URL" else cookies
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inspect Active")
                }
            }

            // Inspection display card
            if (activeCookiesText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Cookies in Store:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandAccent
                            )
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText,
                                modifier = Modifier.clickable { activeCookiesText = null }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeCookiesText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryText,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val target = normalizeUrl(siteUrl) ?: return@OutlinedButton
                                clearCookiesForUrl(target)
                                activeCookiesText = "All cookies cleared for this site"
                                Toast.makeText(context, "Cleared cookies for this site", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Cookies for this Site", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun normalizeUrl(raw: String): String? {
    var trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        trimmed = "https://$trimmed"
    }
    val uri = try { Uri.parse(trimmed) } catch (_: Exception) { return null }
    val host = uri.host ?: return null
    return "${uri.scheme}://$host"
}

private fun injectCookies(baseUrl: String, rawCookies: String): Int {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)

    val host = Uri.parse(baseUrl).host ?: return 0
    val rootDomain = host.removePrefix("www.")

    var count = 0
    val items = rawCookies.split(";", "\n").map { it.trim() }.filter { it.contains("=") }

    for (item in items) {
        val split = item.split("=", limit = 2)
        if (split.size == 2) {
            val name = split[0].trim()
            val value = split[1].trim()
            if (name.isNotBlank()) {
                val pair = "$name=$value"
                cookieManager.setCookie(baseUrl, "$pair; Path=/")
                cookieManager.setCookie(baseUrl, "$pair; Domain=$host; Path=/")
                if (rootDomain != host) {
                    cookieManager.setCookie(baseUrl, "$pair; Domain=.$rootDomain; Path=/")
                }
                count++
            }
        }
    }
    cookieManager.flush()
    return count
}

private fun clearCookiesForUrl(baseUrl: String) {
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(baseUrl) ?: return
    val host = Uri.parse(baseUrl).host ?: ""
    val rootDomain = host.removePrefix("www.")

    val items = cookies.split(";").map { it.trim() }.filter { it.contains("=") }
    for (item in items) {
        val name = item.substringBefore("=").trim()
        if (name.isNotBlank()) {
            cookieManager.setCookie(baseUrl, "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
            if (host.isNotBlank()) {
                cookieManager.setCookie(baseUrl, "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=$host; Path=/")
                if (rootDomain != host) {
                    cookieManager.setCookie(baseUrl, "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=.$rootDomain; Path=/")
                }
            }
        }
    }
    cookieManager.flush()
}
