package br.com.skala.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.skala.app.avisos.AvisosDoApp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.FakeAuthRepository
import br.com.skala.app.data.FakeSalaoRepository
import br.com.skala.app.data.Cliente
import br.com.skala.app.data.ClienteJaCadastrada
import br.com.skala.app.data.DiaFuncionamento
import br.com.skala.app.data.agendamentosForaDoHorario
import br.com.skala.app.data.StatusDia
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
import br.com.skala.app.ui.cliente.MeusDadosScreen
import br.com.skala.app.ui.cliente.PerfilScreen
import br.com.skala.app.ui.cliente.ServicosScreen
import br.com.skala.app.ui.dona.AgendaScreen
import br.com.skala.app.ui.dona.AvisoConflitoDialog
import br.com.skala.app.ui.dona.ClientesScreen
import br.com.skala.app.ui.dona.NovaClienteScreen
import br.com.skala.app.ui.dona.formatarCelular
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
    /** true quando o app foi aberto tocando num lembrete: vai direto para Meus agendamentos. */
    private var abrirAgendamentos by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lê os dados salvos no celular antes de montar as telas
        FakeAuthRepository.iniciar(this)
        FakeSalaoRepository.iniciar(this)
        AvisosDoApp.iniciar(this)
        if (savedInstanceState == null) lerIntent(intent)
        // O app é sempre claro, então os ícones da barra de status ficam escuros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            SkalaTheme {
                SkalaApp(
                    abrirAgendamentos = abrirAgendamentos,
                    onAgendamentosAbertos = { abrirAgendamentos = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lerIntent(intent)
    }

    private fun lerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ABRIR_AGENDAMENTOS, false) == true) abrirAgendamentos = true
    }

    companion object {
        const val EXTRA_ABRIR_AGENDAMENTOS = "abrir_agendamentos"
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

/** Mudança de horário esperando a dona confirmar, porque deixa clientes agendadas fora do expediente. */
private class MudancaPendente(val conflitos: List<Agendamento>, val salvar: () -> Unit)

@Composable
fun SkalaApp(
    abrirAgendamentos: Boolean = false,
    onAgendamentosAbertos: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    agendamentoViewModel: AgendamentoViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val auth by authViewModel.estado.collectAsStateWithLifecycle()
    val fluxo by agendamentoViewModel.estado.collectAsStateWithLifecycle()
    val repositorio = agendamentoViewModel.repositorio
    val dias by repositorio.dias.collectAsStateWithLifecycle()
    val agendamentos by repositorio.agendamentos.collectAsStateWithLifecycle()
    val dadosSalao by repositorio.dadosSalao.collectAsStateWithLifecycle()
    val clientes by repositorio.clientes.collectAsStateWithLifecycle()
    val avisosDona by repositorio.avisosDona.collectAsStateWithLifecycle()
    val rotaAtual = nav.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    val erro = auth.erro?.let { stringResource(it) }
    val usuario = auth.usuario

    fun aviso(texto: String) = Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
    fun dia(data: LocalDate) = dias[data] ?: DiaFuncionamento(data)
    fun proximasSemanas(base: DiaFuncionamento) = (1L..4L).map { base.copy(data = base.data.plusWeeks(it)) }

    // Android 13+: a permissão de notificação é pedida quando a cliente confirma o primeiro horário.
    val pedirPermissaoNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun pedirNotificacoesSePreciso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) pedirPermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    var mudancaPendente by remember { mutableStateOf<MudancaPendente?>(null) }
    var diaCopiado by remember { mutableStateOf<DiaFuncionamento?>(null) }
    var erroNovaCliente by remember { mutableStateOf<String?>(null) }
    // Quando a dona está reservando para uma cliente, o fluxo de agendamento volta para o painel dela.
    val donaReservando = usuario?.ehDona == true
    val nomeParaCliente = fluxo.paraCliente?.nome.takeIf { donaReservando }

    /** A dona escolhe a cliente e entra no mesmo fluxo de agendamento da cliente. */
    fun reservarPara(cliente: Cliente) {
        agendamentoViewModel.iniciarParaCliente(cliente)
        nav.navigate(Rotas.SERVICOS)
    }

    /** Salva os dias; se alguma cliente já agendada ficar com o salão fechado, pergunta antes. */
    fun salvarDias(novos: List<DiaFuncionamento>, depois: () -> Unit) {
        val salvar = {
            novos.forEach(repositorio::salvarDia)
            depois()
        }
        val conflitos = novos.flatMap { it.agendamentosForaDoHorario(agendamentos) }
        if (conflitos.isEmpty()) salvar() else mudancaPendente = MudancaPendente(conflitos, salvar)
    }

    // Login ou cadastro deram certo: a dona vai para o painel, a cliente para o Início.
    LaunchedEffect(usuario) {
        val rota = nav.currentDestination?.route
        if (usuario != null && !usuario.ehDona) {
            repositorio.registrarClienteDoApp(Cliente(usuario.nome, usuario.celular, usuario.email))
        }
        if (usuario != null && (rota == Rotas.LOGIN || rota == Rotas.CADASTRO)) {
            nav.irLimpandoPilha(if (usuario.ehDona) Rotas.DONA_AGENDA else Rotas.INICIO)
        }
    }

    // Cliente logada: os horários dela que ainda não têm lembrete neste celular (como os que a
    // dona marcou por ela) ganham as notificações.
    LaunchedEffect(usuario, agendamentos, dadosSalao) {
        if (usuario != null && !usuario.ehDona) {
            AvisosDoApp.sincronizarCliente(Cliente(usuario.nome, usuario.celular, usuario.email), agendamentos, dadosSalao)
        }
    }

    // Tocou num lembrete: com a cliente logada, abre Meus agendamentos (se não, espera o login).
    LaunchedEffect(abrirAgendamentos, usuario) {
        if (!abrirAgendamentos || usuario == null) return@LaunchedEffect
        if (!usuario.ehDona) nav.irParaAba(Rotas.AGENDAMENTOS, Rotas.INICIO)
        onAgendamentosAbertos()
    }

    Scaffold(
        containerColor = br.com.skala.app.ui.theme.SkalaColors.Ivory,
        bottomBar = {
            when (rotaAtual) {
                in itensCliente.map { it.rota }.takeIf { !donaReservando }.orEmpty() -> BarraInferior(itensCliente, rotaAtual) { rota ->
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
                    .filter { it.eDe(Cliente(u.nome, u.celular, u.email)) && it.status == br.com.skala.app.data.StatusAgendamento.CONFIRMADO }
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
                    onVoltar = {
                        if (donaReservando) nav.popBackStack() else nav.irParaAba(Rotas.INICIO, Rotas.INICIO)
                    },
                    onContinuar = { marcados ->
                        agendamentoViewModel.escolherServicos(marcados)
                        nav.navigate(if (marcados.temServicoDeCabelo) Rotas.CABELO else Rotas.HORARIO)
                    },
                    paraCliente = nomeParaCliente,
                )
            }
            composable(Rotas.CABELO) {
                val deCabelo = fluxo.servicos.filter { it.dependeDoCabelo }
                if (deCabelo.isEmpty()) return@composable
                CabeloScreen(
                    servicosDeCabelo = deCabelo,
                    cabelo = fluxo.cabelo,
                    tempoEstimado = textoDuracao(fluxo.duracaoMin),
                    onAlterar = agendamentoViewModel::atualizarCabelo,
                    onVoltar = { nav.popBackStack() },
                    onContinuar = { nav.navigate(Rotas.HORARIO) },
                    paraCliente = nomeParaCliente,
                )
            }
            composable(Rotas.HORARIO) {
                val servicos = fluxo.servicos
                if (servicos.isEmpty()) return@composable
                val comCabelo = servicos.temServicoDeCabelo
                val minutos = fluxo.duracaoMin
                HorarioScreen(
                    resumoServicos = "${servicos.titulo} · ${textoDuracao(minutos)}",
                    passo = if (comCabelo) 3 else 2,
                    totalPassos = if (comCabelo) 4 else 3,
                    dataSelecionada = fluxo.data,
                    horaSelecionada = fluxo.hora,
                    // ler `dias` e `agendamentos` aqui faz a tela atualizar quando a dona muda a agenda
                    statusDia = { d -> dia(d).status },
                    horariosLivres = { d -> agendamentos.size; repositorio.horariosLivres(d, minutos, fluxo.remarcandoId) },
                    onEscolherData = agendamentoViewModel::escolherData,
                    onEscolherHora = agendamentoViewModel::escolherHora,
                    onVoltar = { nav.popBackStack() },
                    onContinuar = { nav.navigate(Rotas.CONFIRMAR) },
                    paraCliente = nomeParaCliente,
                )
            }
            composable(Rotas.CONFIRMAR) {
                val servicos = fluxo.servicos
                val data = fluxo.data
                val hora = fluxo.hora
                val u = usuario
                // Se o cabelo foi editado e o horário deixou de caber, volta para escolher outro
                if (servicos.isNotEmpty() && data != null && hora == null) {
                    LaunchedEffect(Unit) { nav.popBackStack(Rotas.HORARIO, inclusive = false) }
                }
                if (servicos.isEmpty() || data == null || hora == null || u == null) return@composable
                val comCabelo = servicos.temServicoDeCabelo
                ConfirmarScreen(
                    servicos = servicos,
                    data = data,
                    hora = hora,
                    cabelo = fluxo.cabelo,
                    observacao = fluxo.observacao,
                    passo = if (comCabelo) 4 else 3,
                    totalPassos = if (comCabelo) 4 else 3,
                    remarcando = fluxo.remarcandoId != null,
                    onObservacao = agendamentoViewModel::atualizarObservacao,
                    onEditarCabelo = { nav.navigate(Rotas.CABELO) },
                    onAlterarHorario = { nav.popBackStack(Rotas.HORARIO, inclusive = false) },
                    onVoltar = { nav.popBackStack() },
                    onConfirmar = {
                        val paraCliente = fluxo.paraCliente
                        if (agendamentoViewModel.confirmar(u) != null) {
                            if (donaReservando && paraCliente != null) {
                                aviso("Horário reservado para ${paraCliente.nome}")
                                nav.navigate(Rotas.DONA_AGENDA) {
                                    popUpTo(Rotas.DONA_AGENDA) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                aviso("Horário confirmado!")
                                pedirNotificacoesSePreciso()
                                nav.navigate(Rotas.AGENDAMENTOS) {
                                    popUpTo(Rotas.INICIO)
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                    paraCliente = nomeParaCliente,
                )
            }
            composable(Rotas.AGENDAMENTOS) {
                val u = usuario ?: return@composable
                AgendamentosScreen(
                    agendamentos = agendamentos.filter { it.eDe(Cliente(u.nome, u.celular, u.email)) },
                    onRemarcar = {
                        agendamentoViewModel.remarcar(it)
                        nav.navigate(Rotas.HORARIO)
                    },
                    onCancelar = {
                        agendamentoViewModel.cancelar(it, pelaCliente = true)
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
                    salao = dadosSalao,
                    onHistorico = { nav.irParaAba(Rotas.AGENDAMENTOS, Rotas.INICIO) },
                    onMeusDados = { nav.navigate(Rotas.MEUS_DADOS) },
                    onSair = {
                        authViewModel.sair()
                        agendamentoViewModel.iniciar()
                        nav.irLimpandoPilha(Rotas.LOGIN)
                    },
                )
            }

            // ---------- Dona do salão ----------
            composable(Rotas.DONA_AGENDA) {
                AgendaScreen(
                    dia = ::dia,
                    agendamentos = agendamentos,
                    onAjustarDia = { nav.navigate(Rotas.donaDia(it)) },
                    avisos = avisosDona,
                    onMarcarAvisosLidos = repositorio::marcarAvisosLidos,
                )
            }
            composable(Rotas.DONA_CLIENTES) {
                ClientesScreen(
                    clientes = clientes,
                    agendamentos = agendamentos,
                    onNovaCliente = {
                        erroNovaCliente = null
                        nav.navigate(Rotas.DONA_NOVA_CLIENTE)
                    },
                    onReservar = ::reservarPara,
                )
            }
            composable(Rotas.DONA_NOVA_CLIENTE) {
                NovaClienteScreen(
                    erro = erroNovaCliente,
                    onVoltar = { nav.popBackStack() },
                    onSalvar = { cliente, reservar ->
                        repositorio.cadastrarCliente(cliente)
                            .onSuccess {
                                erroNovaCliente = null
                                aviso("${it.nome} cadastrada")
                                nav.popBackStack()
                                if (reservar) reservarPara(it)
                            }
                            .onFailure { e ->
                                erroNovaCliente = if (e is ClienteJaCadastrada) {
                                    "O celular ${formatarCelular(e.existente.celular)} já é de ${e.existente.nome}."
                                } else {
                                    "Não foi possível cadastrar. Tente de novo."
                                }
                            }
                    },
                )
            }
            composable(Rotas.DONA_HORARIOS) {
                HorariosScreen(
                    dia = ::dia,
                    onEditarDia = { nav.navigate(Rotas.donaDia(it)) },
                    onRepetirSemanas = { d ->
                        salvarDias(proximasSemanas(dia(d))) { aviso("Repetido nas próximas 4 semanas") }
                    },
                    onFecharDia = { d ->
                        salvarDias(listOf(dia(d).copy(status = StatusDia.FECHADO))) { aviso("Dia fechado") }
                    },
                    diaCopiado = diaCopiado,
                    onCopiarDia = { d ->
                        diaCopiado = dia(d)
                        aviso("Horário copiado. Escolha o dia e toque em Colar aqui")
                    },
                    onColarDia = { d ->
                        val copia = diaCopiado ?: return@HorariosScreen
                        salvarDias(listOf(copia.copy(data = d))) { aviso("Horários colados") }
                    },
                    onLimparCopia = { diaCopiado = null },
                )
            }
            composable(Rotas.DONA_DIA) { entrada ->
                val data = entrada.arguments?.getString("data")?.let(LocalDate::parse) ?: return@composable
                EditarDiaScreen(
                    diaInicial = dia(data),
                    onVoltar = { nav.popBackStack() },
                    onSalvar = { novo, repetir ->
                        salvarDias(listOf(novo) + if (repetir) proximasSemanas(novo) else emptyList()) {
                            aviso(if (repetir) "Dia salvo e repetido" else "Dia salvo")
                            nav.popBackStack()
                        }
                    },
                )
            }
            composable(Rotas.MEUS_DADOS) {
                val u = usuario ?: return@composable
                MeusDadosScreen(
                    usuario = u,
                    salao = dadosSalao,
                    onVoltar = { nav.popBackStack() },
                    onSalvar = { nome, celular, salao ->
                        authViewModel.atualizarDados(nome, celular)
                        salao?.let(repositorio::salvarDadosSalao)
                        aviso("Dados salvos")
                        nav.popBackStack()
                    },
                )
            }
            composable(Rotas.DONA_PERFIL) {
                val u = usuario ?: return@composable
                PerfilScreen(
                    usuario = u,
                    salao = dadosSalao,
                    onHistorico = { nav.irParaAba(Rotas.DONA_HORARIOS, Rotas.DONA_AGENDA) },
                    onMeusDados = { nav.navigate(Rotas.MEUS_DADOS) },
                    onSair = {
                        authViewModel.sair()
                        nav.irLimpandoPilha(Rotas.LOGIN)
                    },
                )
            }
        }
    }

    mudancaPendente?.let { m ->
        AvisoConflitoDialog(
            conflitos = m.conflitos,
            onAjustar = { mudancaPendente = null },
            onSalvarMesmoAssim = {
                mudancaPendente = null
                m.salvar()
            },
        )
    }
}
