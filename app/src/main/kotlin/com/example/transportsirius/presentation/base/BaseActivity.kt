package com.example.transportsirius.presentation.base

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

abstract class BaseActivity : AppCompatActivity() {
    
    /**
     * Показать сообщение об ошибке пользователю
     */
    protected fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Установить наблюдателя на LiveData ошибок
     */
    protected fun <T> LiveData<T>.observeNonNull(observer: (T) -> Unit) {
        this.observe(this@BaseActivity) { value ->
            value?.let(observer)
        }
    }
} 