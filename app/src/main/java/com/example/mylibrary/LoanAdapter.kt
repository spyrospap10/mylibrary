package com.example.mylibrary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.data.Loan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoanAdapter(
    private val isAdmin: Boolean,
    private val onReturnClick: (Loan) -> Unit
) : RecyclerView.Adapter<LoanAdapter.LoanViewHolder>() {

    private var loans: List<Loan> = emptyList()

    fun updateLoans(newLoans: List<Loan>) {
        loans = newLoans
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loan, parent, false)
        return LoanViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        holder.bind(loans[position])
    }

    override fun getItemCount(): Int = loans.size

    inner class LoanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvLoanBookTitle)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvLoanUsername)
        private val tvDate: TextView = itemView.findViewById(R.id.tvLoanDate)
        private val btnReturn: Button = itemView.findViewById(R.id.btnReturnLoan)

        fun bind(loan: Loan) {
            tvTitle.text = loan.bookTitle

            if (loan.status == "RETURNED") {
                tvDate.text = "Δανεισμός: ${loan.borrowDate}\nΕπιστράφηκε: ${loan.returnDate}"
                tvDate.setTextColor(android.graphics.Color.GRAY)
                btnReturn.visibility = View.GONE
            } else {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                var daysRemainingText = ""
                try {
                    val returnDate = sdf.parse(loan.returnDate)
                    val today = java.util.Date()
                    val diff = returnDate.time - today.time
                    val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt() + 1

                    daysRemainingText = if (daysLeft > 0) "($daysLeft ημέρες απομένουν)" else "(ΛΗΞΙΠΡΟΘΕΣΜΟ!)"
                } catch (e: Exception) {
                    daysRemainingText = ""
                }

                tvDate.text = "Δανεισμός: ${loan.borrowDate}\nΕπιστροφή: ${loan.returnDate} $daysRemainingText"
                tvDate.setTextColor(android.graphics.Color.BLACK)

                if (isAdmin) {
                    btnReturn.visibility = View.VISIBLE
                } else {
                    btnReturn.visibility = View.GONE
                }
            }

            if (isAdmin) {
                tvUsername.visibility = View.VISIBLE
                tvUsername.text = "Χρήστης: ${loan.username}\nΤρόπος: ${if(loan.deliveryMethod == "Pickup") "Παραλαβή από ${loan.pickupBranch}" else "Αποστολή: ${loan.address}"}"
            } else {
                tvUsername.visibility = View.GONE
            }

            btnReturn.setOnClickListener { onReturnClick(loan) }
        }
    }
}