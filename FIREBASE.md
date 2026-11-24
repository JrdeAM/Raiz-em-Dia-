# Banco de dados (Firebase)

O Skala usa o **Firebase Authentication** (contas) e o **Cloud Firestore** (dados).

## Situação atual (projeto `skala-a38ca`)

- [x] App Android `br.com.skala.app` registrado, com SHA-1 e SHA-256 da chave de **debug** desta máquina.
- [x] `app/google-services.json` no projeto.
- [x] Login por **e-mail/senha** ativo.
- [x] Firestore em `southamerica-east1` com as regras de [`firestore.rules`](firestore.rules) publicadas.
- [ ] Login por **telefone**: desligado (ativar no passo 2 abaixo; até lá o botão de celular dá erro).
- [ ] **TTL de 60 dias**: exige o plano Blaze. Enquanto isso, quem apaga é o próprio app (passo 5, item 2).
- [x] **Conta da dona**: `donaskala@gmail.com`, já definida em `config/salao.donaUid`. O endereço e o
  WhatsApp ainda são os de exemplo: a dona corrige em "Meus dados" no app.
- [ ] Para gerar o APK de **release**, cadastrar também o SHA-1/SHA-256 da chave de release.

- **Sem** o arquivo `app/google-services.json`, o app continua funcionando só com os dados de teste
  guardados no celular (contas `cliente@skala.com` e `dona@skala.com`, senha `skala123`; no login
  pelo celular o código é sempre `123456`).
- **Com** o arquivo, o app passa a usar o Firebase sozinho (veja `Repositorios.kt`).

## 1. Criar o projeto

1. Em <https://console.firebase.google.com>, crie um projeto.
2. Adicione um app Android com o pacote **`br.com.skala.app`**.
3. Cadastre as impressões digitais **SHA-1 e SHA-256** do app (elas são obrigatórias para o login
   pelo celular). Para a chave de debug:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
   ```
4. Baixe o `google-services.json` e coloque em `app/google-services.json`.

## 2. Ligar o login

Em **Authentication > Método de login**, ative:

- **E-mail/senha**
- **Telefone**: o envio de SMS é cobrado por mensagem e exige o plano **Blaze** (pago conforme o uso).
  Para testar sem gastar, cadastre **números de teste** (ex.: `+55 11 91234-5678` com o código
  `123456`) na mesma tela: eles não enviam SMS.

Em **Authentication > Modelos**, dá para traduzir o e-mail de "Esqueci minha senha" para português.

## 3. Criar o banco

1. Em **Firestore Database**, crie o banco (região `southamerica-east1`, São Paulo).
2. Publique as regras de acesso: copie o conteúdo de [`firestore.rules`](firestore.rules) para a aba
   **Regras**, ou use a CLI: `firebase deploy --only firestore:rules`.

## 4. Definir a conta da dona

1. Abra o app e crie a conta da dona normalmente (Criar conta).
2. Em **Authentication > Usuários**, copie o **UID** dela.
3. Em **Firestore**, crie a coleção `config` com o documento `salao` e os campos:

   | campo      | tipo   | valor                         |
   |------------|--------|-------------------------------|
   | `donaUid`  | string | o UID copiado                 |
   | `nome`     | string | nome do salão                 |
   | `endereco` | string | endereço                      |
   | `whatsapp` | string | só dígitos, ex.: `11934567890` |

4. Saia e entre de novo no app com a conta da dona: o painel dela abre.

O `donaUid` só pode ser alterado pelo console (as regras impedem que alguém se torne dona pelo app).

## 5. Apagar automaticamente depois de 60 dias

Agendamentos, avisos, horários ocupados e dias de funcionamento têm o campo **`apagarEm`**: a data
(60 dias depois do dia do horário) em que podem ser apagados. Há duas proteções:

1. **TTL do Firestore** (recomendado): o próprio Firebase apaga os documentos vencidos, sem depender
   do app. Em **Firestore > Time-to-live (TTL)**, crie uma política com o campo `apagarEm` para cada
   grupo de coleções: `agendamentos`, `avisos`, `ocupacoes` e `dias`. Ou pela linha de comando:
   ```bash
   for c in agendamentos avisos ocupacoes dias; do
     gcloud firestore fields ttls update apagarEm --collection-group=$c --enable-ttl
   done
   ```
   O Firebase apaga em até ~24 h depois do vencimento.
2. **Limpeza pelo app**: sempre que a dona entra, o app apaga o que já venceu
   (`FirebaseSalaoRepository.apagarAntigos`). Funciona mesmo sem a política de TTL.

Para mudar o prazo, altere `DIAS_GUARDADOS` em `data/Salao.kt`.

## Como os dados ficam organizados

| coleção            | o que guarda                                             | quem lê            |
|--------------------|----------------------------------------------------------|--------------------|
| `usuarios/{uid}`   | nome, e-mail e celular de cada conta                      | a própria pessoa e a dona |
| `config/salao`     | dados do salão e `donaUid`                                | todos logados      |
| `clientes/{celular}` | lista de clientes da dona                               | só a dona          |
| `dias/{data}`      | funcionamento do dia (aberto/fechado, faixas, bloqueios)  | todos logados      |
| `agendamentos/{id}`| os horários marcados                                      | a dona e a cliente dona do horário |
| `ocupacoes/{data}` | só início e fim dos horários ocupados (sem dados pessoais) | todos logados      |
| `avisos/{id}`      | avisos de "marcou / desmarcou / remarcou" para a dona     | só a dona          |

Ao confirmar um horário, o app usa uma **transação**: confere no servidor se o horário ainda está
livre e só então grava. Se duas clientes confirmarem o mesmo horário ao mesmo tempo, só uma consegue;
a outra recebe "Esse horário acabou de ser ocupado".
