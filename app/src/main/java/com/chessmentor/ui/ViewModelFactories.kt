package com.chessmentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Small factory helper so ViewModels with constructor arguments
 * (engine, repository) can be created with viewModel(factory = ...).
 * Keeps MainActivity free of a factory class per ViewModel.
 */
@Suppress("UNCHECKED_CAST")
fun <T : ViewModel> viewModelFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
    }
