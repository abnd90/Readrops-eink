package com.readrops.app.account.credentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.account.selection.adaptiveIconPainterResource
import com.readrops.app.home.HomeScreen
import com.readrops.app.util.ErrorMessage
import com.readrops.app.util.components.AndroidScreen
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.spacing
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import org.koin.core.parameter.parametersOf

enum class AccountCredentialsScreenMode {
    NEW_CREDENTIALS,
    EDIT_CREDENTIALS
}

class AccountCredentialsScreen(
    private val account: Account,
    private val mode: AccountCredentialsScreenMode
) : AndroidScreen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel =
            getScreenModel<AccountCredentialsScreenModel>(parameters = { parametersOf(account, mode) })

        val state by screenModel.state.collectAsStateWithLifecycle()

        if (state.exitScreen) {
            if (mode == AccountCredentialsScreenMode.NEW_CREDENTIALS) {
                navigator.replaceAll(HomeScreen)
            } else {
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (mode == AccountCredentialsScreenMode.EDIT_CREDENTIALS)
                                stringResource(id = R.string.credentials)
                            else
                                stringResource(id = R.string.new_account)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .imePadding()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.mediumSpacing)
                        .verticalScroll(rememberScrollState())
                ) {
                    Image(
                        painter = adaptiveIconPainterResource(id = account.accountType!!.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )

                    ShortSpacer()

                    Text(
                        text = stringResource(id = account.accountType!!.typeName),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    MediumSpacer()

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { screenModel.onEvent(Event.NameEvent(it)) },
                        label = { Text(text = stringResource(id = R.string.account_name)) },
                        singleLine = true,
                        isError = state.isNameError,
                        supportingText = { Text(text = state.nameError?.errorText().orEmpty()) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ShortSpacer()

                    OutlinedTextField(
                        value = state.url,
                        onValueChange = { screenModel.onEvent(Event.URLEvent(it)) },
                        label = { Text(text = stringResource(id = R.string.account_url)) },
                        singleLine = true,
                        isError = state.isUrlError,
                        supportingText = {
                            when {
                                state.urlError != null -> {
                                    Text(text = state.urlError!!.errorText())
                                }
                                account.accountType == AccountType.FEVER -> {
                                    Text(text = stringResource(R.string.provide_full_url))
                                }
                                else -> {
                                    Text(text = stringResource(R.string.provide_root_url))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ShortSpacer()

                    OutlinedTextField(
                        value = state.login,
                        onValueChange = { screenModel.onEvent(Event.LoginEvent(it)) },
                        label = { Text(text = stringResource(id = R.string.login)) },
                        singleLine = true,
                        isError = state.isLoginError,
                        supportingText = { Text(text = state.loginError?.errorText().orEmpty()) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ShortSpacer()

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { screenModel.onEvent(Event.PasswordEvent(it)) },
                        label = { Text(text = stringResource(id = R.string.password)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { screenModel.setPasswordVisibility(!state.isPasswordVisible) }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (state.isPasswordVisible) {
                                            R.drawable.ic_visible_off
                                        } else R.drawable.ic_visible
                                    ),
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (state.isPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        isError = state.isPasswordError,
                        supportingText = {
                            when {
                                state.passwordError != null -> {
                                    Text(text = state.passwordError!!.errorText())
                                }
                                account.accountType == AccountType.FRESHRSS -> {
                                    Text(text = stringResource(id = R.string.password_helper))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ShortSpacer()

                    Button(
                        onClick = { screenModel.login() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isLoginOnGoing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(text = stringResource(id = R.string.validate))
                        }
                    }

                    if (state.loginException != null) {
                        ShortSpacer()

                        Text(
                            text = ErrorMessage.get(state.loginException!!, LocalContext.current),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
