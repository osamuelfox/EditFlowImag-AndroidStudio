# EditFlow Android

**EditFlow** é um aplicativo Android desenvolvido para edição e geração de imagens utilizando Inteligência Artificial. O projeto conecta-se a uma API backend robusta (`fox.api.br`) para oferecer uma experiência fluida de criação de prompts guiados, geração de imagens via IA, e um sistema de gestão de créditos em tempo real para os usuários.

O aplicativo passou por uma reestruturação profunda de UI/UX, aderindo estritamente aos princípios do **Material Design 3**, adotando padrões modernos de arquitetura Android (MVVM + UiState) para garantir um código limpo, testável e manutenível.

## 🚀 Funcionalidades

* **Geração Guiada de Prompts:** Interface interativa utilizando `TabLayout` e `ViewPager2` para guiar o usuário na criação de descrições detalhadas (prompts) para a geração de imagens.
* **Geração de Imagens com IA:** Comunicação com API baseada em nuvem utilizando lógica de *polling* para aguardar o processamento e a finalização das imagens pela IA.
* **Sistema de Créditos:** Monitoramento em tempo real do saldo de créditos do usuário (com fallback via `TokenManager` local), tratamento robusto de erros HTTP 402 (Payment Required) e bloqueio condicional de recursos baseados em créditos disponíveis. Cada novo registro concede automaticamente 2 créditos iniciais.
* **Gestão de Perfil de Usuário:** Autenticação (Login/Registro), gerenciamento de tokens JWT e exibição de dados atualizados do perfil diretamente no aplicativo.
* **Downloads Autenticados:** Integração com **Glide** contendo interceptadores no OkHttp para realizar o download seguro de imagens geradas que exigem autenticação.
* **Interface Moderna e Responsiva (Material Design 3):** Componentes centralizados (`themes.xml`, `colors.xml`, `dimens.xml`), componentes reutilizáveis como `LoadingOverlay` e transições de atividades e animações suaves.

## 🛠️ Tecnologias e Arquitetura

O projeto foi construído utilizando as seguintes bibliotecas e padrões de projeto:

* **Linguagem:** Java 8
* **SDK Android:** Min SDK 24 / Target SDK 34
* **Arquitetura:** Padrão MVVM (Model-View-ViewModel) utilizando `UiState` para gerenciar os estados de tela de forma reativa.
* **ViewBinding:** Para uma interação segura (null-safe e type-safe) com as views XML.
* **Retrofit 2 & OkHttp 3:** Para requisições HTTP RESTful, interceptação de tokens de autenticação (JWT) e tratamento de erros globais.
* **Glide (com integração OkHttp3):** Para o carregamento eficiente, cache e exibição de imagens assíncronas.
* **AndroidX Lifecycle:** `ViewModel`, `LiveData` para observabilidade de dados.
* **Material Components:** Adoção total do `com.google.android.material:material` em sua versão mais recente.

## 📁 Estrutura do Projeto

Abaixo os destaques da organização arquitetônica adotada:

* **`/ui` ou `/view`:** Activities, Fragments e Adapters. Gerencia estritamente o layout e eventos da UI.
* **`/viewmodel`:** Lógica de apresentação, consumo de repositórios e exposição de estados (`UiState`) via LiveData.
* **`/repository`:** Lógica de acesso a dados (APIs, persistência local).
* **`/network` / `/api`:** Interfaces do Retrofit, interceptors do OkHttp e Data Transfer Objects (DTOs).
* **`/utils`:** Classes utilitárias, como o `TokenManager` para gestão do JWT e controle local de créditos.

## ⚙️ Pré-requisitos e Como Rodar

Para executar este projeto, você precisará de:

1. **Android Studio** (versão Hedgehog ou mais recente recomendada).
2. **SDK do Android** instalado (API 34).
3. Dispositivo Físico ou Emulador rodando **Android 7.0 (API 24)** ou superior.

### Passos para rodar localmente:

1. Clone o repositório:
   ```bash
   git clone https://github.com/SEU_USUARIO/EditFlowImag-AndroidStudio.git
   ```
2. Abra o projeto no **Android Studio**.
3. Aguarde o *Gradle Sync* finalizar para baixar todas as dependências (Retrofit, Glide, etc.).
4. (Opcional) Verifique as configurações de API URL em suas classes de Network/Retrofit para apontar corretamente para os servidores de teste/produção (`fox.api.br`).
5. Clique no botão **Run (▶)** ou use o atalho `Shift + F10` para instalar o aplicativo no seu dispositivo/emulador.

## 🛡️ Tratamento de Erros e Polling

Um dos maiores desafios técnicos resolvidos no aplicativo é o fluxo assíncrono de geração de imagens:
1. O App envia o prompt para a API.
2. O servidor retorna um *Task ID*.
3. O App inicia um sistema contínuo de **Polling** consultando o *status* dessa Task até que a imagem seja concluída.
4. Se o usuário estiver sem créditos, o sistema intercepta de imediato o HTTP 402 e exibe a interface de recarga/aviso sem quebrar o fluxo.

## 📝 Documentação Adicional

O projeto conta também com modelos de relatórios acadêmicos (arquivos PDF e DOCX) localizados na raiz do repositório, que fundamentam a arquitetura, refatoração de UI/UX, e metodologias utilizadas para a construção técnica deste cliente.

---

**Desenvolvido com dedicação por Fox e Equipe.** 🦊
