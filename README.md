# Block War

Jogo Android em Kotlin e Jetpack Compose: partidas contra CPU, batalhas online, personalização, missões e conquistas. Pacote Android: `com.blockwar.game`.

## Organização
- `app/`: aplicativo Android e testes.
- `server/`: cópia versionada do backend Firebase atualmente publicado, regras do Realtime Database e testes Node.js.
- `docs/ACESSO-REMOTO.md`: continuar o trabalho pelo celular ou preparar um ambiente na nuvem.

A pasta Android principal continua em `C:/Users/el_ke/Downloads/block-war`. O backend operacional existente continua em `C:/Users/el_ke/Downloads/block-war-servidor`; ao alterar esse backend, atualize também a cópia em `server/` antes de sincronizar. Não execute deploy das duas cópias independentemente.

## Android local
Abra a pasta principal no Android Studio. O projeto usa Gradle Wrapper e SDK Android 36, revisão menor 1. Use o JDK do Android Studio ou um JDK compatível com o Gradle configurado.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Em clones novos, configure o SDK em `local.properties` ou `ANDROID_HOME`. Para usar Firebase, forneça `app/google-services.json` pelo canal privado do ambiente; ele não está versionado. O build permite a ausência desse arquivo, mas autenticação e funcionalidades online precisam da configuração correta em execução.

O certificado de debug original está fora do Git. Um clone limpo usa a chave de debug padrão do Android. Um APK com certificado diferente não substitui a instalação existente e precisa do SHA-1 correspondente nas credenciais Android do Play Games. Para manter a identidade atual do APK, forneça a chave existente por canal privado; nunca envie keystores ao repositório.

## Backend
Node.js 22:

```sh
cd server/functions
npm ci
npm test
```

O projeto Firebase é `block-war-ca797`. Deploy requer login e autorização próprios do ambiente. As credenciais de Firebase, contas de serviço, chaves de assinatura e variáveis secretas não estão incluídas. A função de entrega de ataques mantém `retry: false`.

## Conquistas
O perfil contém Conquistas, Estatísticas, Item de customização e Histórico de desempenho. São seis categorias e sete níveis por categoria (42 escudos). Resultados online são validados no servidor. Combos são contados uma vez por sequência de duas ou mais peças consecutivas limpando linhas; registros anteriores à implementação não possuem essa contagem.

## Sincronizar

```sh
git pull --ff-only
git status
git add <arquivos-alterados>
git commit -m "Descreva a mudança"
git push origin main
```

Confira alterações antes de enviar. GitHub não sincroniza alterações automaticamente. Não use force push para resolver divergências.
