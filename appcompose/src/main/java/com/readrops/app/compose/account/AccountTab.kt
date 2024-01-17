package com.readrops.app.compose.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.readrops.app.compose.account.selection.AccountSelectionScreen
import org.koin.androidx.compose.getViewModel

object AccountTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = "Account"
        )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getViewModel<AccountViewModel>()
        val closeHome by viewModel.closeHome.collectAsStateWithLifecycle()

        if (closeHome) {
            navigator.replaceAll(AccountSelectionScreen())
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(text = "Account") })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                Row {
                    Button(onClick = { viewModel.deleteAccount() }) {
                        Text(
                            text = "Delete"
                        )
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Button(onClick = { navigator.push(AccountSelectionScreen()) }) {
                        Text(
                            text = "New"
                        )
                    }
                }
            }
        }


    }

}