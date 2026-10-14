# Correções das prioridades — 21/10/2026

## PIN

Proteção compartilhada por perfil implementada e publicada nas funções bwProfileLogin, bwChangePin e bwPlayLink. Ver CORRECAO-PIN-2026-10-21.md para limites, testes e ressalvas.

## Sincronização Play Games

O fluxo do SDK agora possui prazo de 120 segundos, e consulta/vínculo no backend possuem prazo de 70 segundos por operação. Cada operação entrega resultado ou erro somente uma vez. Respostas atrasadas são ignoradas e etapas seguintes não são iniciadas depois do vencimento. Cancelamento do SDK também produz erro recuperável. O nome de exibição é opcional e tem fallback após três segundos; sua leitura não impede confirmar a identidade Firebase.

O timeout é de confirmação, não desfaz uma transação já iniciada. Caso o vínculo tenha sido concluído pelo servidor e a resposta não chegue, a mensagem orienta reconectar para consultar o resultado, em vez de presumir falha ou criar outro perfil. Não se apagam contas nem se troca o cliente OAuth que já passou a funcionar.

## Loja

A grade usa largura mínima de 168 dp por cartão, ajustada pela escala de fonte. Em pouco espaço reduz o número de colunas. Preço e ação podem quebrar para linhas separadas. A ação tem altura mínima de 48 dp. A margem inferior e o tratamento de barras Android existentes foram preservados.

## Missões

As nove missões existentes continuam disponíveis com os mesmos identificadores, metas e recompensas. Há três extras diárias, três semanais e três mensais: seis de cada período, dezoito no total. Cada grupo extra alterna entre objetivos de partidas concluídas, linhas, ataques, vitórias, vitórias online e combo máximo.

A seleção é determinística pelo período em Brasília: não muda ao reabrir a tela ou reinstalar o aplicativo. As semanais mudam na segunda-feira e as mensais no início do mês. Progresso e recompensa continuam calculados pelo servidor com resultados verificados; abandono não avança as extras, e resgate/reenvio de resultado não duplica prêmio.

O aplicativo passa a ler também definições vindas do servidor, em vez de descartar identificadores desconhecidos. As versões antigas continuam vendo suas nove missões; a versão 1.0.1 (código 2) exibe as dezoito depois da sincronização. Progresso dos períodos antigos é mantido no servidor. O retorno de uma requisição iniciada antes da virada de período é descartado e consultado novamente.

Servidor validado com 31 testes Node sem falhas. Publicação de bwMissions e bwSettleMatch concluída com status success e saída zero do Firebase CLI; publicação das três funções do PIN também concluída. Não foram republicadas as regras de banco nem a função de entrega de ataques.

## Distribuição e validação

As alterações Android precisam de novo AAB assinado com a chave de upload existente, usando scripts/Gerar-AAB.ps1 e a senha digitada apenas localmente. Não instalar o APK debug sobre o jogo da Play Store, pois as assinaturas são diferentes. Não foi solicitada nem armazenada senha no chat.

Testes Android: 88 executados, zero falhas e zero erros. Incluem entrega única do resultado de autenticação (sucesso/erro/timeout), descarte de respostas atrasadas, aceitação das novas definições de missões e preço/botão dentro do cartão estreito com fonte 1,6 vezes maior. APK debug compilado para validação, sem instalar nos aparelhos.

Gradle concluiu testDebugUnitTest, assembleDebug e lintDebug com BUILD SUCCESSFUL. Nenhum AAB novo foi assinado ou enviado à Play Console nesta etapa; a versão anterior da loja não recebe as mudanças de interface até distribuir o código 2.

Os testes automatizados não substituem conferir a versão nova em dois aparelhos reais, especialmente o retorno de perda de conexão e o último cartão da loja com fonte ampliada. Nenhum aparelho foi desinstalado ou teve dados apagados durante estas correções.
