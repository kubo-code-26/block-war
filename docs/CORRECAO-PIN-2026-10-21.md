# Proteção de tentativas de PIN — 21/10/2026

Foi acrescentado um orçamento compartilhado de 10 verificações de PIN por perfil a cada 15 minutos. Login, troca de PIN e vínculo com Play Games usam o mesmo registro privado, identificado por SHA-256 do apelido normalizado. O limite anterior por UID continua existindo. Uma nova sessão anônima não reinicia o orçamento do perfil.

O prazo começa na primeira tentativa e não é prolongado pelas solicitações recusadas. A conta Google já vinculada ao perfil não passa por esse orçamento, pois o servidor confirma seu UID autenticado. Perfis sem vínculo aguardam o prazo caso o limite seja atingido. O orçamento inclui verificações bem-sucedidas, para não liberar uma rajada concorrente antes de conhecer o resultado; não há bloqueio permanente. A limitação reduz ataques distribuídos, mas não elimina negação temporária de acesso ao PIN por terceiros. Não substitui autenticação forte nem App Check.

Nenhum PIN, saldo, inventário, regra de banco ou chave de assinatura é modificado por esta implementação. Os novos registros ficam sob privateLimits, protegido pelas regras existentes que negam acesso ao cliente.

Validação: 28 testes Node passaram. Seis novos testes cobrem janela, normalização, compartilhamento entre identidades, chamadas concorrentes simuladas, os três handlers reais com infraestrutura substituída e acesso da conta Google durante limitação. Não foram executadas tentativas abusivas no servidor real, nem teste de concorrência com o emulador Firebase.

Arquivos operacionais conferidos antes da cópia: domain.js, catalog.js, replay.js e package.json idênticos ao repositório; index.js idêntico à versão base do Git. Backup do index.js anterior em .audit-backups/20261021-pin. Publicação limitada a bwProfileLogin, bwChangePin e bwPlayLink, sem alterar bwDeliverAttack ou sua política de repetição.

Publicação concluída com sucesso no projeto block-war-ca797 (Firebase CLI: status success, código de saída zero).
