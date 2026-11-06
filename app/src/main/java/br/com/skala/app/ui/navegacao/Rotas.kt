package br.com.skala.app.ui.navegacao

import java.time.LocalDate

object Rotas {
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"

    // Cliente
    const val INICIO = "inicio"
    const val SERVICOS = "servicos"
    const val CABELO = "cabelo"
    const val HORARIO = "horario"
    const val CONFIRMAR = "confirmar"
    const val AGENDAMENTOS = "agendamentos"
    const val PERFIL = "perfil"

    // Dona do salão
    const val DONA_AGENDA = "dona/agenda"
    const val DONA_HORARIOS = "dona/horarios"
    const val DONA_DIA = "dona/dia/{data}"
    const val DONA_PERFIL = "dona/perfil"

    fun donaDia(data: LocalDate) = "dona/dia/$data"
}
