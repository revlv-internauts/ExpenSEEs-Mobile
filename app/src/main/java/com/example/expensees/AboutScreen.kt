package com.example.expensees.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.R

@Composable
fun AboutScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Back Button and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = Color(0xFF1F2937)
                )
            }
            Text(
                text = "About Us",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp
                ),
                color = Color(0xFF734656),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(40.dp)) // Placeholder for layout balance
        }

        // About the App Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "About the App",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                ),
                color = Color(0xFF734656),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "ExpenSEEs is a mobile expense tracker app designed to help users manage their daily expenses with ease. Built over six weeks in 2025, it reflects our commitment to creating a simple yet powerful tool for financial control. With intuitive features for tracking, categorizing, and visualizing expenses, ExpenSEEs empowers users to gain better insight into their spending habits.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = Color(0xFF4B5563),
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Our Team Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Our Team",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                ),
                color = Color(0xFF734656),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            // Team Member 1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Profile Image
                Image(
                    painter = painterResource(id = R.drawable.andrew),
                    contentDescription = "Andrew Abarientos Profile",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF734656), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Andrew Emmanuel A. Abarientos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Frontend Developer",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color(0xFF734656),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Designed and developed the mobile frontend, ensuring an intuitive and user-friendly experience.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Start
                )
            }
            // Team Member 2
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Profile Image
                Image(
                    painter = painterResource(id = R.drawable.al),
                    contentDescription = "Al Francis Paz Profile",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF734656), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Al Francis B. Paz",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Backend Developer",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color(0xFF734656),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Managed backend development, ensuring seamless functionality and performance.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Start
                )
            }
        }

        // Our Journey Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Our Journey",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                ),
                color = Color(0xFF734656),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Developed as part of our internship at REVLV, ExpenSEEs is a milestone for us as upcoming 4th-year Computer Engineering students at Ateneo de Naga University. This project showcases our dedication, creativity, and technical skills, marking a significant step in our journey as future engineers.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = Color(0xFF4B5563),
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Thank you for using ExpenSEEs!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = Color(0xFF734656),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Version Information
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            ),
            color = Color(0xFF4B5563),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}