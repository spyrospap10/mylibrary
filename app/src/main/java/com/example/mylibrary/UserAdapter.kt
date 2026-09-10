package com.example.mylibrary

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.data.User

class UserAdapter(private val onUserClick: (User) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users: List<User> = emptyList()

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFullName: TextView = itemView.findViewById(R.id.tvUserFullName)
        private val tvRole: TextView = itemView.findViewById(R.id.tvUserRole)

        fun bind(user: User) {
            tvFullName.text = "${user.lastName} ${user.firstName}"

            if (user.role == "ADMIN") {
                tvRole.text = "Ρόλος: Διαχειριστής"
                tvRole.setTextColor(Color.RED)
            } else {
                tvRole.text = "Ρόλος: Χρήστης"
                tvRole.setTextColor(Color.parseColor("#388E3C"))
            }
            itemView.setOnClickListener {
                onUserClick(user)
            }
        }
    }
}