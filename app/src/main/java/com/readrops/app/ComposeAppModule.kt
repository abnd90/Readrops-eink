package com.readrops.app

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.readrops.api.services.Credentials
import com.readrops.app.account.AccountScreenModel
import com.readrops.app.account.credentials.AccountCredentialsScreenMode
import com.readrops.app.account.credentials.AccountCredentialsScreenModel
import com.readrops.app.account.selection.AccountSelectionScreenModel
import com.readrops.app.feeds.FeedScreenModel
import com.readrops.app.item.ItemScreenModel
import com.readrops.app.notifications.NotificationsScreenModel
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.repositories.FreshRSSRepository
import com.readrops.app.repositories.GetFoldersWithFeeds
import com.readrops.app.repositories.LocalRSSRepository
import com.readrops.app.repositories.NextcloudNewsRepository
import com.readrops.app.timelime.TimelineScreenModel
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val composeAppModule = module {

    factory { TimelineScreenModel(get(), get()) }

    factory { FeedScreenModel(get(), get(), get(), androidContext()) }

    factory { AccountSelectionScreenModel(get()) }

    factory { AccountScreenModel(get()) }

    factory { (itemId: Int) -> ItemScreenModel(get(), itemId) }

    factory { (accountType: Account, mode: AccountCredentialsScreenMode) ->
        AccountCredentialsScreenModel(accountType, mode, get())
    }

    factory { (account: Account) -> NotificationsScreenModel(account, get()) }

    single { GetFoldersWithFeeds(get()) }

    // repositories

    factory<BaseRepository> { (account: Account) ->
        when (account.accountType) {
            AccountType.LOCAL -> LocalRSSRepository(get(), get(), account)
            AccountType.FRESHRSS -> FreshRSSRepository(
                get(), account,
                get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )
            AccountType.NEXTCLOUD_NEWS -> NextcloudNewsRepository(
                get(), account,
                get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )
            else -> throw IllegalArgumentException("Unknown account type")
        }
    }

    single {
        val masterKey = MasterKey.Builder(androidContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            androidContext(),
            "account_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}