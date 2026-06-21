package com.tejaslabs.beauty.ai.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class FeatureIcon {
    data class Vector(val imageVector: ImageVector) : FeatureIcon()
    data class Emoji(val char: String) : FeatureIcon()
}

@Composable
fun BeautySliderRow(
    label: String,
    value: Float,
    min: Float = 0f,
    max: Float = 1f,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.width(110.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF4081),
                activeTrackColor = Color(0xFFFF4081),
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.weight(1f)
        )
        
        val intValue = if (min < 0) {
            ((value - min) / (max - min) * 200 - 100).toInt()
        } else {
            (value * 100).toInt()
        }
        Text(
            text = "$intValue",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun FilterThumb(
    name: String,
    isSelected: Boolean,
    lightTheme: Boolean = true,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFFFF4081)
    val inactiveBg = if (lightTheme) Color(0xFFF5F5F5) else Color.DarkGray
    val inactiveText = if (lightTheme) Color(0xFF757575) else Color.White.copy(alpha = 0.6f)
    val inactiveIcon = if (lightTheme) Color(0xFF424242) else Color.White.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) activeColor else inactiveBg)
                .border(
                    width = 2.dp,
                    color = if (isSelected) activeColor else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ColorLens,
                contentDescription = name,
                tint = if (isSelected) Color.White else inactiveIcon,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            color = if (isSelected) activeColor else inactiveText,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun BeautyIconItem(
    label: String,
    icon: FeatureIcon,
    isSelected: Boolean,
    isActive: Boolean, // Whether the parameter value is non-zero (customized)
    onClick: () -> Unit
) {
    val activeColor = Color(0xFFFF4081)
    val selectedBg = Color(0xFFFFF0F5)
    val inactiveBg = Color(0xFFF8F9FA)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isSelected) selectedBg else inactiveBg)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) activeColor else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                is FeatureIcon.Vector -> {
                    Icon(
                        imageVector = icon.imageVector,
                        contentDescription = label,
                        tint = if (isSelected) activeColor else Color(0xFF495057),
                        modifier = Modifier.size(24.dp)
                    )
                }
                is FeatureIcon.Emoji -> {
                    Text(
                        text = icon.char,
                        fontSize = 22.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = label,
            color = if (isSelected) activeColor else Color(0xFF495057),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Small active dot indicator (non-zero customized value)
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else Color.Transparent)
        )
    }
}

@Composable
fun PresetCapsule(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFFFF4081)
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) activeColor else Color(0xFFF1F3F5))
            .border(
                width = 1.dp,
                color = if (isSelected) activeColor else Color(0xFFE9ECEF),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else Color(0xFF495057),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
