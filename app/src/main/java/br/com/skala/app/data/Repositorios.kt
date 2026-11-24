package br.com.skala.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

/** Escolhe onde os dados ficam: no Firebase (com google-services.json) ou só no celular. */
object Repositorios {
    lateinit var auth: AuthRepository
        private set
    lateinit var salao: SalaoRepository
        private set

    /** true quando o app está ligado ao Firebase; false com os dados de teste no celular. */
    var usandoFirebase = false
        private set

    fun iniciar(context: Context) {
        if (::auth.isInitialized) return
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            auth = FirebaseAuthRepository()
            salao = FirebaseSalaoRepository()
            usandoFirebase = true
        } else {
            Log.w("Repositorios", "Sem google-services.json: usando os dados de teste guardados no celular")
            FakeAuthRepository.iniciar(context)
            FakeSalaoRepository.iniciar(context)
            auth = FakeAuthRepository
            salao = FakeSalaoRepository
        }
    }
}
