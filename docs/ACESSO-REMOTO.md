# Trabalhar no Block War pelo celular

## Usando o PC de casa (caminho imediato)
1. No aplicativo desktop, Configurações > Conexões > Controlar este PC > Configurar/Adicionar.
2. Autorize a conexão e leia o QR code com o celular.
3. Confirme a mesma conta e workspace do ChatGPT.
4. No celular, abra Codex/Remoto, selecione o computador e encontre esta conversa/projeto.
5. Deixe o PC ligado, conectado e sem suspensão; mantenha o aplicativo aberto. Configure manter acordado nas opções de conexão, se disponível.

Esse modo executa o trabalho no PC: usa os arquivos, Android Studio, SDK, sessões e ferramentas locais. A conexão com GitHub guarda o código, mas não é o acesso remoto em si. Para tarefas de interface no Windows, a sessão precisa estar desbloqueada e disponível.

## Execução inteiramente na nuvem
1. No desktop ou web: Trabalhar em/Work in > Cloud > Selecionar ambiente > Criar ambiente.
2. Escolha `kubo-code-26/block-war` na conexão GitHub.
3. Prepare JDK, Android SDK 36.1, licenças e Node.js 22. Instale as dependências do Gradle e do backend.
4. Forneça arquivos e credenciais privados por recursos de segredos do ambiente; não os coloque no Git.
5. Valide `./gradlew assembleDebug testDebugUnitTest` e `cd server/functions && npm ci && npm test`.
6. Revise e publique o ambiente. Depois selecione-o no Codex do celular e inicie uma tarefa.

O ambiente da nuvem não herda a pasta C:, sessões do Firebase nem o celular conectado por ADB. Esta conversa local não é convertida apenas por conectar GitHub. Um novo trabalho na nuvem deve receber o contexto e usar os commits sincronizados.

Documentação: https://learn.chatgpt.com/docs/remote-connections e https://learn.chatgpt.com/docs/cloud
