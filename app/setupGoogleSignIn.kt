private fun setupGoogleSignIn() {
        // R.string.default_web_client_id는 Firebase 콘솔 설정 시 자동으로 생성됩니다.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // R.string 경로는 실제 프로젝트에 맞게 수정해야 할 수 있습니다.
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }