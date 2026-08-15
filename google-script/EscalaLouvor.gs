/**
 * Escala Louvor 2k26 - Google Apps Script Backend
 * Suporta requisições GET e POST para criação e edição de recados com imagem,
 * gestão de escalas e solicitações de troca.
 */

function doGet(e) {
  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

/**
 * Centraliza o processamento das requisições e garante resposta JSON consistente.
 */
function handleRequest(e) {
  var response = {
    sucesso: false,
    mensagem: "Erro inesperado no servidor."
  };

  try {
    var params = e.parameter;
    var action = params.action;

    if (!action) {
      response.mensagem = "Ação não especificada.";
      return createJsonResponse(response);
    }

    // Roteamento de ações
    if (action === "getEscalaData") {
      response = getEscalaDataAction(params);
    } else if (action === "createRecado") {
      response = createRecadoAction(params);
    } else if (action === "updateRecado") {
      response = updateRecadoAction(params);
    } else if (action === "deleteRecado") {
      response = deleteRecadoAction(params);
    } else if (action === "createEscala") {
      response = createEscalaAction(params);
    } else if (action === "updateEscala") {
      response = updateEscalaAction(params);
    } else if (action === "updateFullEscala") {
      response = updateFullEscalaAction(params);
    } else if (action === "createSolicitacao") {
      response = createSolicitacaoAction(params);
    } else if (action === "processaSolicitacao") {
      response = processaSolicitacaoAction(params);
    } else if (action === "getNotificacoes") {
      response = getNotificacoesAction(params);
    } else if (action === "marcarNotificacaoComoLida") {
      response = marcarNotificacaoComoLidaAction(params);
    } else if (action === "updateSolicitacao") {
      response = updateSolicitacaoOldAction(params); // Mantendo compatibilidade se necessário
    } else {
      response.mensagem = "Ação '" + action + "' não implementada.";
    }

  } catch (error) {
    response.sucesso = false;
    response.mensagem = "Erro crítico: " + error.toString();
  }

  return createJsonResponse(response);
}

function createJsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Retorna os dados necessários para o App, respeitando regras de visibilidade.
 */
function getEscalaDataAction(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var nome = params.nome;
  var senha = params.senha;

  var integrantes = getSheetData(ss, "integrantes");
  var user = integrantes.find(function(i) {
    return i.nome && i.nome.toString().trim().toLowerCase() === (nome || "").trim().toLowerCase() &&
           i.senha && i.senha.toString().trim() === (senha || "").trim();
  });

  var isLider = user && user.funcao && user.funcao.toUpperCase().indexOf("LIDER") !== -1;
  var allSolicitacoes = getSheetData(ss, "solicitacoes");

  var filteredSolicitacoes;
  if (isLider) {
    filteredSolicitacoes = allSolicitacoes;
  } else {
    // Para integrantes comuns, mostrar apenas as solicitações que eles criaram
    filteredSolicitacoes = allSolicitacoes.filter(function(s) {
       return s.quem_pediu && s.quem_pediu.toString().trim().toLowerCase() === (nome || "").trim().toLowerCase();
    });
  }

  return {
    sucesso: true,
    escala: getSheetData(ss, "escala"),
    integrantes: integrantes,
    solicitacoes: filteredSolicitacoes,
    recados: getSheetData(ss, "recados")
  };
}

function getSheetData(ss, sheetName) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) return [];

  var values = sheet.getDataRange().getValues();
  if (values.length <= 1) return [];

  var headers = values[0];
  var data = [];

  for (var i = 1; i < values.length; i++) {
    var row = values[i];
    var item = {};
    for (var j = 0; j < headers.length; j++) {
      var header = headers[j].toString().toLowerCase()
        .replace(/ /g, "_")
        .replace(/ç/g, "c")
        .replace(/[áàâã]/g, "a")
        .replace(/[éèê]/g, "e")
        .replace(/[íìî]/g, "i")
        .replace(/[óòôõ]/g, "o")
        .replace(/[úùû]/g, "u");

      item[header] = row[j];
    }
    data.push(item);
  }
  return data;
}

