package com.example.intrusiondetector.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Кастомний Compose Canvas-компонент прямокутного дизайну, що реалізує графічний 2D-індикатор.
 * Динамічно реагує на зміну координат сенсора по осях X та Y за допомогою рухомого червоного квадрата.
 */
@Composable
fun BubbleLevelCanvas(
    x: Float,
    y: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier,
) {
    // Постійне червоне забарвлення маркеру відповідно до побажань
    val markerColor = Color(0xFFE53935)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Графічний Індикатор Положення (Canvas)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Прямокутний контейнер для 2D-полотна Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RectangleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RectangleShape,
                    ),
            ) {
                Canvas(
                    modifier = Modifier.size(200.dp),
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val halfWidth = size.width / 2f

                    // Прямокутні рамки мішені
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        topLeft = Offset(center.x - halfWidth * 0.8f, center.y - halfWidth * 0.8f),
                        size = Size(halfWidth * 1.6f, halfWidth * 1.6f),
                        style = Stroke(width = 2f),
                    )
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.8f),
                        topLeft = Offset(center.x - halfWidth * 0.4f, center.y - halfWidth * 0.4f),
                        size = Size(halfWidth * 0.8f, halfWidth * 0.8f),
                        style = Stroke(width = 2f),
                    )
                    drawRect(
                        color = Color.Red.copy(alpha = 0.3f),
                        topLeft = Offset(center.x - halfWidth * 0.15f, center.y - halfWidth * 0.15f),
                        size = Size(halfWidth * 0.3f, halfWidth * 0.3f),
                        style = Stroke(width = 3f),
                    )

                    // Малювання штрихових ліній перехрестя
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y),
                        strokeWidth = 2f,
                        pathEffect = dashPathEffect,
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        strokeWidth = 2f,
                        pathEffect = dashPathEffect,
                    )

                    // Розрахунок зміщення маркера на основі координат сенсора X та Y
                    val maxOffset = halfWidth * 0.75f
                    val offsetX = (-x / 9.81f).coerceIn(-1f, 1f) * maxOffset
                    val offsetY = (y / 9.81f).coerceIn(-1f, 1f) * maxOffset

                    val markerCenter = Offset(center.x + offsetX, center.y + offsetY)
                    val markerSize = halfWidth * 0.28f

                    // Малювання стабільно червоного прямокутного маркера
                    drawRect(
                        color = markerColor,
                        topLeft = Offset(markerCenter.x - markerSize / 2f, markerCenter.y - markerSize / 2f),
                        size = Size(markerSize, markerSize),
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = Offset(markerCenter.x - markerSize * 0.2f, markerCenter.y - markerSize * 0.2f),
                        size = Size(markerSize * 0.4f, markerSize * 0.4f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Текстовий статус стану
            Text(
                text = if (isLevel) "ГОРИЗОНТАЛЬНО (РІВНО)" else "ВІДХИЛЕННЯ / НАХИЛ",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = markerColor,
                fontSize = 14.sp,
            )
        }
    }
}
