package presenter

interface SignUpContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun onSignUpSuccess(message: String)
        fun onSignUpError(error: String)
    }

    interface Presenter {
        fun signUp(
            fullName: String,
            email: String,
            password: String,
            confirmPassword: String
        )
    }
}