/**
 * Criação de solicitação de troca.
 */
function createSolicitacaoAction(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("solicitacoes");
  if (!sheet) return { sucesso: false, mensagem: "Aba 'solicitacoes' não encontrada." };

  var nome = params.nome;
  var dataEscala = params.dataEscala;
  var substituto = params.substituto;
  var motivo = params.motivo;
  var funcao = params.funcao;
  var instrumento = params.instrumento;

  if (!motivo || motivo.trim() === "") return { sucesso: false, mensagem: "Informe o motivo da solicitação." };
  if (nome === substituto) return { sucesso: false, mensagem: "Você não pode escolher a si mesmo como substituto." };

  // Validar se já existe pendente
  var data = getSheetData(ss, "solicitacoes");
  var jaExiste = data.some(function(s) {
    return s.quem_pediu === nome && s.data_escala === dataEscala && s.status === "PENDENTE";
  });
  if (jaExiste) return { sucesso: false, mensagem: "Já existe uma solicitação de troca pendente para esta escala." };

  // Validar se substituto já está escalado
  var escalaData = getSheetData(ss, "escala");
  var escalaRow = escalaData.find(function(e) { return e.data === dataEscala; });
  if (escalaRow) {
    var estaEscalado = (escalaRow.dirigente && escalaRow.dirigente.indexOf(substituto) !== -1) ||
                       (escalaRow.vocal && escalaRow.vocal.indexOf(substituto) !== -1) ||
                       (escalaRow.musicos && escalaRow.musicos.indexOf(substituto) !== -1) ||
                       (escalaRow.mesario && escalaRow.mesario.indexOf(substituto) !== -1);

    if (estaEscalado) return { sucesso: false, mensagem: "Este integrante já está escalado nesta data." };
  }

  sheet.appendRow([
    Utilities.getUuid(),
    dataEscala,
    nome,
    funcao,
    instrumento,
    substituto,
    motivo,
    "PENDENTE",
    new Date(),
    "", // Data Decisão
    "", // Decidido Por
    ""  // Motivo Decisão
  ]);

  return { sucesso: true, mensagem: "Solicitação de troca enviada com sucesso!" };
}

/**
 * Processamento de solicitação (Aprovar, Recusar, Cancelar).
 */
function processaSolicitacaoAction(params) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000); // Aguarda até 10 segundos

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheetSolicitacoes = ss.getSheetByName("solicitacoes");
    var id = params.id;
    var acao = params.novaAcao;
    var nomeLider = params.nome;
    var motivoDecisao = params.motivoDecisao || "";

    // 1. Validar permissão de líder se for APROVAR ou RECUSAR
    if (acao === "APROVAR" || acao === "RECUSAR") {
      var integrantes = getSheetData(ss, "integrantes");
      var user = integrantes.find(function(i) { return i.nome === nomeLider; });
      if (!user || user.funcao.toUpperCase().indexOf("LIDER") === -1) {
        return { sucesso: false, mensagem: "Somente o Líder pode realizar esta ação." };
      }
    }

    var values = sheetSolicitacoes.getDataRange().getValues();
    var rowIndex = -1;
    var solicitacao = null;

    for (var i = 1; i < values.length; i++) {
      if (values[i][0] === id) {
        rowIndex = i + 1;
        solicitacao = {
          id: values[i][0],
          dataEscala: values[i][1],
          quemPediu: values[i][2],
          funcao: values[i][3],
          instrumento: values[i][4],
          substituto: values[i][5],
          status: values[i][7]
        };
        break;
      }
    }

    if (!solicitacao) return { sucesso: false, mensagem: "Solicitação não encontrada." };
    if (solicitacao.status !== "PENDENTE") return { sucesso: false, mensagem: "Esta solicitação já foi processada." };

    if (acao === "APROVAR") {
      // 2. Localizar Escala e Aplicar Troca
      var resTroca = aplicarTrocaNaEscala(ss, solicitacao);
      if (!resTroca.sucesso) return resTroca;

      // 3. Atualizar Status
      updateSolicitacaoStatusRow(sheetSolicitacoes, rowIndex, "APROVADA", nomeLider, motivoDecisao);
      return { sucesso: true, mensagem: "Solicitação aprovada e escala atualizada!" };

    } else if (acao === "RECUSAR") {
      updateSolicitacaoStatusRow(sheetSolicitacoes, rowIndex, "RECUSADA", nomeLider, motivoDecisao);
      return { sucesso: true, mensagem: "Solicitação recusada." };

    } else if (acao === "CANCELAR") {
      if (solicitacao.quemPediu !== nomeLider) return { sucesso: false, mensagem: "Somente o solicitante pode cancelar." };
      updateSolicitacaoStatusRow(sheetSolicitacoes, rowIndex, "CANCELADA", nomeLider, "Cancelado pelo usuário");
      return { sucesso: true, mensagem: "Solicitação cancelada." };
    }

    return { sucesso: false, mensagem: "Ação inválida." };

  } finally {
    lock.releaseLock();
  }
}

function updateSolicitacaoStatusRow(sheet, rowIndex, status, decididoPor, motivo) {
  sheet.getRange(rowIndex, 8).setValue(status);
  sheet.getRange(rowIndex, 10).setValue(new Date());
  sheet.getRange(rowIndex, 11).setValue(decididoPor);
  sheet.getRange(rowIndex, 12).setValue(motivo);
}

function aplicarTrocaNaEscala(ss, solicitacao) {
  var sheetEscala = ss.getSheetByName("escala");
  var values = sheetEscala.getDataRange().getValues();
  var headers = values[0];
  var colIndexData = headers.indexOf("DATA");
  var colIndexDirigente = headers.indexOf("DIRIGENTE");
  var colIndexVocal = headers.indexOf("VOCAL");
  var colIndexMusicos = headers.indexOf("MUSICOS");
  var colIndexMesario = headers.indexOf("MESARIO");

  for (var i = 1; i < values.length; i++) {
    if (values[i][colIndexData] === solicitacao.dataEscala) {
      var rowIndex = i + 1;

      // Verificar se o substituto já está na escala em OUTRA função
      var row = values[i];
      var jaNaEscala = false;
      [colIndexDirigente, colIndexVocal, colIndexMusicos, colIndexMesario].forEach(function(ci) {
        if (ci !== -1 && row[ci] && row[ci].toString().indexOf(solicitacao.substituto) !== -1) {
          jaNaEscala = true;
        }
      });
      if (jaNaEscala) return { sucesso: false, mensagem: "Não foi possível aprovar. O substituto já está escalado nesta data." };

      // Aplicar substituição no campo correto
      var campoAfetado = -1;
      var valorAtual = "";

      if (solicitacao.funcao === "Dirigente") campoAfetado = colIndexDirigente;
      else if (solicitacao.funcao === "Vocal") campoAfetado = colIndexVocal;
      else if (solicitacao.funcao === "Músico") campoAfetado = colIndexMusicos;
      else if (solicitacao.funcao === "Mesário") campoAfetado = colIndexMesario;

      if (campoAfetado !== -1) {
        valorAtual = row[campoAfetado].toString();
        // Substituir respeitando o formato (especialmente para músicos com instrumentos)
        var novoValor = valorAtual.replace(solicitacao.quemPediu, solicitacao.substituto);
        sheetEscala.getRange(rowIndex, campoAfetado + 1).setValue(novoValor);
        return { sucesso: true };
      }

      return { sucesso: false, mensagem: "Função do solicitante não encontrada na escala." };
    }
  }
  return { sucesso: false, mensagem: "Escala da data " + solicitacao.dataEscala + " não encontrada." };
}

