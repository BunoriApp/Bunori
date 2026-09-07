package com.halovoid.lncrawler.ui.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.R
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun WelcomeScreen(
    onNext: () -> Unit
) {
    OnboardingStep(
        title = "Welcome to LNCrawler",
        subtitle = "Search, discover, and download light novels from multiple sources — all in one place.",
        buttonText = "Get Started",
        onNext = onNext
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 12.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.Search,
                    title = "Search",
                    description = "Find novels across your available sources."
                )

                FeatureRow(
                    icon = Icons.Default.Extension,
                    title = "Multiple sources",
                    description = "Browse content from different crawlers in one place."
                )

                FeatureRow(
                    icon = Icons.Default.Download,
                    title = "Download & read",
                    description = "Save novels for offline reading."
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryText.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                lineHeight = 20.sp
            )
        }
    }
}
