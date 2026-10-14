# Diagnóstico do Google Play Games — 20/10/2026

## Evidências

O vídeo enviado mostra o seletor de perfil do Google Play Games e o retorno a Ajustes com o status Desconectado. Não mostra um código de erro conclusivo. Os registros recentes consultados não continham um erro específico de GamesSignIn.

No aparelho, com.blockwar.game está instalado pela Play Store (installer com.android.vending), versão 1 / 1.0, sem flag DEBUGGABLE. O perfil principal possui a instalação; o perfil Vault não possui mais o pacote instalado. Nenhum pacote foi removido por esta investigação.

O APK instalado foi copiado sem dados do usuário e seu certificado foi lido com apksigner. Impressões digitais públicas da assinatura de distribuição:

- SHA-1: `18:1D:97:36:C2:41:0A:82:60:8F:3D:70:CA:B5:D4:32:12:AF:64:C5`
- SHA-256: `17:54:DB:96:34:E9:0C:F7:73:A2:2B:37:32:D8:7B:B4:8A:D4:00:11:D6:01:A3:28:18:40:63:C5:D9:1B:E2:7F`

## Correção efetuada

O Firebase Android App correspondente ao pacote com.blockwar.game, projeto block-war-ca797, tinha apenas o SHA-1 `9c495a9f4a2f174934e9ebcb81cdaaf3c82a88d8`, usado na configuração de testes anterior.

Foram acrescentados, com sucesso e confirmação por leitura posterior, o SHA-1 e o SHA-256 da distribuição real acima. O certificado anterior foi mantido. Nenhum perfil, saldo, vínculo, regra de banco ou chave privada foi alterado.

## Situação posterior — 21/10/2026

As imagens posteriores confirmaram que a credencial Android publicada estava associada ao cliente Android Debug. O usuário criou o cliente OAuth Android da distribuição Google Play, vinculou a credencial nos Serviços do Play Games e publicou as alterações. Em seguida confirmou no chat que a conexão funcionou. A pendência de configuração abaixo foi resolvida por esse procedimento; a persistência após reiniciar e a conferência de saldo/itens permanecem verificações de uso.

## Investigação anterior

Conferir na Play Console → Serviços do Play Games → Configuração e administração → Configuração se existe credencial Android vinculada ao pacote com.blockwar.game e ao SHA-1 de distribuição acima. Cadastrar/vincular a credencial da loja se ausente e publicar as alterações dos Serviços do Play Games. Isso é separado da publicação do AAB na faixa de teste interno.

O cliente de servidor usado no código continua o configurado em app/src/main/res/values/play_games.xml. Não foi trocado sem verificar o provedor Firebase/credencial de servidor. A configuração SDK obtida após a inclusão dos certificados ainda não listou um cliente OAuth Android da loja; não foi sobrescrito o google-services.json local com configuração presumidamente suficiente.

Retestar o login e o vínculo após conferir/publicar as credenciais. A correção dos certificados Firebase não é prova de sincronização completa. Não gerar outro AAB apenas por causa do cadastro de certificados, nem reinstalar versão debug sobre a versão da loja, pois as assinaturas diferem.

Referências oficiais:
- https://developer.android.com/games/pgs/android/troubleshooting
- https://firebase.google.com/docs/auth/android/play-games