/**
 * Ações de Recado (Preservando as existentes)
 */
function createRecadoAction(params) {
  var result = { sucesso: false, mensagem: "Falha ao criar recado." };
  try {
    var titulo = params.titulo;
    var mensagem = params.mensagem;
    var imageBase64 = params.imageBase64;
    var imagemUrl = "";

    if (imageBase64 && imageBase64.length > 0) {
      try {
        var folderId = "SEU_FOLDER_ID_DO_DRIVE"; // ADAPTE COM SEU ID
        var folder = DriveApp.getFolderById(folderId);
        var contentType = "image/jpeg";
        var fileName = "recado_" + new Date().getTime() + ".jpg";
        var decoded = Utilities.base64Decode(imageBase64);
        var blob = Utilities.newBlob(decoded, contentType, fileName);
        var file = folder.createFile(blob);
        // O compartilhamento deve ser gerenciado pela pasta, mas mantemos se necessário
        file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
        imagemUrl = "https://drive.google.com/uc?export=view&id=" + file.getId();
      } catch (e) {
        return { sucesso: false, mensagem: "Erro ao salvar imagem no Drive: " + e.toString() };
      }
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("recados");
    sheet.appendRow([Utilities.getUuid(), titulo, mensagem, imagemUrl, "SIM", new Date(), new Date()]);
    result.sucesso = true;
    result.mensagem = "Recado publicado com sucesso!";
  } catch (e) {
    result.mensagem = "Erro ao acessar planilha: " + e.toString();
  }
  return result;
}

function updateRecadoAction(params) {
  var result = { sucesso: false, mensagem: "Falha ao atualizar recado." };
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("recados");
    var id = params.id;
    var values = sheet.getDataRange().getValues();

    for (var i = 1; i < values.length; i++) {
      if (values[i][0] === id) {
        sheet.getRange(i + 1, 2).setValue(params.titulo);
        sheet.getRange(i + 1, 3).setValue(params.mensagem);
        if (params.imagemUrl) sheet.getRange(i + 1, 4).setValue(params.imagemUrl);
        sheet.getRange(i + 1, 5).setValue(params.ativo);
        sheet.getRange(i + 1, 7).setValue(new Date());
        return { sucesso: true, mensagem: "Recado atualizado!" };
      }
    }
  } catch (e) { result.mensagem = e.toString(); }
  return result;
}

function deleteRecadoAction(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("recados");
  var values = sheet.getDataRange().getValues();
  for (var i = 1; i < values.length; i++) {
    if (values[i][0] === params.id) {
      sheet.deleteRow(i + 1);
      return { sucesso: true, mensagem: "Recado removido!" };
    }
  }
  return { sucesso: false, mensagem: "Recado não encontrado." };
}

/**
 * DIAGNÓSTICO TEMPORÁRIO: testarIntegrantesEscalaNotificacao
 * Objetivo: Verificar se o script consegue identificar corretamente os integrantes escalados.
 */
