# Primeira assinatura de upload do Block War

## Situação confirmada em 20/10/2026

A tela “Todos os pacotes de apps” enviada pelo usuário está vazia. Não há um AAB anterior listado para o qual seja necessário recuperar a chave de upload. A tela de assinatura já exibida corresponde à assinatura do aplicativo gerenciada pelo Google; ela é diferente da assinatura de upload local.

A informação inicial de que já havia sido enviado AAB não foi corroborada pela lista completa de pacotes. Não executar redefinição de upload ou mudar a assinatura do app com base nessa informação inicial.

Nenhuma chave de produção foi gerada automaticamente. O pacote com.blockwar.game e a assinatura de depuração foram preservados. O versionCode 1 foi mantido para o primeiro envio.

## Gerar a primeira chave e o AAB

No terminal PowerShell do Android Studio:

```powershell
& 'C:\Users\el_ke\Downloads\block-war\scripts\Gerar-AAB.ps1'
```

O script pede uma senha escolhida pelo usuário e a confirmação, sem mostrar o conteúdo. A senha deve ter pelo menos 12 caracteres. Não enviá-la pelo chat.

- A chave privada será criada em `C:/Users/el_ke/.block-war-signing/upload-keystore.jks`, fora do projeto.
- Alias: `upload`; RSA de 3072 bits; validade de 10000 dias; formato PKCS12.
- Uma chave já existente nesse caminho será reutilizada, nunca substituída automaticamente.
- O certificado público PEM será exportado ao lado da chave.
- As senhas são transmitidas aos processos locais por variáveis temporárias de ambiente. Não são gravadas em arquivo de configuração, no Git ou na linha de comando.
- O Gradle roda sem cache de configuração e sem daemon reutilizável para evitar persistir as credenciais nessa configuração.
- Se o build falhar após criar a chave, ela é preservada para a próxima tentativa. Não gerar outra chave por causa de uma falha de compilação.
- O AAB será gerado em `app/build/outputs/bundle/release/app-release.aab`.
- Nenhum envio ou publicação na Play Console é feito pelo script.

Guardar uma cópia segura da chave fora deste computador e guardar a senha separadamente. Sem a senha, o arquivo da chave não basta para assinar novamente. Não enviar a chave ou a senha ao Git ou pelo chat.

## Próximas conferências

Após gerar o AAB, usar a faixa de teste interno da Play Console para o primeiro envio. Manter a assinatura do aplicativo gerenciada pelo Google.

Para Firebase/Play Games, conferir os certificados da assinatura de distribuição do Google Play. O certificado da chave de upload não substitui esses certificados; APK de debug, AAB local e APK distribuído pela loja podem ter identidades de assinatura diferentes.

O arquivo `17:54:DB:96:34:E9:0C:F7:73:A2:2B:37:32:D8:7B:B4:8A:D4:00:11:D6:01:A3:28:18:40:63:C5:D9:1B:E2:7F` foi informado inicialmente como impressão digital. As imagens posteriores mostraram a área de assinatura do app, portanto ele não deve ser tratado como certificado de upload confirmado.

## Validação da preparação

A sintaxe PowerShell e o modo `-CheckOnly` foram verificados. JDK e Gradle foram encontrados. A criação da chave e o build assinado ainda dependem da senha digitada localmente pelo usuário; não declarar o AAB assinado como pronto antes de concluir esse passo.

Documentação oficial: https://support.google.com/googleplay/android-developer/answer/9842756?hl=pt-BR

## Chave e pacote gerados

Em 20/10/2026, após o usuário executar o script e digitar a senha localmente:

- Keystore existente em `C:/Users/el_ke/.block-war-signing/upload-keystore.jks`.
- Certificado público existente em `C:/Users/el_ke/.block-war-signing/upload-certificate.pem`.
- AAB existente em `app/build/outputs/bundle/release/app-release.aab`, com 17.737.127 bytes na verificação.
- `jarsigner -verify` retornou `jar verified` e código de saída 0. O certificado é autoassinado, como é comum em chaves de upload Android; essa verificação não equivale a aceite da Play Console.
- O certificado extraído do AAB corresponde ao PEM gerado pelo script.
- SHA-256 público da nova chave de upload: `63:64:22:84:C2:39:0D:0D:1D:43:00:9A:41:0F:C9:7E:B4:1C:23:1A:B6:4E:44:56:C4:19:8D:48:36:FD:9A:FB`.
- Integridade ZIP do AAB verificada sem entradas corrompidas; configuração e manifesto do módulo base presentes.

Ainda falta enviar o AAB à faixa de teste interno e conferir a validação da Play Console. Não houve upload nem publicação automática. A senha não foi solicitada pelo chat, lida ou armazenada no projeto.
