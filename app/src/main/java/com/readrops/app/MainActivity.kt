package com.readrops.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.ScreenTransition
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import com.readrops.app.account.selection.AccountSelectionScreen
import com.readrops.app.account.selection.AccountSelectionScreenModel
import com.readrops.app.home.HomeScreen
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.sync.SyncWorker
import com.readrops.app.timelime.TimelineTab
import com.readrops.app.util.Migrations
import com.readrops.app.util.Preferences
import com.readrops.app.util.theme.ReadropsTheme
import com.readrops.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity(), KoinComponent {

    @OptIn(KoinExperimentalAPI::class, ExperimentalVoyagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        runBlocking {
            Migrations.upgrade(
                appPreferences = get(),
                encryptedPreferences = get(),
                oldPreferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity),
                database = get(),
            )
        }

        val screenModel = get<AccountSelectionScreenModel>()
        val accountExists = screenModel.accountExists()

        val preferences = get<Preferences>()

        val darkFlag = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val initialUseDarkTheme = runBlocking {
            useDarkTheme(preferences.theme.flow.first(), darkFlag)
        }

        setContent {
            KoinAndroidContext {
                val useDarkTheme by preferences.theme.flow
                    .map { mode -> useDarkTheme(mode, darkFlag) }
                    .collectAsState(initial = initialUseDarkTheme)

                ReadropsTheme(
                    useDarkTheme = useDarkTheme
                ) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                    )

                    Navigator(
                        screen = if (accountExists) HomeScreen else AccountSelectionScreen(),
                        disposeBehavior = NavigatorDisposeBehavior(
                            // prevent screenModels being recreated when opening a screen from a tab
                            disposeNestedNavigators = false,
                            disposeSteps = false
                        )
                    ) { navigator ->
                        LaunchedEffect(Unit) {
                            handleIntent(intent)
                        }

                        NoTransition (
                            navigator = navigator
                        ) { screen ->
                            screen.Content()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        lifecycleScope.launch(Dispatchers.IO) {
            handleIntent(intent)
        }
    }

    private suspend fun handleIntent(intent: Intent) {
        when {
            intent.hasExtra(SyncWorker.ACCOUNT_ID_KEY) -> {
                val accountId = intent.getIntExtra(SyncWorker.ACCOUNT_ID_KEY, -1)
                val database = get<Database>().also {
                    it.accountDao()
                        .updateCurrentAccount(accountId)
                }

                HomeScreen.openTab(TimelineTab)

                if (intent.hasExtra(SyncWorker.ITEM_ID_KEY)) {
                    val itemId = intent.getIntExtra(SyncWorker.ITEM_ID_KEY, -1)
                    val account = database.accountDao().select(accountId)
                    val item = database.itemDao().select(itemId)
                        .apply { isRead = true }

                    get<BaseRepository>(parameters = { parametersOf(account) })
                        .setItemReadState(item)
                    HomeScreen.openItemScreen(itemId)
                }
            }

            intent.action != null && intent.action == Intent.ACTION_SEND -> {
                HomeScreen.openAddFeedDialog(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
            }
        }
    }

    private fun useDarkTheme(mode: String, darkFlag: Int): Boolean {
        return when (mode) {
            "light" -> false
            "dark" -> true
            else -> darkFlag == Configuration.UI_MODE_NIGHT_YES
        }
    }
}

@OptIn(ExperimentalVoyagerApi::class)
@Composable
fun NoTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: ScreenTransitionContent
) {
    ScreenTransition(
        navigator = navigator,
        modifier = modifier,
        content = content,
        disposeScreenAfterTransitionEnd = true,
        transition = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None
            )
        }
    )
}
