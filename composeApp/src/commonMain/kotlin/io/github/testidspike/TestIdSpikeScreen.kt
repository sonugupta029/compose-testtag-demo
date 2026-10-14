package io.github.testidspike

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Spike screen to validate Appium testTag/accessibilityId behavior.
 *
 * Convention: FeatureName-ScreenName-ElementType-uniqueName
 *
 * This screen contains all common element types that we'll need to
 * locate via Appium on both Android and iOS using the same ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestIdSpikeScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var switchToggle by remember { mutableStateOf(false) }
    var loginClicked by remember { mutableStateOf(false) }

    // testTagsAsResourceId is applied at the root App.kt level via platformTestTagModifier()
    // On iOS, testTag maps to accessibilityIdentifier by default — no extra config needed
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TestTag Demo",
                        modifier = Modifier.testTag("Auth-Login-Text-screenTitle")
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .testTag("Auth-Login-ScrollView-mainContent"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== SECTION 1: Text Fields =====
            item {
                Text(
                    text = "Login Form",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("Auth-Login-Text-sectionHeader")
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("Auth-Login-TextField-email"),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("Auth-Login-TextField-password"),
                    singleLine = true
                )
            }

            // ===== SECTION 2: Toggles =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("Auth-Login-Row-rememberMe"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        modifier = Modifier.testTag("Auth-Login-Checkbox-rememberMe")
                    )
                    Text(
                        text = "Remember Me",
                        modifier = Modifier.testTag("Auth-Login-Text-rememberMeLabel")
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("Auth-Login-Row-biometric"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable Biometric",
                        modifier = Modifier.testTag("Auth-Login-Text-biometricLabel")
                    )
                    Switch(
                        checked = switchToggle,
                        onCheckedChange = { switchToggle = it },
                        modifier = Modifier.testTag("Auth-Login-Switch-biometric")
                    )
                }
            }

            // ===== SECTION 3: Buttons =====
            item {
                Button(
                    onClick = { loginClicked = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("Auth-Login-Button-signIn")
                ) {
                    Text(
                        text = "Sign In",
                        modifier = Modifier.testTag("Auth-Login-Text-signInButtonLabel")
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = { /* SSO */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("Auth-Login-Button-ssoLogin")
                ) {
                    Text(
                        text = "Sign In with SSO",
                        modifier = Modifier.testTag("Auth-Login-Text-ssoButtonLabel")
                    )
                }
            }

            item {
                TextButton(
                    onClick = { /* Forgot password */ },
                    modifier = Modifier.testTag("Auth-Login-Button-forgotPassword")
                ) {
                    Text("Forgot Password?")
                }
            }

            // ===== SECTION 4: Status / Result (conditional) =====
            if (loginClicked) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("Auth-Login-Card-loginStatus")
                    ) {
                        Text(
                            text = "Login button was tapped! Email: $email",
                            modifier = Modifier
                                .padding(16.dp)
                                .testTag("Auth-Login-Text-loginStatusMessage"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ===== SECTION 5: List items (simulating Router List) =====
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Router List (Static IDs)",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("RouterList-Section-Text-header")
                )
            }

            val routers = listOf(
                "E3000" to "Online",
                "R920" to "Offline",
                "S700" to "Online"
            )

            items(routers) { (name, status) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("RouterList-Item-Card-$name")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.testTag("RouterList-Item-Text-name-$name")
                        )
                        Text(
                            text = status,
                            modifier = Modifier.testTag("RouterList-Item-Text-status-$name"),
                            color = if (status == "Online")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ===== SECTION 6: Dropdown / Environment Selector =====
            item {
                Spacer(modifier = Modifier.height(16.dp))
                var expanded by remember { mutableStateOf(false) }
                var selectedEnv by remember { mutableStateOf("Production") }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.testTag("Settings-Env-Dropdown-selector")
                ) {
                    OutlinedTextField(
                        value = selectedEnv,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Environment") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("Settings-Env-TextField-selected")
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Production", "QA4").forEach { env ->
                            DropdownMenuItem(
                                text = { Text(env) },
                                onClick = {
                                    selectedEnv = env
                                    expanded = false
                                },
                                modifier = Modifier.testTag("Settings-Env-MenuItem-$env")
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
