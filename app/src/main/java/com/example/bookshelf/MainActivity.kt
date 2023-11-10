package com.example.bookshelf

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Callback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Book( //Se define una clase para representar el libro
    val title: String,
    val authors: List<String>,
    val description: String,
    val imageUrl: String
)

data class GoogleBooksResponse( //Se define datos para mapear las respuesta de la api
    val items: List<BookItem>
)

data class BookItem(
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String,
    val authors: List<String>?,
    val description: String?,
    val imageLinks: ImageLinks?
)

data class ImageLinks(
    val thumbnail: String
)
//Se define la interfaz del servicio de goolgle para los libros por medio de retrofit(Para solicitudes de api)
interface GoogleBooksService {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int
    ): Response<GoogleBooksResponse>
}

object RetrofitClient { //Se define un objeto singloton para crear instancias del servicio retrofict
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    fun createService(): GoogleBooksService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(GoogleBooksService::class.java)
    }
}
//actividad principal
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        GlobalScope.launch(Dispatchers.IO) {
            val books = getBooks()
            withContext(Dispatchers.Main) {
                val adapter = BookAdapter(books)
                recyclerView.adapter = adapter
            }
        }
    }

    private suspend fun getBooks(): List<Book> {
        val service = RetrofitClient.createService()
        val query = "android"  // Personaliza la consulta de búsqueda
        val maxResults = 10  // Personaliza el número máximo de resultados

        try { //try catch para el manejo de errores
            val response = service.searchBooks(query, maxResults)
            if (response.isSuccessful) {
                val googleBooksResponse = response.body()
                val books = googleBooksResponse?.items?.map { item ->
                    Book(
                        item.volumeInfo.title,
                        item.volumeInfo.authors ?: emptyList(),
                        item.volumeInfo.description ?: "",
                        item.volumeInfo.imageLinks?.thumbnail ?: ""
                    )
                }
                return books ?: emptyList()
            } else {
                // Maneja el error de la respuesta, por ejemplo, mostrar un mensaje de error en la IU
                return emptyList()
            }
        } catch (e: Exception) {
            // Maneja errores de red o excepciones aquí
            return emptyList()
        }
    }
}
// para el adaptador del reclerView que muestre la lista de los libro
class BookAdapter(private val books: List<Book>) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    // Variable para rastrear el estado actual (imagen visible o texto visible)
    private val stateMap = mutableMapOf<Int, Boolean>()

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImageView: ImageView = itemView.findViewById(R.id.coverImageView)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val authorsTextView: TextView = itemView.findViewById(R.id.authorsTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.book_item, parent, false)
        return BookViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        val imageUrl = if (book.imageUrl.startsWith("http://")) {
            book.imageUrl.replace("http://", "https://")
        } else {
            book.imageUrl
        }

        // Configucion de las vistas con los datos del libro y carga la imagen desde la URL
        Picasso.get().load(imageUrl).into(holder.coverImageView, object : Callback {
            override fun onSuccess() {
                Log.d("Picasso", "Image loaded successfully")
            }

            override fun onError(e: Exception?) {
                Log.e("Picasso", "Error loading image: ${e?.message}")
            }
        })

        holder.titleTextView.text = book.title
        holder.authorsTextView.text = book.authors.joinToString(", ")
        holder.descriptionTextView.text = book.description

        // Rastrea el estado de la vista actual
        val isTextVisible = stateMap.getOrDefault(position, false)

        // Muestra u oculta las vistas de título y descripción según el estado
        if (isTextVisible) {
            holder.titleTextView.visibility = View.VISIBLE
            holder.descriptionTextView.visibility = View.VISIBLE
            holder.coverImageView.visibility = View.GONE
        } else {
            holder.titleTextView.visibility = View.GONE
            holder.descriptionTextView.visibility = View.GONE
            holder.coverImageView.visibility = View.VISIBLE
        }

        // Define un OnClickListener para cambiar el estado cuando se toca un libro
        holder.itemView.setOnClickListener {
            val currentState = stateMap.getOrDefault(position, false)
            stateMap[position] = !currentState

            holder.titleTextView.visibility = if (!currentState) View.VISIBLE else View.GONE
            holder.descriptionTextView.visibility = if (!currentState) View.VISIBLE else View.GONE
            holder.coverImageView.visibility = if (currentState) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount(): Int {
        return books.size
    }
}


