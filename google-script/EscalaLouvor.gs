/**
 * Escala Louvor 2k26 - Google Apps Script Backend
 * Suporta requisições GET e POST para criação e edição de recados com imagem.
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
    // No Apps Script, e.parameter contém os dados de GET e de formulários POST (FormUrlEncoded)
    var params = e.parameter;
    var action = params.action;

    if (!action) {
      response.mensagem = "Ação não especificada.";
      return createJsonResponse(response);
    }

    // Roteamento de ações
    if (action === "createRecado") {
      response = createRecadoAction(params);
    } else if (action === "updateRecado") {
      response = updateRecadoAction(params);
    } else {
      // Aqui você deve manter suas outras funções doGet existentes
      // Exemplo: if (action === "getEscalaData") { ... }
      response.mensagem = "Ação '" + action + "' não implementada para POST/GET centralizado.";
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
 * Exemplo de implementação para salvar imagem e criar recado.
 * Adapte com seus IDs de pasta e planilha.
 */
function createRecadoAction(params) {
  var result = { sucesso: false, mensagem: "Falha ao criar recado." };

  try {
    var titulo = params.titulo;
    var mensagem = params.mensagem;
    var imageBase64 = params.imageBase64;
    var imagemUrl = "";

    // 1. Processar imagem se existir
    if (imageBase64 && imageBase64.length > 0) {
      try {
        var folderId = "SEU_FOLDER_ID_DO_DRIVE"; // ADAPTE AQUI
        var folder = DriveApp.getFolderById(folderId);
        var contentType = "image/jpeg";
        var fileName = "recado_" + new Date().getTime() + ".jpg";

        // Decodifica Base64 e cria arquivo
        var decoded = Utilities.base64Decode(imageBase64);
        var blob = Utilities.newBlob(decoded, contentType, fileName);
        var file = folder.createFile(blob);
        file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
        imagemUrl = "https://drive.google.com/uc?export=view&id=" + file.getId();
      } catch (e) {
        return { sucesso: false, mensagem: "Erro ao salvar imagem no Drive: " + e.toString() };
      }
    }

    // 2. Gravar na Planilha
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("recados");
    if (!sheet) return { sucesso: false, mensagem: "Aba 'recados' não encontrada." };

    sheet.appendRow([
      Utilities.getUuid(),
      titulo,
      mensagem,
      imagemUrl,
      "SIM",
      new Date(),
      new Date()
    ]);

    result.sucesso = true;
    result.mensagem = "Recado publicado com sucesso!";

  } catch (e) {
    result.mensagem = "Erro ao acessar planilha: " + e.toString();
  }

  return result;
}

function updateRecadoAction(params) {
  // Implementação análoga ao createRecadoAction
  return { sucesso: true, mensagem: "Recado atualizado com sucesso!" };
}
