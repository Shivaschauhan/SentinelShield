package com.sentinelshield.antitheft.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.sentinelshield.antitheft.utils.DebugLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentLatLng by remember { mutableStateOf(LatLng(28.6139, 77.2090)) } // Default New Delhi fallback
    var locationText by remember { mutableStateOf("Fetching live GPS coordinates...") }
    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    val fetchLocation = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isPermissionGranted = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val newPos = LatLng(loc.latitude, loc.longitude)
                    currentLatLng = newPos
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(newPos, 16f)
                    locationText = "Lat: ${String.format("%.5f", loc.latitude)}, Lng: ${String.format("%.5f", loc.longitude)}"
                    DebugLogger.log(context, "LiveMapScreen", "Live location fetched: $newPos", force = true)
                } else {
                    locationText = "Location signal unavailable. Turn on GPS."
                }
            }
        } else {
            isPermissionGranted = false
            locationText = "Location Permission Required."
        }
    }

    LaunchedEffect(Unit) {
        fetchLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Tracking & Maps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fetchLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
                    }
                    IconButton(onClick = {
                        val mapsUrl = "https://maps.google.com/?q=${currentLatLng.latitude},${currentLatLng.longitude}"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Device Live GPS Location")
                            putExtra(Intent.EXTRA_TEXT, "SentinelShield Live Device Location:\n$mapsUrl")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Location Link"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Location")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = isPermissionGranted),
                uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(currentLatLng),
                    title = "Current Device Spot",
                    snippet = locationText
                )
            }

            // Floating Location Info Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Live GPS Coordinates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:${currentLatLng.latitude},${currentLatLng.longitude}?q=${currentLatLng.latitude},${currentLatLng.longitude}(Stolen Phone)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${currentLatLng.latitude},${currentLatLng.longitude}"))
                                context.startActivity(webIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open in Google Maps App")
                    }
                }
            }
        }
    }
}

@Composable
fun rememberUpdatedMarkerState(newPosition: LatLng): MarkerState {
    return remember(newPosition) {
        MarkerState(position = newPosition)
    }.apply {
        position = newPosition
    }
}