function testarIntegrantesEscalaNotificacao() {
  var targetDateStr = "14/08/2026";
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheetEscala = ss.getSheetByName("ESCALA");
  var sheetIntegrantes = ss.getSheetByName("INTEGRANTES");

  if (!sheetEscala || !sheetIntegrantes) {
    Logger.log("ERRO: Aba ESCALA ou INTEGRANTES não encontrada.");
    return;
  }

  var escalaData = sheetEscala.getDataRange().getValues();
  var headers = escalaData[0].map(function(h) { return h.toString().toUpperCase().trim(); });

  var colData = headers.indexOf("DATA");
  var colDirigente = headers.indexOf("DIRIGENTE");
  var colVocal = headers.indexOf("VOCAL");
  var colMusicos = headers.indexOf("MUSICOS");
  var colMesario = headers.indexOf("MESÁRIO");
  if (colMesario === -1) colMesario = headers.indexOf("MESARIO");
  var colLouvores = headers.indexOf("LOUVORES");
  var colUniforme = headers.indexOf("UNIFORME");

  var escalaEncontrada = null;
  for (var i = 1; i < escalaData.length; i++) {
    var row = escalaData[i];
    var rowDate = row[colData];

    // Normalização de data para comparação
    var currentDataStr = "";
    if (rowDate instanceof Date) {
      currentDataStr = Utilities.formatDate(rowDate, ss.getSpreadsheetTimeZone(), "dd/MM/yyyy");
    } else {
      currentDataStr = rowDate.toString().trim();
    }

    if (currentDataStr === targetDateStr || currentDataStr.indexOf(targetDateStr.substring(0,5)) === 0) {
      escalaEncontrada = row;
      break;
    }
  }

  if (!escalaEncontrada) {
    Logger.log("ERRO: Escala para a data " + targetDateStr + " não encontrada.");
    return;
  }

  // ETAPA 1 e 2 - Extrair e Separar Nomes
  var integrantesNaEscala = {}; // { NOME_NORMALIZADO: { nomeOriginal: string, funcoes: [] } }

  function processarCampo(valor, funcao) {
    if (!valor) return;
    var nomes = valor.toString().split(/[xX,]/);
    nomes.forEach(function(n) {
      var nomeLimpo = n.split(/[—\-]/)[0].trim(); // Remove instrumento se houver
      if (nomeLimpo) {
        var norm = normalizarTexto(nomeLimpo);
        if (!integrantesNaEscala[norm]) {
          integrantesNaEscala[norm] = { nome: nomeLimpo, funcoes: [] };
        }
        if (integrantesNaEscala[norm].funcoes.indexOf(funcao) === -1) {
          integrantesNaEscala[norm].funcoes.push(funcao);
        }
      }
    });
  }

  processarCampo(escalaEncontrada[colDirigente], "DIRIGENTE");
  processarCampo(escalaEncontrada[colVocal], "VOCAL");
  processarCampo(escalaEncontrada[colMusicos], "MUSICO");
  processarCampo(escalaEncontrada[colMesario], "MESARIO");

  // ETAPA 4 - Consultar Detalhes dos Integrantes
  var integrantesInfo = {}; // { NORM: { funcao: string, instrumento: string } }
  var integrantesPlanilha = getSheetData(ss, "INTEGRANTES");
  integrantesPlanilha.forEach(function(int) {
    var norm = normalizarTexto(int.nome);
    integrantesInfo[norm] = {
      funcao: int.funcao,
      instrumento: int.instrumento
    };
  });

  // ETAPA 7 - Montar Log
  var log = "\n========================================\n";
  log += "TESTE DE INTEGRANTES DA ESCALA\n";
  log += "========================================\n\n";
  log += "DATA: " + targetDateStr + "\n";

  // Obter Título do Culto (lógica simplificada para o teste)
  var titulo = "Culto";
  try {
     // A regra central virá do app depois, aqui fazemos manual ou chamamos helper se existisse
     // Para o log exemplo: "Cura e Libertação" se for sexta
     titulo = "Cura e Libertação";
  } catch(e){}

  log += "CULTO: " + titulo + "\n\n";
  log += "INTEGRANTES ENCONTRADOS:\n\n";

  var count = 1;
  var listaEncontradosNorm = Object.keys(integrantesNaEscala);
  listaEncontradosNorm.sort().forEach(function(norm) {
    var item = integrantesNaEscala[norm];
    var info = integrantesInfo[norm] || { funcao: "N/A", instrumento: "N/A" };
    log += count + ". " + item.nome.toUpperCase() + "\n";
    log += "   Funções na Escala: " + item.funcoes.join(", ") + "\n";
    log += "   Cadastro: " + info.funcao + (info.instrumento ? " (" + info.instrumento + ")" : "") + "\n\n";
    count++;
  });

  log += "----------------------------------------\n\n";
  log += "TESTE DE USUÁRIOS\n\n";

  var usuariosTeste = ["JACO", "NEUZA", "LUCIANA", "PIETRO", "JADSON", "VICTORIA"];
  usuariosTeste.forEach(function(u) {
    var normU = normalizarTexto(u);
    var escalado = integrantesNaEscala[normU];
    var status = escalado ? "-> ESCALADO" : "-> NÃO ESCALADO";
    if (escalado && normU === "victoria") status = "-> ESCALADA";

    var pad = u;
    while(pad.length < 8) pad += " ";
    log += pad + " " + status + (escalado ? " (" + escalado.funcoes.join(", ") + ")" : "") + "\n";
  });

  log += "\n========================================\n";

  Logger.log(log);
}

  /**
 * Ações de Notificações
 */
