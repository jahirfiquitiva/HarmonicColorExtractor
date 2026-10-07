package dev.jahir.harmonic.colors.demo

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dev.jahir.harmonic.colors.HarmonicColorExtractor
import dev.jahir.harmonic.colors.HarmonicColors
import dev.jahir.harmonic.colors.demo.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private sealed interface UiState {
        data object Empty : UiState
        data object Loading : UiState
        data object Error : UiState
        data class Loaded(val bitmap: Bitmap, val colors: HarmonicColors) : UiState
    }

    private lateinit var binding: ActivityMainBinding
    private var imageUri: Uri? = null
    private var bitmap: Bitmap? = null
    private var job: Job? = null

    private val pickImage = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) load(uri)
    }

    private val selectedRegion: Region
        get() = Region.fromButtonId(binding.regions.checkedRadioButtonId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val padding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
            insets
        }

        binding.pickImage.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
        binding.regions.setOnCheckedChangeListener { _, _ -> bitmap?.let(::extract) }

        val savedUri = savedInstanceState?.let { BundleCompat.getParcelable(it, KEY_IMAGE_URI, Uri::class.java) }
        if (savedUri != null) load(savedUri) else render(UiState.Empty)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_IMAGE_URI, imageUri)
    }

    private fun load(uri: Uri) {
        imageUri = uri
        bitmap = null
        job?.cancel()
        render(UiState.Loading)
        job = lifecycleScope.launch {
            val loaded = loadBitmap(contentResolver, uri, MAX_IMAGE_SIZE)
            bitmap = loaded
            if (loaded == null) render(UiState.Error) else render(UiState.Loaded(loaded, colorsOf(loaded)))
        }
    }

    private fun extract(bitmap: Bitmap) {
        job?.cancel()
        job = lifecycleScope.launch { render(UiState.Loaded(bitmap, colorsOf(bitmap))) }
    }

    private suspend fun colorsOf(bitmap: Bitmap): HarmonicColors {
        val region = selectedRegion
        return withContext(Dispatchers.Default) {
            region.applyTo(HarmonicColorExtractor().setBitmap(bitmap)).getColors()
        }
    }

    private fun render(state: UiState) {
        binding.loading.isVisible = state is UiState.Loading
        binding.message.isVisible = state is UiState.Empty || state is UiState.Error
        binding.result.isVisible = state is UiState.Loaded
        when (state) {
            UiState.Empty -> binding.message.setText(R.string.empty_message)
            UiState.Error -> binding.message.setText(R.string.error_message)
            UiState.Loading -> Unit
            is UiState.Loaded -> showColors(state.bitmap, state.colors)
        }
    }

    private fun showColors(bitmap: Bitmap, colors: HarmonicColors) = with(binding) {
        result.setBackgroundColor(colors.backgroundColor)
        image.setImageBitmap(bitmap)
        primaryText.setTextColor(colors.firstForegroundColor)
        primaryText.text = getString(R.string.primary_text, colors.firstForegroundColor.toHex())
        secondaryText.setTextColor(colors.secondForegroundColor)
        secondaryText.text = getString(R.string.secondary_text, colors.secondForegroundColor.toHex())
        backgroundText.setTextColor(colors.secondForegroundColor)
        backgroundText.text = getString(R.string.background_text, colors.backgroundColor.toHex())
    }

    private fun @receiver:ColorInt Int.toHex(): String = "#%06X".format(this and 0xFFFFFF)

    private companion object {
        const val KEY_IMAGE_URI = "image_uri"

        /** Large enough to look sharp on screen, small enough to decode quickly */
        const val MAX_IMAGE_SIZE = 1080
    }
}
