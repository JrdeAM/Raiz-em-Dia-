package br.com.skala.app

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.skala.app.data.DiaFuncionamento
import br.com.skala.app.data.StatusDia
import br.com.skala.app.data.duracaoTotalMin
import br.com.skala.app.data.temServicoDeCabelo
import br.com.skala.app.data.textoDuracao
import br.com.skala.app.data.titulo
import br.com.skala.app.ui.agendamento.AgendamentoViewModel
import br.com.skala.app.ui.auth.AuthViewModel
import br.com.skala.app.ui.cadastro.CadastroScreen
import br.com.skala.app.ui.cliente.AgendamentosScreen
import br.com.skala.app.ui.cliente.CabeloScreen
import br.com.skala.app.ui.cliente.ConfirmarScreen
import br.com.skala.app.ui.cliente.HorarioScreen
import br.com.skala.app.ui.cliente.InicioScreen
import br.com.skala.app.ui.cliente.PerfilScreen
import br.com.skala.app.ui.cliente.ServicosScreen
import br.com.skala.app.ui.dona.AgendaDiaScreen
import br.com.skala.app.ui.dona.EditarDiaScreen
import br.com.skala.app.ui.dona.HorariosScreen
import br.com.skala.app.ui.login.LoginScreen
import br.com.skala.app.ui.navegacao.BarraInferior
import br.com.skala.app.ui.navegacao.Rotas
import br.com.skala.app.ui.navegacao.itensCliente
import br.com.skala.app.ui.navegacao.itensDona
import br.com.skala.app.ui.theme.SkalaTheme
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O app é sempre claro, então os ícones da barra de status ficam escuros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            SkalaTheme {
                SkalaApp()
            }
        }
    }
}

private const val DURACAO_MS = 300

// Telas do fluxo deslizam para os lados; trocar de aba ou entrar/sair usa fade.
private typealias Entrada = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Saida = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

private val abas = (itensCliente + itensDona).map { it.rota }.toSet() + Rotas.LOGIN

private fun AnimatedContentTransitionScope<NavBackStackEntry>.trocaDeAba() =
    initialState.destination.route in abas && targetState.destination.route in abas

private val entrar: Entrada = {
    if (trocaDeAba()) fadeIn(tween(DURACAO_MS)) else slideIntoContainer(SlideDirection.Start, tween(DURACAO_MS))
}
private val sair: Saida = {
    if (trocaDeAba()) fadeOut(tween(DURACAO_MS)) else slideOutOfContainer(SlideDirection.Start, tween(DURACAO_MS))
}
private val voltarEntrando: Entrada = { slideIntoContainer(SlideDirection.End, tween(DURACAO_MS)) }
private val voltarSaindo: Saida = { slideOutOfContainer(SlideDirection.End, tween(DURACAO_MS)) }

