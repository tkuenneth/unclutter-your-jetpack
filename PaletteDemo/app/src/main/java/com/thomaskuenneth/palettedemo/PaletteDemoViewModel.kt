package com.thomaskuenneth.palettedemo

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PaletteDemoViewModel : ViewModel() {

    private val _bitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    fun setBitmap(bitmap: Bitmap) {
        _bitmap.update { bitmap }
    }

    private val _palette: MutableStateFlow<Palette?> = MutableStateFlow(null)
    val palette: StateFlow<Palette?> = _palette.asStateFlow()

    fun setPalette(palette: Palette) {
        _palette.update { palette }
    }
}
