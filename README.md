# Meu Financeiro 💰📱

Aplicativo moderno, minimalista e intuitivo de gestão e controle financeiro pessoal desenvolvido em **Kotlin** com **Jetpack Compose** e **Material Design 3**.

---

## 🚀 Instalação Rápida (Sem Compilar)

Se você deseja apenas instalar e usar o aplicativo diretamente no seu celular Android sem precisar abrir código ou compilar:

### 📍 Localização do APK Pronto para Instalação:
O arquivo instalável já compilado e assinado está disponível em:
- **`APK/app-release.apk`** (ou também em `release/app-release.apk` e na raiz `MeuFinanceiro.apk`)

Para futuras publicações na Google Play Store, o Android App Bundle também já está gerado em:
- **`release/app-release.aab`**

---

## 📲 Como Instalar o APK no Celular Android

1. **Baixe ou Transfira o APK para o Celular**:
   - Faça o download do arquivo `APK/app-release.apk` diretamente no seu celular pelo GitHub/navegador, ou transfira do computador para o celular via cabo USB, Google Drive, Telegram ou WhatsApp.
2. **Abra o Arquivo no Celular**:
   - Abra o gerenciador de arquivos do seu celular ou toque na notificação de download do arquivo `app-release.apk`.
3. **Autorize a Instalação (se solicitado)**:
   - Se o Android exibir a mensagem *"Para sua segurança, seu smartphone não tem permissão para instalar apps desconhecidos desta fonte"*:
     - Toque em **Configurações**.
     - Ative a opção **Permitir desta fonte** (ou *Instalar apps desconhecidos*).
     - Volte para a tela anterior.
4. **Conclua a Instalação**:
   - Toque em **Instalar**.
   - Ao finalizar, toque em **Abrir** e comece a gerenciar suas finanças!

---

## 🛠️ Como Abrir o Projeto no Android Studio

1. **Pré-requisitos**:
   - Android Studio (versão Hedgehog / Iguana / Jellyfish / Ladybug ou mais recente).
   - JDK 17 ou JDK 21.
   - Android SDK 34+.
2. **Abrir o Projeto**:
   - Clone ou descompacte o repositório em uma pasta local.
   - No Android Studio, selecione **File > Open...** (ou *Open an Existing Project*).
   - Navegue até a pasta do projeto e selecione o diretório raiz.
   - Aguarde a sincronização automática do Gradle (Gradle Sync).

---

## ⚙️ Como Gerar um Novo APK ou AAB

Caso você faça modificações no código e queira gerar uma nova versão:

### Pelo Terminal / Linha de Comando:
```bash
# Gerar APK de Release:
STORE_PASSWORD=android KEY_PASSWORD=android gradle :app:assembleRelease

# Gerar Android App Bundle (.aab) para a Google Play:
STORE_PASSWORD=android KEY_PASSWORD=android gradle :app:bundleRelease

# Ou para compilar a versão de depuração (Debug):
gradle :app:assembleDebug
```

### Pelo Android Studio (Interface Gráfica):
1. No menu superior, clique em **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Para gerar uma versão assinada, vá em **Build > Generate Signed Bundle / APK...**, selecione APK ou Android App Bundle, aponte para `my-upload-key.jks` (senha: `android`, alias: `upload`) e conclua o assistente.

Os arquivos gerados ficam localizados em:
- APK de Release: `app/build/outputs/apk/release/app-release.apk`
- AAB de Release: `app/build/outputs/bundle/release/app-release.aab`

---

## ✨ Recursos e Destaques do Aplicativo

- 🎨 **Visual Moderno e Acessível**: Cards com elevação e bordas suaves perfeitamente diferenciados do fundo, tanto no **Modo Claro** quanto no **Modo Escuro**.
- ⬛ **Ícone Minimalista em Preto e Branco**: Ícone adaptativo preto e branco com identificação imediata na tela inicial do Android e suporte a ícones temáticos do Material You.
- 💵 **Gestão Completa de Receitas**: Registro de múltiplas fontes de renda (Salário, Extras, Comissões, etc.) com totalização automática.
- 💳 **Despesas com Parcelamento e Recorrência**:
  - Despesas únicas.
  - Despesas fixas mensais (sem data de término).
  - Compras parceladas com quantidade livre de parcelas (ex: 1/12, 2/12) e contagem decrescente de parcelas restantes.
- 🚦 **Controle de Status das Despesas**:
  - 🟢 **Pagas**
  - 🟡 **A Pagar / Pendentes**
  - 🔴 **Vencidas** (com destaque em vermelho e aviso claro)
  - Botão de ação rápida para marcar como paga com um único toque.
- 🎯 **Teto e Limite por Categoria**: Limite percentual flexível configurável para qualquer categoria (Lazer, Alimentação, etc.) com cálculo automático em R$ e alerta visual ao ultrapassar o teto.
- 🛡️ **Saldo Disponível e Projeção**: Cálculo mês a mês com opção de somar saldos acumulados de meses anteriores e projeção do saldo pós-quitação.
- 🗄️ **Armazenamento 100% Seguro e Offline**: Dados salvos localmente com banco de dados Room (SQLite) com total privacidade.