/** Vai para `rota` limpando a pilha, para o botão voltar não retornar ao login/painel anterior. */
private fun NavHostController.irLimpandoPilha(rota: String) {
    navigate(rota) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/** Troca de aba da barra inferior sem empilhar telas. */
private fun NavHostController.irParaAba(rota: String, inicio: String) {
    navigate(rota) {
        popUpTo(inicio) { saveState = true }
        launchSingleTop = true
        restoreState = rota != Rotas.SERVICOS
    }
}

@Composable
fun SkalaApp(
    authViewModel: AuthViewModel = viewModel(),
    agendamentoViewModel: AgendamentoViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val auth by authViewModel.estado.collectAsStateWithLifecycle()
    val fluxo by agendamentoViewModel.estado.collectAsStateWithLifecycle()
    val repositorio = agendamentoViewModel.repositorio
    val dias by repositorio.dias.collectAsStateWithLifecycle()
    val agendamentos by repositorio.agendamentos.collectAsStateWithLifecycle()
    val rotaAtual = nav.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    val erro = auth.erro?.let { stringResource(it) }
    val usuario = auth.usuario

    fun aviso(texto: String) = Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
    fun dia(data: LocalDate) = dias[data] ?: DiaFuncionamento(data)
    fun repetirNasProximasSemanas(base: DiaFuncionamento) {
        (1L..4L).forEach { semana -> repositorio.salvarDia(base.copy(data = base.data.plusWeeks(semana))) }
    }

    // Login ou cadastro deram certo: a dona vai para o painel, a cliente para o Início.
    LaunchedEffect(usuario) {
        val rota = nav.currentDestination?.route
        if (usuario != null && (rota == Rotas.LOGIN || rota == Rotas.CADASTRO)) {
            nav.irLimpandoPilha(if (usuario.ehDona) Rotas.DONA_AGENDA else Rotas.INICIO)
        }
    }

    Scaffold(
        containerColor = br.com.skala.app.ui.theme.SkalaColors.Ivory,
        bottomBar = {
            when (rotaAtual) {
                in itensCliente.map { it.rota } -> BarraInferior(itensCliente, rotaAtual) { rota ->
                    if (rota == Rotas.SERVICOS) agendamentoViewModel.iniciar()
                    nav.irParaAba(rota, Rotas.INICIO)
                }
                in itensDona.map { it.rota } -> BarraInferior(itensDona, rotaAtual) { rota -> nav.irParaAba(rota, Rotas.DONA_AGENDA) }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Rotas.LOGIN,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = entrar,
            exitTransition = sair,
            popEnterTransition = voltarEntrando,
            popExitTransition = voltarSaindo,
        ) {
            composable(Rotas.LOGIN) {
                LoginScreen(
                    onLogin = authViewModel::entrar,
                    onCriarConta = {
                        authViewModel.limparErro()
                        nav.navigate(Rotas.CADASTRO)
                    },
                    onEsqueciSenha = { aviso(context.getString(R.string.aviso_em_breve)) },
                    onEntrarComCelular = { aviso(context.getString(R.string.aviso_em_breve)) },
                    carregando = auth.carregando,
                    erro = erro,
                )
            }
            composable(Rotas.CADASTRO) {
                val voltar = {
                    authViewModel.limparErro()
                    nav.popBackStack()
                    Unit
                }
                CadastroScreen(
                    onCadastrar = authViewModel::cadastrar,
                    onVoltar = voltar,
                    onEntrar = voltar,
                    onAbrirTermos = { aviso(context.getString(R.string.aviso_em_breve)) },
                    onAbrirPrivacidade = { aviso(context.getString(R.string.aviso_em_breve)) },
                    carregando = auth.carregando,
                    erro = erro,
                )
            }

            // ---------- Cliente ----------
            composable(Rotas.INICIO) {
                val u = usuario ?: return@composable
                val agora = LocalDateTime.now()
                val proximo = agendamentos
                    .filter { it.clienteEmail == u.email && it.status == br.com.skala.app.data.StatusAgendamento.CONFIRMADO }
                    .filter { LocalDateTime.of(it.data, it.inicio).isAfter(agora) }
                    .minByOrNull { LocalDateTime.of(it.data, it.inicio) }
                InicioScreen(
                    nome = u.nome,
                    proximo = proximo,
                    onAgendar = {
                        agendamentoViewModel.iniciar()
                        nav.irParaAba(Rotas.SERVICOS, Rotas.INICIO)
                    },
                    onAtalho = { servicoId, categoria ->
                        agendamentoViewModel.iniciar(repositorio.servicos.firstOrNull { it.id == servicoId }, categoria)
                        nav.irParaAba(Rotas.SERVICOS, Rotas.INICIO)
                    },
                    onVerHorarios = { nav.irParaAba(Rotas.AGENDAMENTOS, Rotas.INICIO) },
                    onPerfil = { nav.irParaAba(Rotas.PERFIL, Rotas.INICIO) },
                )
            }
            composable(Rotas.SERVICOS) {
                ServicosScreen(
                    servicos = repositorio.servicos,
                    categoriaInicial = fluxo.categoria,
                    selecionadosIniciais = fluxo.servicos.map { it.id },
                    precoFixo = { repositorio.estimarValor(it, null) },
                    // Antes do formulário do cabelo, só dá para somar se nenhum serviço depender dele
                    totalAproximado = { marcados ->
                        if (marcados.temServicoDeCabelo) null else repositorio.estimarTotal(marcados, null)
                    },
                    onVoltar = { nav.irParaAba(Rotas.INICIO, Rotas.INICIO) },
                    onContinuar = { marcados ->
                        agendamentoViewModel.escolherServicos(marcados)
                        nav.navigate(if (marcados.temServicoDeCabelo) Rotas.CABELO else Rotas.HORARIO)
                    },
                )
            }
            composable(Rotas.CABELO) {
                val deCabelo = fluxo.servicos.filter { it.dependeDoCabelo }
                if (deCabelo.isEmpty()) return@composable
                CabeloScreen(
                    servicosDeCabelo = deCabelo,
                    cabelo = fluxo.cabelo,
                    valorEstimado = agendamentoViewModel.valorTotal()?.texto,
                    onAlterar = agendamentoViewModel::atualizarCabelo,
                    onVoltar = { nav.popBackStack() },
                    onContinuar = { nav.navigate(Rotas.HORARIO) },
                )
            }
            composable(Rotas.HORARIO) {
                val servicos = fluxo.servicos
                if (servicos.isEmpty()) return@composable
                val comCabelo = servicos.temServicoDeCabelo
                val minutos = servicos.duracaoTotalMin
                HorarioScreen(
                    resumoServicos = "${servicos.titulo} · ${textoDuracao(minutos)}",
                    passo = if (comCabelo) 3 else 2,
                    totalPassos = if (comCabelo) 4 else 3,
                    dataSelecionada = fluxo.data,
                    horaSelecionada = fluxo.hora,
                    // ler `dias` e `agendamentos` aqui faz a tela atualizar quando a dona muda a agenda
                    statusDia = { d -> dia(d).status },
                    horariosLivres = { d -> agendamentos.size; repositorio.horariosLivres(d, minutos) },
                    onEscolherData = agendamentoViewModel::escolherData,
                    onEscolherHora = agendamentoViewModel::escolherHora,
                    onVoltar = { nav.popBackStack() },
                    onContinuar = { nav.navigate(Rotas.CONFIRMAR) },
                )
            }
            composable(Rotas.CONFIRMAR) {
                val servicos = fluxo.servicos
                val data = fluxo.data
                val hora = fluxo.hora
                val u = usuario
                if (servicos.isEmpty() || data == null || hora == null || u == null) return@composable
                val comCabelo = servicos.temServicoDeCabelo
                ConfirmarScreen(
                    servicos = servicos,
                    data = data,
                    hora = hora,
                    cabelo = fluxo.cabelo,
                    valorDe = agendamentoViewModel::valorDe,
                    valorTotal = agendamentoViewModel.valorTotal(),
                    observacao = fluxo.observacao,
                    passo = if (comCabelo) 4 else 3,
                    totalPassos = if (comCabelo) 4 else 3,
                    remarcando = fluxo.remarcandoId != null,
                    onObservacao = agendamentoViewModel::atualizarObservacao,
                    onEditarCabelo = { nav.navigate(Rotas.CABELO) },
                    onAlterarHorario = { nav.popBackStack(Rotas.HORARIO, inclusive = false) },
                    onVoltar = { nav.popBackStack() },
                    onConfirmar = {
                        if (agendamentoViewModel.confirmar(u) != null) {
                            aviso("Horário confirmado!")
                            nav.navigate(Rotas.AGENDAMENTOS) {
                                popUpTo(Rotas.INICIO)
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable(Rotas.AGENDAMENTOS) {
                val u = usuario ?: return@composable
                AgendamentosScreen(
                    agendamentos = agendamentos.filter { it.clienteEmail == u.email },
                    onRemarcar = {
                        agendamentoViewModel.remarcar(it)
                        nav.navigate(Rotas.HORARIO)
                    },
                    onCancelar = {
                        repositorio.cancelar(it.id)
                        aviso("Horário cancelado")
                    },
                    onAgendarDeNovo = {
                        agendamentoViewModel.repetir(it)
                        nav.navigate(if (it.servicos.temServicoDeCabelo) Rotas.CABELO else Rotas.HORARIO)
                    },
                )
            }
            composable(Rotas.PERFIL) {
                val u = usuario ?: return@composable
                PerfilScreen(
                    usuario = u,
                    onHistorico = { nav.irParaAba(Rotas.AGENDAMENTOS, Rotas.INICIO) },
                    onSair = {
                        authViewModel.sair()
                        agendamentoViewModel.iniciar()
                        nav.irLimpandoPilha(Rotas.LOGIN)
                    },
                )
            }

            // ---------- Dona do salão ----------
            composable(Rotas.DONA_AGENDA) {
                val hoje = LocalDate.now()
                AgendaDiaScreen(
                    dia = dia(hoje),
                    agendamentos = agendamentos,
                    onAjustarDia = { nav.navigate(Rotas.donaDia(hoje)) },
                )
            }
            composable(Rotas.DONA_HORARIOS) {
                HorariosScreen(
                    dia = ::dia,
                    onEditarDia = { nav.navigate(Rotas.donaDia(it)) },
                    onRepetirSemanas = { d ->
                        repetirNasProximasSemanas(dia(d))
                        aviso("Repetido nas próximas 4 semanas")
                    },
                    onFecharDia = { d ->
                        repositorio.salvarDia(dia(d).copy(status = StatusDia.FECHADO))
                        aviso("Dia fechado")
                    },
                )
            }
            composable(Rotas.DONA_DIA) { entrada ->
                val data = entrada.arguments?.getString("data")?.let(LocalDate::parse) ?: return@composable
                EditarDiaScreen(
                    diaInicial = dia(data),
                    onVoltar = { nav.popBackStack() },
                    onSalvar = { novo, repetir ->
                        repositorio.salvarDia(novo)
                        if (repetir) repetirNasProximasSemanas(novo)
                        aviso(if (repetir) "Dia salvo e repetido" else "Dia salvo")
                        nav.popBackStack()
                    },
                )
            }
            composable(Rotas.DONA_PERFIL) {
                val u = usuario ?: return@composable
                PerfilScreen(
                    usuario = u,
                    onHistorico = { nav.irParaAba(Rotas.DONA_HORARIOS, Rotas.DONA_AGENDA) },
                    onSair = {
                        authViewModel.sair()
                        nav.irLimpandoPilha(Rotas.LOGIN)
                    },
                )
            }
        }
    }
}