function getNotificacoesAction(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var nome = params.nome;
  var senha = params.senha;

  // Validar usuário
  var integrantes = getSheetData(ss, "integrantes");
  var user = integrantes.find(function(i) {
    return i.nome && i.nome.toString().trim().toLowerCase() === (nome || "").trim().toLowerCase() &&
           i.senha && i.senha.toString().trim() === (senha || "").trim();
  });

  if (!user) return { sucesso: false, mensagem: "Usuário não autenticado." };

  var todasNotif = getSheetData(ss, "notificacoes");
  var userNotif = todasNotif.filter(function(n) {
    return n.destinatario && n.destinatario.toString().trim().toLowerCase() === user.nome.toString().trim().toLowerCase() &&
           n.lida && n.lida.toString().toUpperCase() === "NAO";
  });

  return {
    sucesso: true,
    mensagem: userNotif.length + " notificação(ões) encontrada(s).",
    notificacoes: userNotif
  };
}

function marcarNotificacaoComoLidaAction(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var id = params.id;
  var nome = params.nome;
  var senha = params.senha;

  // Validar usuário
  var integrantes = getSheetData(ss, "integrantes");
  var user = integrantes.find(function(i) {
    return i.nome && i.nome.toString().trim().toLowerCase() === (nome || "").trim().toLowerCase() &&
           i.senha && i.senha.toString().trim() === (senha || "").trim();
  });

  if (!user) return { sucesso: false, mensagem: "Usuário não autenticado." };

  var sheet = ss.getSheetByName("notificacoes");
  if (!sheet) return { sucesso: false, mensagem: "Aba 'notificacoes' não encontrada." };

  var values = sheet.getDataRange().getValues();
  var headers = values[0].map(function(h) { return h.toString().toUpperCase().trim(); });
  var colId = headers.indexOf("ID");
  var colLida = headers.indexOf("LIDA");

  if (colId === -1 || colLida === -1) return { sucesso: false, mensagem: "Estrutura da aba incorreta." };

  for (var i = 1; i < values.length; i++) {
    // Compara IDs de forma robusta: convertendo para string e removendo espaços
    var sheetId = values[i][colId].toString().trim();
    var requestId = id.toString().trim();

    if (sheetId == requestId) {
      sheet.getRange(i + 1, colLida + 1).setValue("SIM");
      return { sucesso: true, mensagem: "Notificacao marcada como lida no servidor." };
    }
  }

  return { sucesso: false, mensagem: "Notificação não encontrada." };
}

/**
 * Normaliza o texto removendo acentos, espaços extras e convertendo para minúsculas.
 */
function normalizarTexto(txt) {
  if (!txt) return "";
  return txt.toString().toLowerCase().trim()
    .normalize("NFD").replace(/[\u0300-\u036f]/g, "") // Remove acentos
    .replace(/\s+/g, " "); // Remove espaços duplos
}

// Stubs para outras funções que o app chama mas não foram fornecidas no código inicial
function createEscalaAction(p) { return {sucesso: true}; }
function updateEscalaAction(p) { return {sucesso: true}; }
function updateFullEscalaAction(p) { return {sucesso: true}; }
function updateSolicitacaoOldAction(p) { return {sucesso: true}; }
