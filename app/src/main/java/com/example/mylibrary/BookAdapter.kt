package com.example.mylibrary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.data.Book

class BookAdapter(
    private val isAdmin: Boolean,
    private val onBorrowClick: (Book) -> Unit,
    private val onEditClick: (Book) -> Unit,
    private val onDeleteClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private var books: List<Book> = emptyList()
    private var authorNamesMap: Map<Int, String> = emptyMap()

    fun updateBooks(newBooks: List<Book>, newAuthorNamesMap: Map<Int, String>) {
        books = newBooks
        authorNamesMap = newAuthorNamesMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = books.size

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvBookTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvBookAuthor)
        private val tvGenre: TextView = itemView.findViewById(R.id.tvBookGenre)
        private val tvStock: TextView = itemView.findViewById(R.id.tvBookStock)
        private val btnBorrow: Button = itemView.findViewById(R.id.btnBorrow)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditBook)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteBook)

        fun bind(book: Book) {
            tvTitle.text = book.title
            tvGenre.text = book.genre
            tvStock.text = "Απόθεμα: ${book.stock}"

            val authorName = authorNamesMap[book.authorId] ?: "Άγνωστος"
            tvAuthor.text = authorName

            if (isAdmin) {
                btnBorrow.visibility = View.GONE
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
            } else {
                btnBorrow.visibility = View.VISIBLE
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
            }

            btnBorrow.setOnClickListener { onBorrowClick(book) }
            btnEdit.setOnClickListener { onEditClick(book) }
            btnDelete.setOnClickListener { onDeleteClick(book) }
        }
    }
}