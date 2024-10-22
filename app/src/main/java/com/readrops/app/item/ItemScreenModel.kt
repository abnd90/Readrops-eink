package com.readrops.app.item

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.Stable
import androidx.core.content.FileProvider
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.util.FontPreference
import com.readrops.app.util.Preferences
import com.readrops.db.Database
import com.readrops.db.entities.Item
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.queries.ItemSelectionQueryBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.dankito.readability4j.Readability4J
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ItemScreenModel(
    private val database: Database,
    private val itemId: Int,
    private val preferences: Preferences,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : StateScreenModel<ItemState>(ItemState()), KoinComponent {

    //TODO Is this really the best solution?
    lateinit var account: Account
    lateinit var repository: BaseRepository

    init {
        screenModelScope.launch(dispatcher) {
            database.accountDao().selectCurrentAccount()
                .flatMapLatest { account ->
                    this@ItemScreenModel.account = account!!

                    if (account.accountType == AccountType.FEVER) {
                        get<SharedPreferences>().apply {
                            account.login = getString(account.loginKey, null)
                            account.password = getString(account.passwordKey, null)
                        }
                    }

                    repository = get { parametersOf(account) }

                    val query = ItemSelectionQueryBuilder.buildQuery(
                        itemId = itemId,
                        separateState = account.config.useSeparateState
                    )

                    database.itemDao().selectItemById(query)
                }
                .collect { itemWithFeed ->
                    mutableState.update {
                        it.copy(
                            itemWithFeed = itemWithFeed,
                            bottomBarState = BottomBarState(
                                isRead = itemWithFeed.item.isRead,
                                isStarred = itemWithFeed.item.isStarred
                            )
                        )
                    }
                }
        }

        screenModelScope.launch(dispatcher) {
            combine(
                preferences.openLinksWith.flow,
                preferences.theme.flow
            ) { openLinksWith, theme ->
                openLinksWith to theme
            }.collect { (openLinksWith, theme) ->
                mutableState.update {
                    it.copy(
                        openInExternalBrowser = when (openLinksWith) {
                            "external_navigator" -> true
                            else -> false
                        },
                        theme = theme
                    )
                }
            }
        }

        screenModelScope.launch(dispatcher) {
            getFormattingPreferences()
                .collect { preferences ->
                    mutableState.update {
                        it.copy(
                            formatSettings = preferences
                        )
                    }
                }
        }
    }

    private fun getFormattingPreferences(): Flow<ItemFormatSettings> {
        return combine(
            preferences.itemFont.flow,
            preferences.itemJustifyText.flow,
            preferences.itemTextSizeMultiplier.flow,
            preferences.itemLineSizeMultiplier.flow,
            transform = { it ->
                ItemFormatSettings(
                    font = FontPreference.fromInt(it[0] as Int),
                    justifyText = it[1] as Boolean,
                    textSizeMultiplier = it[2] as Float,
                    lineSizeMultiplier = it[3] as Float
                )
            }
        )
    }

    fun shareItem(item: Item, context: Context) {
        Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, item.link)
        }.also {
            context.startActivity(Intent.createChooser(it, null))
        }
    }

    fun setItemReadState(item: Item) {
        screenModelScope.launch(dispatcher) {
            repository.setItemReadState(item)
        }
    }

    fun setItemStarState(item: Item) {
        screenModelScope.launch(dispatcher) {
            repository.setItemStarState(item)
        }
    }

    fun openImageDialog(url: String) = mutableState.update { it.copy(imageDialogUrl = url) }

    fun closeImageDialog() = mutableState.update { it.copy(imageDialogUrl = null) }

    fun downloadImage(url: String, context: Context) {
        screenModelScope.launch(dispatcher) {
            val bitmap = getImage(url, context)

            val target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                url.substringAfterLast('/')
            )
            FileOutputStream(target).apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, this)
                flush()
                close()
            }

            mutableState.update { it.copy(fileDownloadedEvent = true) }
        }
    }

    fun shareImage(url: String, context: Context) {
        screenModelScope.launch(dispatcher) {
            val bitmap = getImage(url, context)
            val uri = saveImageInCache(bitmap, url, context)

            Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }.also {
                context.startActivity(Intent.createChooser(it, null))
            }
        }
    }

    private suspend fun getImage(url: String, context: Context): Bitmap {
        val downloader = context.imageLoader

        return (downloader.execute(
            ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
        ).drawable as BitmapDrawable).bitmap
    }

    private fun saveImageInCache(bitmap: Bitmap, url: String, context: Context): Uri {
        val imagesFolder = File(context.cacheDir.absolutePath, "images")
        if (!imagesFolder.exists()) imagesFolder.mkdirs()

        val image = File(imagesFolder, url.substringAfterLast('/'))
        FileOutputStream(image).apply {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, this)
            flush()
            close()
        }

        return FileProvider.getUriForFile(context, context.packageName, image)
    }

    fun readableArticleText(
        itemWithFeed: ItemWithFeed,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit
    ) {
        screenModelScope.launch(dispatcher) {
            val url = itemWithFeed.item.link!!

            val client: OkHttpClient = get()
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val html = response.body!!.string()
                    val readability = Readability4J(url, html)
                    val article = readability.parse()

                    onSuccess(article.content!!)
                }
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }

    fun setItemJustifyText(value: Boolean) {
        screenModelScope.launch {
            preferences.itemJustifyText.write(value)
            mutableState.update {
                it.copy(formatSettings = it.formatSettings.copy(justifyText = value))
            }
        }
    }
    fun setItemTextSizeMultiplier(value: Float) {
        screenModelScope.launch {
            preferences.itemTextSizeMultiplier.write(value)
            mutableState.update {
                it.copy(formatSettings = it.formatSettings.copy(textSizeMultiplier = value))
            }
        }
    }
    fun setItemLineSizeMultiplier(value: Float) {
        screenModelScope.launch {
            preferences.itemLineSizeMultiplier.write(value)
            mutableState.update {
                it.copy(formatSettings = it.formatSettings.copy(lineSizeMultiplier = value))
            }
        }
    }

    fun setItemFont(font: FontPreference) {
        screenModelScope.launch {
            preferences.itemFont.write(font.value)
            mutableState.update {
                it.copy(formatSettings = it.formatSettings.copy(font = font))
            }
        }

    }
}

@Stable
data class ItemFormatSettings(
    val justifyText: Boolean = false,
    val textSizeMultiplier: Float = 1.0f,
    val lineSizeMultiplier: Float = 1.0f,
    val font: FontPreference = FontPreference.SANS_SERIF
)

@Stable
data class ItemState(
    val itemWithFeed: ItemWithFeed? = null,
    val bottomBarState: BottomBarState = BottomBarState(),
    val imageDialogUrl: String? = null,
    val fileDownloadedEvent: Boolean = false,
    val openInExternalBrowser: Boolean = false,
    val formatSettings: ItemFormatSettings = ItemFormatSettings(),
    val theme: String? = ""
)