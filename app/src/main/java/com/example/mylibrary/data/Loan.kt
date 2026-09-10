package com.example.mylibrary.data

data class Loan(
    var loanId: String = "",
    val username: String = "",
    val bookTitle: String = "",
    val branchId: Int = 0,
    val borrowDate: String = "",
    val returnDate: String = "",
    val deliveryMethod: String = "",
    val pickupBranch: String = "",
    val address: String = "",
    val city: String = "",
    val zip: String = "",
    val status: String = ""
)