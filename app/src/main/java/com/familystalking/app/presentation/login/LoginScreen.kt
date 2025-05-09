package com.familystalking.app.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.familystalking.app.R
import com.familystalking.app.domain.model.AuthError
import com.familystalking.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val navigateTo by viewModel.navigateTo.collectAsState()

    LaunchedEffect(navigateTo) {
        navigateTo?.let { route ->
            navController.navigate(route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
            viewModel.onNavigated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        Surface(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_filled),
                contentDescription = "App Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Sign in to continue to family stalking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        error?.let { authError ->
            Text(
                text = authError.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = error is AuthError.InvalidEmail || error is AuthError.EmailAlreadyInUse
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            isError = error is AuthError.InvalidCredentials || error is AuthError.WeakPassword
        )

        TextButton(
            onClick = { navController.navigate(Screen.ForgotPassword.route) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = "Forgot password?",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::onSignIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign in")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { navController.navigate(Screen.Signup.route) }) {
                Text(
                    text = "Sign up",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
} 