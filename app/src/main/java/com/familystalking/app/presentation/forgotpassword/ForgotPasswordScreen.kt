package com.familystalking.app.presentation.forgotpassword

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.familystalking.app.R
import com.familystalking.app.domain.model.AuthError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val email by viewModel.email.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val resetSent by viewModel.resetSent.collectAsState()

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
            text = "Reset Password",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Enter your email to receive reset instructions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        error?.let { authError: AuthError ->
            Text(
                text = authError.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (resetSent) {
            Text(
                text = "Reset instructions sent to your email",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Email") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            isError = error is AuthError.InvalidEmail
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.onResetPassword() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send Reset Link")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Back to Sign In",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
} 