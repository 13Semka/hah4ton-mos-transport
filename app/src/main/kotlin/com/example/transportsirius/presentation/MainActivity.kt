package com.example.transportsirius.presentation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.transportsirius.R
import com.example.transportsirius.domain.entity.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: RouteViewModel by viewModels()
    
    private lateinit var searchButton: Button
    private lateinit var resultText: TextView
    private lateinit var routeCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            // Инициализация UI элементов
            searchButton = findViewById(R.id.searchButton)
            resultText = findViewById(R.id.resultText)
            routeCard = findViewById(R.id.routeCard)
            
            // Изначально результаты скрыты
            resultText.visibility = View.GONE
            routeCard.visibility = View.GONE
            
            // Настройка обработчика нажатий
            searchButton.setOnClickListener {
                // Показываем Toast сообщение
                Toast.makeText(this, "Поиск маршрутов...", Toast.LENGTH_SHORT).show()
                
                // Загружаем маршруты
                searchRoutes()
            }
        } catch (e: Exception) {
            // Логируем ошибку в случае проблем
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun searchRoutes() {
        try {
            // Показываем индикатор загрузки
            searchButton.isEnabled = false
            searchButton.text = "Загрузка..."
            
            // Координаты Москвы и Санкт-Петербурга
            val from = LatLng(55.751244, 37.618423)
            val to = LatLng(59.938784, 30.314997)

            // Используем ViewModel для загрузки маршрутов
            lifecycleScope.launch {
                try {
                    viewModel.loadRoutes(from, to).observe(this@MainActivity) { routes ->
                        // Возвращаем кнопку в нормальное состояние
                        searchButton.isEnabled = true
                        searchButton.text = "Найти маршруты"
                        
                        // Показываем результаты
                        resultText.visibility = View.VISIBLE
                        routeCard.visibility = View.VISIBLE
                        
                        // Устанавливаем текст результата
                        resultText.text = "Найдено маршрутов: ${routes.size}"
                    }
                } catch (e: Exception) {
                    // Логируем ошибку при загрузке маршрутов
                    Toast.makeText(this@MainActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
                    searchButton.isEnabled = true
                    searchButton.text = "Найти маршруты"
                }
            }
        } catch (e: Exception) {
            // Логируем ошибку при запросе
            Toast.makeText(this, "Ошибка запроса: ${e.message}", Toast.LENGTH_LONG).show()
            searchButton.isEnabled = true
            searchButton.text = "Найти маршруты"
        }
    }
}