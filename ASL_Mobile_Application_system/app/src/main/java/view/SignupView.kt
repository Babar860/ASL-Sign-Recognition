package com.example.signassistap.view

interface SignupView {
    fun showLoading()
    fun hideLoading()
    fun showError(message: String)
    fun signupSuccess(message: String)
}










