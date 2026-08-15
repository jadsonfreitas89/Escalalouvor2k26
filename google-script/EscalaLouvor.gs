/**
 * ================================================================
 * ESCALA DE LOUVOR 2K26
 * BACKEND GOOGLE APPS SCRIPT — UNIFICADO FINAL
 * ================================================================
 *
 * CONSOLIDAÇÃO:
 * - Backend atual;
 * - Alterações adicionadas pela IA do Android Studio;
 * - Integração de louvores com links do YouTube.
 *
 * REGRA DA CONSOLIDAÇÃO:
 * Uma única implementação por funcionalidade.
 * Quando a IA criou funções duplicadas, a lógica robusta existente
 * foi preservada e os nomes novos foram mantidos como aliases.
 *
 * PRESERVADO:
 * - Escalas
 * - Integrantes
 * - Solicitações de troca
 * - Recados
 * - Notificações
 * - Google Drive
 * - Tratamento de datas
 * - Normalização de dados
 * - LockService
 * - Compatibilidade com ações anteriores
 * - Louvores e links do YouTube
 *
 * YOUTUBE:
 * - Aba LINK_LOUVORES
 * - Data, ordem, louvor e link
 * - Sincronização por data
 * - Leitura dos links junto com a escala
 *
 * ================================================================
 */

// ============================================================
// CONFIGURAÇÕES
// ============================================================

var SPREADSHEET_ID =
  "1gUSw3B--ysm8xhhtIvAiV1e8tbrMb7q394QUU13G-F0";

var FOLDER_ID =
  "1nJ1TiTpJSDknAfxWbtotdiOshJ0rZFCe";

var ABA_ESCALA = "ESCALA";
var ABA_INTEGRANTES = "INTEGRANTES";
var ABA_SOLICITACOES = "SOLICITAÇÕES";
var ABA_RECADOS = "RECADOS";
var ABA_NOTIFICACOES = "NOTIFICACOES";


// ============================================================
// GET
// ============================================================

function doGet(e) {
  return handleRequest(e);
}


// ============================================================
// POST
// ============================================================

function doPost(e) {
  return handleRequest(e);
}


// ============================================================
// PROCESSADOR CENTRAL
// ============================================================

function handleRequest(e) {

  // Zera o cache de abas a cada requisição, para nunca reaproveitar
  // dados de uma execução anterior em instâncias "quentes".
  _sheetSecureCache = null;

  var response = {
    sucesso: false,
    mensagem: "Erro inesperado no servidor."
  };

  try {

    e = e || {};

    var params = e.parameter || {};

    var action =
      safeString(params.action)
        .trim();


    // --------------------------------------------------------
    // SEM ACTION
    // --------------------------------------------------------

    if (!action) {

      response =
        getEscalaData(params);

      return responseJSON(response);

    }


    // --------------------------------------------------------
    // ROTEAMENTO
    // --------------------------------------------------------

    switch (action) {

      // ======================================================
      // DADOS
      // ======================================================

      case "getEscalaData":

        response =
          getEscalaData(params);

        break;


      // ======================================================
      // LOUVORES / YOUTUBE
      // ======================================================
      case "getLinkLouvores":
        response = getLinkLouvores(params);
        break;

      // ======================================================
      // NOTIFICAÇÕES
      // ======================================================

      case "getNotificacoes":

        response =
          getNotificacoes(params);

        break;


      case "marcarNotificacaoLida":

        response =
          marcarNotificacaoLida(params);

        break;


      // Compatibilidade com possível nome usado
      // por versões anteriores do aplicativo.

      case "marcarNotificacaoComoLida":

        response =
          marcarNotificacaoLida(params);

        break;


      case "marcarTodasNotificacoesLidas":

        response =
          marcarTodasNotificacoesLidas(params);

        break;


      case "deleteNotificacao":

        response =
          deleteNotificacao(params);

        break;


      case "limparNotificacoesLidas":

        response =
          limparNotificacoesLidas(params);

        break;


      // ======================================================
      // FCM (TOKEN DE PUSH)
      // ======================================================

      case "atualizarTokenFcm":

        response =
          atualizarTokenFcm(params);

        break;


      // ======================================================
      // RECADOS
      // ======================================================

      case "createRecado":

        response =
          createRecado(params);

        break;


      case "updateRecado":

        response =
          updateRecado(params);

        break;


      case "deleteRecado":

        response =
          deleteRecado(params);

        break;


      // ======================================================
      // ESCALAS
      // ======================================================

      case "createEscala":

        response =
          createEscala(params);

        break;


      case "updateEscala":

        response =
          updateEscala(params);

        break;


      case "updateFullEscala":

        response =
          updateFullEscala(params);

        break;


      // ======================================================
      // SOLICITAÇÕES
      // ======================================================

      // ======================================================
      // COMPATIBILIDADE COM AÇÕES "Action"
      // ======================================================
      // Algumas versões geradas pela IA usam estes nomes.
      // Mantemos os nomes como aliases, sem duplicar a lógica.
      case "createEscalaAction":
        response = createEscala(params);
        break;

      case "updateEscalaAction":
        response = updateEscala(params);
        break;

      case "updateFullEscalaAction":
        response = updateFullEscala(params);
        break;

      case "createSolicitacao":

        response =
          createSolicitacao(params);

        break;


      case "processaSolicitacao":

        response =
          processaSolicitacao(params);

        break;


      case "updateSolicitacao":

        response =
          updateSolicitacao(params);

        break;


      // ======================================================
      // TESTE
      // ======================================================

      case "testarNotificacoes":

        response =
          testarSistemaNotificacoesInterno(params);

        break;


      default:

        response = {

          sucesso: false,

          mensagem:
            "Ação '" +
            action +
            "' não implementada."

        };

        break;

    }


    // --------------------------------------------------------
    // GARANTIR RESPOSTA VÁLIDA
    // --------------------------------------------------------

    if (
      !response ||
      typeof response !== "object"
    ) {

      response = {

        sucesso: false,

        mensagem:
          "Resposta inválida do servidor."

      };

    }


    if (
      response.sucesso === undefined
    ) {

      response.sucesso = false;

    }


    if (
      response.mensagem === undefined ||
      response.mensagem === null
    ) {

      response.mensagem =
        response.sucesso
          ? "Operação concluída com sucesso."
          : "Não foi possível concluir a operação.";

    }

  } catch (error) {

    response = {

      sucesso: false,

      mensagem:
        "Erro no servidor: " +
        safeErrorMessage(error)

    };

  }


  return responseJSON(response);

}


// ============================================================
// LEITURA GERAL
// ============================================================

function getEscalaData(params) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var nome =
      safeString(params.nome)
        .trim();


    var senha =
      safeString(params.senha)
        .trim();


    // --------------------------------------------------------
    // INTEGRANTES
    // --------------------------------------------------------

    var integrantes =
      getSheetData(
        ss,
        ABA_INTEGRANTES,
        mapIntegrante
      );


    // --------------------------------------------------------
    // LOCALIZAR USUÁRIO
    // --------------------------------------------------------

    var usuario = null;


    for (
      var i = 0;
      i < integrantes.length;
      i++
    ) {

      if (

        normalizarTexto(
          integrantes[i].nome
        ) ===
        normalizarTexto(nome)

        &&

        safeString(
          integrantes[i].senha
        ).trim() === senha

      ) {

        usuario =
          integrantes[i];

        break;

      }

    }


    // --------------------------------------------------------
    // VERIFICAR LÍDER
    // --------------------------------------------------------

    var isLider =
      usuario &&
      normalizarTexto(
        usuario.funcao
      ).indexOf("lider") !== -1;


    // --------------------------------------------------------
    // SOLICITAÇÕES
    // --------------------------------------------------------

    var todasSolicitacoes =
      getSheetData(
        ss,
        ABA_SOLICITACOES,
        mapSolicitacao
      );


    var solicitacoes;


    if (isLider) {

      solicitacoes =
        todasSolicitacoes;

    } else {

      solicitacoes =
        todasSolicitacoes.filter(
          function(item) {

            return (

              normalizarTexto(
                item.quem_pediu
              ) ===
              normalizarTexto(nome)

            );

          }
        );

    }


    // --------------------------------------------------------
    // RETORNO
    // --------------------------------------------------------

    return {

      sucesso: true,

      escala:
        getSheetData(
          ss,
          ABA_ESCALA,
          mapEscala
        ),

      integrantes:
        integrantes,

      solicitacoes:
        solicitacoes,

      recados:
        getSheetData(
          ss,
          ABA_RECADOS,
          mapRecado
        ),

      // Detalhes dos louvores e respectivos vídeos do YouTube.
      link_louvores:
        getSheetData(
          ss,
          ABA_LINK_LOUVORES,
          mapLinkLouvor
        )

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao carregar dados: " +
        safeErrorMessage(error),

      escala: [],
      integrantes: [],
      solicitacoes: [],
      recados: [],
      link_louvores: []

    };

  }

}


// ============================================================
// LEITURA GENÉRICA
// ============================================================

function getSheetData(
  ss,
  sheetName,
  mapFunction
) {

  var sheet =
    getSheetSecure(
      ss,
      sheetName
    );


  if (!sheet) {
    return [];
  }


  var data =
    sheet
      .getDataRange()
      .getValues();


  if (
    !data ||
    data.length < 2
  ) {

    return [];

  }


  var rows =
    data.slice(1);


  if (mapFunction) {

    return rows
      .map(mapFunction)
      .filter(
        function(item) {

          return (
            item !== null &&
            item !== undefined
          );

        }
      );

  }


  return rows;

}


// ============================================================
// LOCALIZAR ABA DE FORMA ROBUSTA
// ============================================================

// Cache válido apenas durante a execução atual (uma chamada de
// doGet/doPost). Evita repetir ss.getSheets() -- que busca os
// metadados de TODAS as abas -- toda vez que uma função pede uma
// aba específica. É zerado no início de handleRequest().
var _sheetSecureCache = null;

function getSheetSecure(
  ss,
  name
) {

  if (!ss || !name) {
    return null;
  }


  if (!_sheetSecureCache) {
    _sheetSecureCache = {};
  }

  var alvo =
    normalizarTexto(name);

  var chaveCache =
    ss.getId() + "::" + alvo;

  if (
    Object.prototype.hasOwnProperty.call(_sheetSecureCache, chaveCache)
  ) {
    return _sheetSecureCache[chaveCache];
  }


  var sheets =
    ss.getSheets();


  var encontrada =
    null;


  for (
    var i = 0;
    i < sheets.length;
    i++
  ) {

    if (
      normalizarTexto(
        sheets[i].getName()
      ) === alvo
    ) {

      encontrada = sheets[i];
      break;

    }

  }


  _sheetSecureCache[chaveCache] = encontrada;

  return encontrada;

}


// ============================================================
// ESCALA
// ============================================================

function mapEscala(row) {

  return {

    data:
      formatDateBR(row[0]),

    dirigente:
      safeString(row[1]),

    vocal:
      safeString(row[2]),

    musicos:
      safeString(row[3]),

    mesario:
      safeString(row[4]),

    louvores:
      safeString(row[5]),

    uniforme:
      safeString(row[6])

  };

}


// ============================================================
// LOUVORES / YOUTUBE
// ============================================================
// Estrutura da aba LINK_LOUVORES:
// ID | DATA | ORDEM | LOUVOR | LINK_YOUTUBE
//
// Esta camada é independente da aba ESCALA. A coluna LOUVORES
// continua armazenando o resumo exibido na escala, enquanto
// LINK_LOUVORES guarda cada louvor individual e seu vídeo.

var ABA_LINK_LOUVORES = "LINK_LOUVORES";

function mapLinkLouvor(row) {
  return {
    id: safeString(row[0]),
    data: formatDateBR(row[1]),
    ordem: safeString(row[2]),
    louvor: safeString(row[3]),
    link_youtube: normalizarLinkYouTube(row[4])
  };
}

function obterAbaLinkLouvores(ss, criarSeNaoExistir) {
  var sheet = getSheetSecure(ss, ABA_LINK_LOUVORES);

  if (!sheet && criarSeNaoExistir) {
    sheet = ss.insertSheet(ABA_LINK_LOUVORES);
    sheet.getRange(1, 1, 1, 5).setValues([[
      "ID", "DATA", "ORDEM", "LOUVOR", "LINK_YOUTUBE"
    ]]);
  }

  return sheet;
}

function garantirEstruturaLinkLouvores(sheet) {
  if (!sheet) return false;

  var headers = sheet.getLastColumn() > 0
    ? sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0]
    : [];

  var obrigatorios = ["ID", "DATA", "ORDEM", "LOUVOR", "LINK_YOUTUBE"];

  for (var i = 0; i < obrigatorios.length; i++) {
    var esperado = normalizarTexto(obrigatorios[i]);
    var encontrado = false;

    for (var j = 0; j < headers.length; j++) {
      if (normalizarTexto(headers[j]) === esperado) {
        encontrado = true;
        break;
      }
    }

    if (!encontrado) {
      var novaColuna = sheet.getLastColumn() + 1;
      sheet.getRange(1, novaColuna).setValue(obrigatorios[i]);
      headers.push(obrigatorios[i]);
    }
  }

  return true;
}

function normalizarLinkYouTube(url) {
  var valor = safeString(url).trim();
  if (!valor) return "";

  valor = valor.replace(/[\\s<>"']/g, "");

  // Aceita links HTTP/HTTPS. Não executamos nem baixamos o conteúdo.
  if (!/^https?:\/\//i.test(valor)) return "";

  return valor;
}

function obterLinksLouvoresPorData(ss, dataEscala) {
  var sheet = obterAbaLinkLouvores(ss, false);
  if (!sheet || sheet.getLastRow() <= 1) return [];

  var dados = sheet.getDataRange().getValues();
  var resultado = [];

  for (var i = 1; i < dados.length; i++) {
    if (!sameDate(dados[i][1], dataEscala)) continue;
    resultado.push(mapLinkLouvor(dados[i]));
  }

  resultado.sort(function(a, b) {
    var oa = parseInt(a.ordem, 10);
    var ob = parseInt(b.ordem, 10);
    oa = isNaN(oa) ? 999999 : oa;
    ob = isNaN(ob) ? 999999 : ob;
    return oa - ob;
  });

  return resultado;
}

/**
 * Sincroniza os detalhes dos louvores com a aba LINK_LOUVORES.
 *
 * O aplicativo pode enviar:
 * [
 *   {
 *     ordem: 1,
 *     louvor: "Nome do louvor",
 *     link_youtube: "https://..."
 *   }
 * ]
 *
 * Os registros da data são substituídos pelo estado atual enviado.
 * Isso evita que links antigos permaneçam associados à escala.
 */
function syncLinkLouvores(ss, dataEscala, louvoresJson) {
  if (!ss) {
    return { sucesso: false, mensagem: "Planilha não informada." };
  }

  var sheet = obterAbaLinkLouvores(ss, true);
  garantirEstruturaLinkLouvores(sheet);

  var detalhes;
  try {
    detalhes = typeof louvoresJson === "string"
      ? JSON.parse(louvoresJson)
      : louvoresJson;
  } catch (error) {
    return {
      sucesso: false,
      mensagem: "Os detalhes dos louvores não estão em um JSON válido."
    };
  }

  if (!Array.isArray(detalhes)) {
    return {
      sucesso: false,
      mensagem: "Os detalhes dos louvores devem ser uma lista."
    };
  }

  var dataNormalizada = safeString(dataEscala).trim();
  if (!dataNormalizada) {
    return {
      sucesso: false,
      mensagem: "Data da escala não informada para sincronizar os louvores."
    };
  }

  // Em vez de deleteRow() dentro de um loop (cada chamada reindexa
  // a planilha), filtramos em memória as linhas que NÃO são desta
  // data e depois regravamos tudo de uma vez.
  var values = sheet.getDataRange().getValues();
  var linhasMantidas = [];
  for (var i = 1; i < values.length; i++) {
    if (!sameDate(values[i][1], dataNormalizada)) {
      linhasMantidas.push(values[i]);
    }
  }

  var rows = [];
  for (var j = 0; j < detalhes.length; j++) {
    var item = detalhes[j] || {};
    var louvor = safeString(
      item.louvor || item.nome || item.titulo
    ).trim();

    if (!louvor) continue;

    var ordem = item.ordem !== undefined && item.ordem !== null
      ? safeString(item.ordem).trim()
      : String(j + 1);

    rows.push([
      safeString(item.id).trim() || Utilities.getUuid(),
      dataNormalizada,
      ordem,
      louvor,
      normalizarLinkYouTube(
        item.link_youtube || item.linkYoutube || item.youtube
      )
    ]);
  }

  var totalFinal = linhasMantidas.concat(rows);

  // Limpa o bloco de dados atual e regrava tudo (mantidas + novas)
  // em uma única chamada, em vez de N deletes + 1 append.
  if (values.length > 1) {
    sheet.getRange(2, 1, values.length - 1, 5).clearContent();
  }
  if (totalFinal.length > 0) {
    sheet.getRange(2, 1, totalFinal.length, 5).setValues(totalFinal);
  }

  return {
    sucesso: true,
    mensagem: "Links dos louvores sincronizados com sucesso.",
    quantidade: rows.length
  };
}

// ============================================================
// INTEGRANTE
// ============================================================

function mapIntegrante(row) {

  return {

    nome:
      safeString(row[0]),

    funcao:
      safeString(row[1]),

    senha:
      safeString(row[2]),

    instrumento:
      safeString(row[3])

  };

}


// ============================================================
// SOLICITAÇÃO
// ============================================================

function mapSolicitacao(row) {

  // Estrutura completa:
  //
  // 0 ID
  // 1 DATA
  // 2 QUEM PEDIU
  // 3 FUNÇÃO
  // 4 INSTRUMENTO
  // 5 SUBSTITUTO
  // 6 MOTIVO
  // 7 STATUS
  // 8 DATA DECISÃO
  // 9 DECIDIDO POR
  // 10 MOTIVO DECISÃO
  // 11 OBSERVAÇÃO


  if (row.length >= 8) {

    return {

      id:
        safeString(row[0]),

      data_escala:
        formatDateBR(row[1]),

      quem_pediu:
        safeString(row[2]),

      funcao:
        safeString(row[3]),

      instrumento:
        safeString(row[4]),

      substituto:
        safeString(row[5]),

      motivo:
        safeString(row[6]),

      status:
        safeString(row[7]),

      data_decisao:
        formatDateTimeBR(row[8]),

      decidido_por:
        safeString(row[9]),

      motivo_decisao:
        safeString(row[10])

    };

  }


  // Estrutura antiga:
  //
  // 0 DATA
  // 1 QUEM PEDIU
  // 2 SUBSTITUTO
  // 3 MOTIVO
  // 4 STATUS

  return {

    id: "",

    data_escala:
      formatDateBR(row[0]),

    quem_pediu:
      safeString(row[1]),

    funcao: "",

    instrumento: "",

    substituto:
      safeString(row[2]),

    motivo:
      safeString(row[3]),

    status:
      safeString(row[4]),

    data_decisao: "",

    decidido_por: "",

    motivo_decisao: ""

  };

}


// ============================================================
// RECADO
// ============================================================

function mapRecado(row) {

  return {

    id:
      safeString(row[0]),

    titulo:
      safeString(row[1]),

    mensagem:
      safeString(row[2]),

    imagem_url:
      safeString(row[3]),

    ativo:
      safeString(row[4]),

    data_criacao:
      formatDateTimeBR(row[5]),

    data_atualizacao:
      formatDateTimeBR(row[6])

  };

}


// ============================================================
// CRIAR ESCALA
// ============================================================

function createEscala(p) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_ESCALA
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba ESCALA não encontrada."

      };

    }


    var data =
      safeString(p.data)
        .trim();


    if (!data) {

      return {

        sucesso: false,

        mensagem:
          "Data da escala não informada."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (
        sameDate(
          values[i][0],
          data
        )
      ) {

        return {

          sucesso: false,

          mensagem:
            "Já existe uma escala cadastrada para esta data."

        };

      }

    }


    sheet.appendRow([

      data,

      safeString(p.dirigente),

      safeString(p.vocal),

      safeString(p.musicos),

      safeString(p.mesario),

      safeString(p.louvores),

      safeString(p.uniforme)

    ]);


    // Sincroniza os detalhes dos louvores somente quando enviados pelo Android.
    if (p.louvores_detalhes || p.louvoresDetalhes) {
      var syncResultado = syncLinkLouvores(
        ss,
        data,
        p.louvores_detalhes || p.louvoresDetalhes
      );

      if (!syncResultado.sucesso) {
        return syncResultado;
      }
    }

    criarNotificacaoParaTodos(

      ss,

      "Nova escala",

      "Uma nova escala foi cadastrada para " +
      data +
      ".",

      "ESCALA"

    );


    return {

      sucesso: true,

      mensagem:
        "Escala criada com sucesso."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao criar escala: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// ATUALIZAR CAMPO DA ESCALA
// ============================================================

function updateEscala(p) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_ESCALA
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba ESCALA não encontrada."

      };

    }


    var campo =
      safeString(p.campo)
        .trim()
        .toLowerCase();


    var allowedFields = {

      dirigente: 2,
      vocal: 3,
      musicos: 4,
      mesario: 5,
      louvores: 6,
      uniforme: 7

    };


    if (
      !allowedFields[campo]
    ) {

      return {

        sucesso: false,

        mensagem:
          "Campo de escala não permitido."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (
        sameDate(
          values[i][0],
          p.data
        )
      ) {

        sheet
          .getRange(
            i + 1,
            allowedFields[campo]
          )
          .setValue(
            safeString(p.valor)
          );

        if (campo === "louvores" && (p.louvores_detalhes || p.louvoresDetalhes)) {
          var syncResultado = syncLinkLouvores(
            ss,
            p.data,
            p.louvores_detalhes || p.louvoresDetalhes
          );

          if (!syncResultado.sucesso) return syncResultado;
        }



        criarNotificacaoParaTodos(

          ss,

          "Escala atualizada",

          "A escala de " +
          safeString(p.data) +
          " foi atualizada.",

          "ESCALA"

        );


        return {

          sucesso: true,

          mensagem:
            "Escala atualizada com sucesso."

        };

      }

    }


    return {

      sucesso: false,

      mensagem:
        "Escala não localizada para a data informada."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao atualizar escala: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// ATUALIZAR ESCALA COMPLETA
// ============================================================

function updateFullEscala(p) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_ESCALA
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba ESCALA não encontrada."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (
        sameDate(
          values[i][0],
          p.data
        )
      ) {

        sheet
          .getRange(
            i + 1,
            2,
            1,
            6
          )
          .setValues([[

            safeString(p.dirigente),

            safeString(p.vocal),

            safeString(p.musicos),

            safeString(p.mesario),

            safeString(p.louvores),

            safeString(p.uniforme)

          ]]);


        if (p.louvores_detalhes || p.louvoresDetalhes) {
          var syncResultado = syncLinkLouvores(
            ss,
            p.data,
            p.louvores_detalhes || p.louvoresDetalhes
          );

          if (!syncResultado.sucesso) {
            return syncResultado;
          }
        }

        criarNotificacaoParaTodos(

          ss,

          "Escala atualizada",

          "A escala de " +
          safeString(p.data) +
          " foi atualizada.",

          "ESCALA"

        );


        return {

          sucesso: true,

          mensagem:
            "Escala atualizada com sucesso."

        };

      }

    }


    return {

      sucesso: false,

      mensagem:
        "Escala não localizada para a data informada."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao atualizar escala: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// CRIAR SOLICITAÇÃO
// ============================================================

function createSolicitacao(p) {

  var lock =
    LockService.getScriptLock();


  try {

    lock.waitLock(10000);


    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_SOLICITACOES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba SOLICITAÇÕES não encontrada."

      };

    }


    var nome =
      safeString(p.nome)
        .trim();


    var dataEscala =
      safeString(
        p.dataEscala ||
        p.data_escala
      ).trim();


    var substituto =
      safeString(p.substituto)
        .trim();


    var motivo =
      safeString(p.motivo)
        .trim();


    if (!nome) {

      return {

        sucesso: false,

        mensagem:
          "Solicitante não informado."

      };

    }


    if (!dataEscala) {

      return {

        sucesso: false,

        mensagem:
          "Data da escala não informada."

      };

    }


    if (!substituto) {

      return {

        sucesso: false,

        mensagem:
          "Informe quem irá substituir."

      };

    }


    if (!motivo) {

      return {

        sucesso: false,

        mensagem:
          "Informe o motivo da solicitação."

      };

    }


    if (
      normalizarTexto(nome) ===
      normalizarTexto(substituto)
    ) {

      return {

        sucesso: false,

        mensagem:
          "Você não pode escolher a si mesmo como substituto."

      };

    }


    // --------------------------------------------------------
    // ESCALA
    // --------------------------------------------------------

    var sheetEscala =
      getSheetSecure(
        ss,
        ABA_ESCALA
      );


    if (!sheetEscala) {

      return {

        sucesso: false,

        mensagem:
          "Aba ESCALA não encontrada."

      };

    }


    var escalas =
      sheetEscala
        .getDataRange()
        .getValues();


    var escalaEncontrada =
      null;


    for (
      var i = 1;
      i < escalas.length;
      i++
    ) {

      if (
        sameDate(
          escalas[i][0],
          dataEscala
        )
      ) {

        escalaEncontrada =
          escalas[i];

        break;

      }

    }


    if (!escalaEncontrada) {

      return {

        sucesso: false,

        mensagem:
          "Não foi encontrada escala para a data informada."

      };

    }


    // --------------------------------------------------------
    // VERIFICAR SOLICITANTE
    // --------------------------------------------------------

    var colunasFuncoes = [
      1,
      2,
      3,
      4
    ];


    var solicitanteEncontrado =
      false;


    for (
      var c = 0;
      c < colunasFuncoes.length;
      c++
    ) {

      var valor =
        safeString(
          escalaEncontrada[
            colunasFuncoes[c]
          ]
        );


      if (
        contemNome(
          valor,
          nome
        )
      ) {

        solicitanteEncontrado =
          true;

        break;

      }

    }


    if (!solicitanteEncontrado) {

      return {

        sucesso: false,

        mensagem:
          "O solicitante não está escalado nesta data."

      };

    }


    // --------------------------------------------------------
    // SUBSTITUTO JÁ ESCALADO?
    // --------------------------------------------------------

    for (
      var x = 0;
      x < colunasFuncoes.length;
      x++
    ) {

      var pessoas =
        safeString(
          escalaEncontrada[
            colunasFuncoes[x]
          ]
        );


      if (
        contemNome(
          pessoas,
          substituto
        )
      ) {

        return {

          sucesso: false,

          mensagem:
            "O substituto informado já está escalado nesta data."

        };

      }

    }


    // --------------------------------------------------------
    // SOLICITAÇÃO PENDENTE
    // --------------------------------------------------------

    var solicitacoes =
      sheet
        .getDataRange()
        .getValues();


    for (
      var s = 1;
      s < solicitacoes.length;
      s++
    ) {

      var status =
        obterStatusSolicitacao(
          solicitacoes[s]
        );


      var pessoa =
        obterQuemPediu(
          solicitacoes[s]
        );


      var dataExistente =
        obterDataSolicitacao(
          solicitacoes[s]
        );


      if (

        sameDate(
          dataExistente,
          dataEscala
        )

        &&

        normalizarTexto(pessoa) ===
        normalizarTexto(nome)

        &&

        status === "PENDENTE"

      ) {

        return {

          sucesso: false,

          mensagem:
            "Já existe uma solicitação de troca pendente para esta escala."

        };

      }

    }


    // --------------------------------------------------------
    // DETECTAR ESTRUTURA
    // --------------------------------------------------------

    var ultimaColuna =
      sheet.getLastColumn();


    var idSolicitacao =
      Utilities.getUuid();


    if (ultimaColuna >= 8) {

      sheet.appendRow([

        idSolicitacao,

        dataEscala,

        nome,

        safeString(p.funcao),

        safeString(p.instrumento),

        substituto,

        motivo,

        "PENDENTE",

        new Date(),

        "",

        "",

        ""

      ]);

    } else {

      sheet.appendRow([

        dataEscala,

        nome,

        substituto,

        motivo,

        "PENDENTE"

      ]);

    }


    // --------------------------------------------------------
    // NOTIFICAR LÍDERES
    // --------------------------------------------------------

    criarNotificacaoParaLideres(

      ss,

      "Nova solicitação de troca",

      nome +
      " solicitou uma troca para " +
      dataEscala +
      ".",

      "SOLICITACAO"

    );


    return {

      sucesso: true,

      mensagem:
        "Solicitação de troca enviada com sucesso."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao criar solicitação: " +
        safeErrorMessage(error)

    };

  } finally {

    try {
      lock.releaseLock();
    } catch (e) {}

  }

}


// ============================================================
// PROCESSAR SOLICITAÇÃO
// ============================================================

function processaSolicitacao(p) {

  var lock =
    LockService.getScriptLock();


  try {

    lock.waitLock(10000);


    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_SOLICITACOES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba SOLICITAÇÕES não encontrada."

      };

    }


    var acao =
      safeString(
        p.acao ||
        p.novaAcao
      )
        .trim()
        .toUpperCase();


    var nomeUsuario =
      safeString(p.nome)
        .trim();


    var dataEscala =
      safeString(
        p.dataEscala ||
        p.data_escala
      ).trim();


    var quemPediu =
      safeString(
        p.quemPediu ||
        p.quem_pediu
      ).trim();


    var substituto =
      safeString(p.substituto)
        .trim();


    var motivoDecisao =
      safeString(
        p.motivoDecisao ||
        p.motivo_decisao
      ).trim();


    var idRecebido =
      safeString(p.id)
        .trim();


    if (!acao) {

      return {

        sucesso: false,

        mensagem:
          "Ação não informada."

      };

    }


    if (!nomeUsuario) {

      return {

        sucesso: false,

        mensagem:
          "Usuário não informado."

      };

    }


    // --------------------------------------------------------
    // LOCALIZAR
    // --------------------------------------------------------

    var values =
      sheet
        .getDataRange()
        .getValues();


    var rowIndex =
      -1;


    var solicitacao =
      null;


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      var linha =
        values[i];


      var idLinha =
        obterIdSolicitacao(
          linha
        );


      var dataLinha =
        obterDataSolicitacao(
          linha
        );


      var pessoaLinha =
        obterQuemPediu(
          linha
        );


      var substitutoLinha =
        obterSubstituto(
          linha
        );


      var statusLinha =
        obterStatusSolicitacao(
          linha
        );


      var encontrou =
        false;


      if (
        idRecebido &&
        idLinha &&
        compararId(
          idLinha,
          idRecebido
        )
      ) {

        encontrou = true;

      }


      if (
        !encontrou &&
        dataEscala &&
        quemPediu &&
        substituto
      ) {

        if (

          sameDate(
            dataLinha,
            dataEscala
          )

          &&

          normalizarTexto(pessoaLinha) ===
          normalizarTexto(quemPediu)

          &&

          normalizarTexto(substitutoLinha) ===
          normalizarTexto(substituto)

        ) {

          encontrou = true;

        }

      }


      if (
        encontrou &&
        statusLinha === "PENDENTE"
      ) {

        rowIndex =
          i + 1;


        solicitacao = {

          id:
            idLinha,

          dataEscala:
            formatDateBR(dataLinha),

          quemPediu:
            pessoaLinha,

          funcao:
            obterFuncaoSolicitacao(
              linha
            ),

          instrumento:
            obterInstrumentoSolicitacao(
              linha
            ),

          substituto:
            substitutoLinha,

          motivo:
            obterMotivoSolicitacao(
              linha
            ),

          status:
            statusLinha

        };


        break;

      }

    }


    if (!solicitacao) {

      return {

        sucesso: false,

        mensagem:
          "Solicitação pendente não encontrada."

      };

    }


    // --------------------------------------------------------
    // APROVAR / RECUSAR
    // SOMENTE LÍDER
    // --------------------------------------------------------

    if (
      acao === "APROVAR" ||
      acao === "RECUSAR"
    ) {

      var integrantes =
        getSheetData(
          ss,
          ABA_INTEGRANTES,
          mapIntegrante
        );


      var ehLider =
        false;


      for (
        var j = 0;
        j < integrantes.length;
        j++
      ) {

        if (

          normalizarTexto(
            integrantes[j].nome
          ) ===
          normalizarTexto(nomeUsuario)

          &&

          normalizarTexto(
            integrantes[j].funcao
          ).indexOf("lider") !== -1

        ) {

          ehLider = true;

          break;

        }

      }


      if (!ehLider) {

        return {

          sucesso: false,

          mensagem:
            "Somente o Líder pode realizar esta ação."

        };

      }

    }


    // --------------------------------------------------------
    // APROVAR
    // --------------------------------------------------------

    if (
      acao === "APROVAR"
    ) {

      var resultado =
        aplicarTrocaNaEscala(
          ss,
          solicitacao
        );


      if (
        !resultado.sucesso
      ) {

        return resultado;

      }


      atualizarStatusSolicitacao(

        sheet,

        rowIndex,

        "APROVADA",

        nomeUsuario,

        motivoDecisao

      );


      criarNotificacao(

        ss,

        solicitacao.quemPediu,

        "Troca aprovada",

        "Sua solicitação de troca para " +
        solicitacao.dataEscala +
        " foi aprovada.",

        "SOLICITACAO"

      );


      return {

        sucesso: true,

        mensagem:
          "Solicitação aprovada e escala atualizada com sucesso."

      };

    }


    // --------------------------------------------------------
    // RECUSAR
    // --------------------------------------------------------

    if (
      acao === "RECUSAR"
    ) {

      atualizarStatusSolicitacao(

        sheet,

        rowIndex,

        "RECUSADA",

        nomeUsuario,

        motivoDecisao

      );


      criarNotificacao(

        ss,

        solicitacao.quemPediu,

        "Troca recusada",

        "Sua solicitação de troca para " +
        solicitacao.dataEscala +
        " foi recusada." +

        (

          motivoDecisao
            ? " Motivo: " +
              motivoDecisao
            : ""

        ),

        "SOLICITACAO"

      );


      return {

        sucesso: true,

        mensagem:
          "Solicitação recusada com sucesso."

      };

    }


    // --------------------------------------------------------
    // CANCELAR
    // --------------------------------------------------------

    if (
      acao === "CANCELAR"
    ) {

      if (

        normalizarTexto(
          solicitacao.quemPediu
        ) !==

        normalizarTexto(
          nomeUsuario
        )

      ) {

        return {

          sucesso: false,

          mensagem:
            "Somente o solicitante pode cancelar a solicitação."

        };

      }


      atualizarStatusSolicitacao(

        sheet,

        rowIndex,

        "CANCELADA",

        nomeUsuario,

        motivoDecisao ||
        "Cancelado pelo usuário"

      );


      criarNotificacaoParaLideres(

        ss,

        "Solicitação cancelada",

        solicitacao.quemPediu +
        " cancelou a solicitação para " +
        solicitacao.dataEscala +
        ".",

        "SOLICITACAO"

      );


      return {

        sucesso: true,

        mensagem:
          "Solicitação cancelada com sucesso."

      };

    }


    return {

      sucesso: false,

      mensagem:
        "Ação inválida: " +
        acao

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao processar solicitação: " +
        safeErrorMessage(error)

    };

  } finally {

    try {
      lock.releaseLock();
    } catch (e) {}

  }

}


// ============================================================
// APLICAR TROCA NA ESCALA
// ============================================================

function aplicarTrocaNaEscala(
  ss,
  solicitacao
) {

  var sheet =
    getSheetSecure(
      ss,
      ABA_ESCALA
    );


  if (!sheet) {

    return {

      sucesso: false,

      mensagem:
        "Aba ESCALA não encontrada."

    };

  }


  var values =
    sheet
      .getDataRange()
      .getValues();


  for (
    var i = 1;
    i < values.length;
    i++
  ) {

    if (
      !sameDate(
        values[i][0],
        solicitacao.dataEscala
      )
    ) {

      continue;

    }


    var row =
      values[i];


    var colunas = [

      {
        indice: 1,
        nome: "Dirigente"
      },

      {
        indice: 2,
        nome: "Vocal"
      },

      {
        indice: 3,
        nome: "Músicos"
      },

      {
        indice: 4,
        nome: "Mesário"
      }

    ];


    // --------------------------------------------------------
    // SUBSTITUTO JÁ ESCALADO
    // --------------------------------------------------------

    for (
      var v = 0;
      v < colunas.length;
      v++
    ) {

      var pessoas =
        safeString(
          row[
            colunas[v].indice
          ]
        );


      if (
        contemNome(
          pessoas,
          solicitacao.substituto
        )
      ) {

        return {

          sucesso: false,

          mensagem:
            "Não foi possível aprovar. O substituto '" +
            solicitacao.substituto +
            "' já está escalado nesta data."

        };

      }

    }


    // --------------------------------------------------------
    // DETERMINAR FUNÇÃO
    // --------------------------------------------------------

    var campoAfetado =
      -1;


    var nomeFuncao =
      "";


    var funcao =
      normalizarTexto(
        solicitacao.funcao
      );


    if (
      funcao.indexOf("dirigente") !== -1
    ) {

      campoAfetado = 1;
      nomeFuncao = "Dirigente";

    }

    else if (
      funcao.indexOf("vocal") !== -1
    ) {

      campoAfetado = 2;
      nomeFuncao = "Vocal";

    }

    else if (
      funcao.indexOf("music") !== -1
    ) {

      campoAfetado = 3;
      nomeFuncao = "Músicos";

    }

    else if (
      funcao.indexOf("mesar") !== -1
    ) {

      campoAfetado = 4;
      nomeFuncao = "Mesário";

    }


    // --------------------------------------------------------
    // SE FUNÇÃO NÃO FOI IDENTIFICADA,
    // PROCURAR PELO NOME
    // --------------------------------------------------------

    if (
      campoAfetado === -1
    ) {

      for (
        var c = 0;
        c < colunas.length;
        c++
      ) {

        var valor =
          safeString(
            row[
              colunas[c].indice
            ]
          );


        if (
          contemNome(
            valor,
            solicitacao.quemPediu
          )
        ) {

          campoAfetado =
            colunas[c].indice;

          nomeFuncao =
            colunas[c].nome;

          break;

        }

      }

    }


    if (
      campoAfetado === -1
    ) {

      return {

        sucesso: false,

        mensagem:
          "O solicitante '" +
          solicitacao.quemPediu +
          "' não foi encontrado na escala desta data."

      };

    }


    var valorAtual =
      safeString(
        row[campoAfetado]
      );


    var novoValor =
      substituirNome(
        valorAtual,
        solicitacao.quemPediu,
        solicitacao.substituto
      );


    if (
      novoValor === valorAtual
    ) {

      return {

        sucesso: false,

        mensagem:
          "Não foi possível realizar a substituição."

      };

    }


    sheet
      .getRange(
        i + 1,
        campoAfetado + 1
      )
      .setValue(
        novoValor
      );


    return {

      sucesso: true,

      mensagem:
        "Troca realizada na função '" +
        nomeFuncao +
        "'."

    };

  }


  return {

    sucesso: false,

    mensagem:
      "Escala não encontrada para a data da solicitação."

  };

}


// ============================================================
// ATUALIZAR STATUS DA SOLICITAÇÃO
// ============================================================

function atualizarStatusSolicitacao(
  sheet,
  rowIndex,
  status,
  decididoPor,
  motivo
) {

  var lastColumn =
    sheet.getLastColumn();


  // Estrutura completa: monta um único array de valores para as
  // colunas 8-11 (conforme disponíveis) e grava tudo em 1 chamada,
  // em vez de até 4 chamadas setValue separadas.
  if (
    lastColumn >= 8
  ) {

    var linhaValores = [status, new Date()];

    if (lastColumn >= 10) {
      linhaValores.push(decididoPor);
    }

    if (lastColumn >= 11) {
      linhaValores.push(motivo);
    }

    sheet
      .getRange(rowIndex, 8, 1, linhaValores.length)
      .setValues([linhaValores]);

    return;

  }


  // Estrutura antiga
  sheet
    .getRange(
      rowIndex,
      5
    )
    .setValue(
      status
    );

}


// ============================================================
// ATUALIZAR SOLICITAÇÃO
// ============================================================

function updateSolicitacao(p) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_SOLICITACOES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba SOLICITAÇÕES não encontrada."

      };

    }


    var data =
      safeString(
        p.dataEscala ||
        p.data_escala
      ).trim();


    var quem =
      safeString(
        p.quemPediu ||
        p.quem_pediu
      ).trim();


    var status =
      safeString(p.status)
        .trim()
        .toUpperCase();


    if (!status) {

      return {

        sucesso: false,

        mensagem:
          "Status não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (

        sameDate(
          obterDataSolicitacao(
            values[i]
          ),
          data
        )

        &&

        normalizarTexto(
          obterQuemPediu(
            values[i]
          )
        ) ===
        normalizarTexto(quem)

      ) {

        atualizarStatusSolicitacao(

          sheet,

          i + 1,

          status,

          safeString(p.nome),

          safeString(
            p.motivoDecisao ||
            p.motivo_decisao
          )

        );


        return {

          sucesso: true,

          mensagem:
            "Solicitação atualizada com sucesso."

        };

      }

    }


    return {

      sucesso: false,

      mensagem:
        "Solicitação não encontrada."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao atualizar solicitação: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// CRIAR RECADO
// ============================================================

function createRecado(p) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_RECADOS
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba RECADOS não encontrada."

      };

    }


    var titulo =
      safeString(p.titulo)
        .trim();


    var mensagem =
      safeString(p.mensagem)
        .trim();


    if (
      !titulo &&
      !mensagem
    ) {

      return {

        sucesso: false,

        mensagem:
          "Informe o título ou a mensagem."

      };

    }


    var imagem =
      "";


    var imageBase64 =
      safeString(
        p.imageBase64 ||
        p.imagemBase64
      );


    if (
      imageBase64
    ) {

      imagem =
        saveImageToDrive(
          imageBase64
        );

    }

    else if (
      p.imagemUrl !== undefined &&
      p.imagemUrl !== null
    ) {

      imagem =
        safeString(
          p.imagemUrl
        );

    }


    var now =
      new Date();


    sheet.appendRow([

      Utilities.getUuid(),

      titulo,

      mensagem,

      imagem,

      "SIM",

      now,

      now

    ]);


    criarNotificacaoParaTodos(

      ss,

      titulo ||
      "Novo recado",

      mensagem ||
      "Um novo recado foi publicado.",

      "RECADO"

    );


    return {

      sucesso: true,

      mensagem:
        "Recado publicado com sucesso."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao criar recado: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// ATUALIZAR RECADO
// ============================================================

function updateRecado(p) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_RECADOS
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba RECADOS não encontrada."

      };

    }


    var id =
      safeString(p.id)
        .trim();


    if (!id) {

      return {

        sucesso: false,

        mensagem:
          "ID do recado não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (
        compararId(
          values[i][0],
          id
        )
      ) {

        var imagem =
          safeString(
            values[i][3]
          );


        var imageBase64 =
          safeString(
            p.imageBase64 ||
            p.imagemBase64
          );


        if (
          imageBase64
        ) {

          imagem =
            saveImageToDrive(
              imageBase64
            );

        }

        else if (
          p.imagemUrl !== undefined &&
          p.imagemUrl !== null
        ) {

          imagem =
            safeString(
              p.imagemUrl
            );

        }


        var ativo =
          safeString(
            values[i][4]
          );


        if (
          p.ativo !== undefined &&
          p.ativo !== null
        ) {

          ativo =
            safeString(
              p.ativo
            );

        }


        // Uma única escrita cobrindo as colunas 2-7 (título,
        // mensagem, imagem, ativo, data_criação inalterada e
        // data_atualização), em vez de 2 chamadas separadas.
        sheet
          .getRange(
            i + 1,
            2,
            1,
            6
          )
          .setValues([[

            safeString(p.titulo),

            safeString(p.mensagem),

            imagem,

            ativo,

            values[i][5],

            new Date()

          ]]);


        criarNotificacaoParaTodos(

          ss,

          "Recado atualizado",

          safeString(p.titulo) ||
          "Um recado foi atualizado.",

          "RECADO"

        );


        return {

          sucesso: true,

          mensagem:
            "Recado atualizado com sucesso."

        };

      }

    }


    return {

      sucesso: false,

      mensagem:
        "Recado não encontrado."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao atualizar recado: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// EXCLUIR RECADO
// ============================================================

function deleteRecado(p) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_RECADOS
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba RECADOS não encontrada."

      };

    }


    var id =
      safeString(p.id)
        .trim();


    if (!id) {

      return {

        sucesso: false,

        mensagem:
          "ID do recado não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (
        compararId(
          values[i][0],
          id
        )
      ) {

        sheet.deleteRow(
          i + 1
        );


        return {

          sucesso: true,

          mensagem:
            "Recado excluído com sucesso."

        };

      }

    }


    return {

      sucesso: false,

      mensagem:
        "Recado não encontrado."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao excluir recado: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// ============================================================
// NOTIFICAÇÕES
// ============================================================
// ============================================================


// ============================================================
// CABEÇALHO PADRÃO
// ============================================================

function cabecalhoNotificacoes() {

  return [

    "ID",
    "DESTINATARIO",
    "TITULO",
    "MENSAGEM",
    "TIPO",
    "DATA",
    "LIDA"

  ];

}


// ============================================================
// OBTER/CRIAR ABA
// ============================================================

function obterAbaNotificacoes(ss) {

  var sheet =
    getSheetSecure(
      ss,
      ABA_NOTIFICACOES
    );


  if (!sheet) {

    sheet =
      ss.insertSheet(
        ABA_NOTIFICACOES
      );

  }


  if (
    sheet.getLastRow() === 0
  ) {

    sheet
      .getRange(
        1,
        1,
        1,
        7
      )
      .setValues([
        cabecalhoNotificacoes()
      ]);

  }


  garantirEstruturaNotificacoes(
    sheet
  );


  return sheet;

}


// ============================================================
// NORMALIZAR CABEÇALHOS
// ============================================================

function obterCabecalhosNotificacao(
  sheet
) {

  var lastColumn =
    sheet.getLastColumn();


  if (
    lastColumn <= 0
  ) {

    return [];

  }


  var headers =
    sheet
      .getRange(
        1,
        1,
        1,
        lastColumn
      )
      .getValues()[0];


  return headers.map(
    function(header) {

      return normalizarTexto(
        header
      ).replace(
        /\s+/g,
        "_"
      );

    }
  );

}


// ============================================================
// GARANTIR ESTRUTURA
// ============================================================

function garantirEstruturaNotificacoes(
  sheet
) {

  var obrigatorios = [

    "id",
    "destinatario",
    "titulo",
    "mensagem",
    "tipo",
    "data",
    "lida"

  ];


  var headers =
    obterCabecalhosNotificacao(
      sheet
    );


  for (
    var i = 0;
    i < obrigatorios.length;
    i++
  ) {

    if (
      headers.indexOf(
        obrigatorios[i]
      ) === -1
    ) {

      var novaColuna =
        sheet.getLastColumn() + 1;


      sheet
        .getRange(
          1,
          novaColuna
        )
        .setValue(
          obrigatorios[i].toUpperCase()
        );


      headers =
        obterCabecalhosNotificacao(
          sheet
        );

    }

  }

}


// ============================================================
// LOCALIZAR COLUNA
// ============================================================

function colunaNotificacao(
  headers,
  nome
) {

  var indice =
    headers.indexOf(
      nome
    );


  return (
    indice >= 0
      ? indice + 1
      : -1
  );

}


// ============================================================
// CRIAR NOTIFICAÇÃO
// ============================================================

function criarNotificacao(
  ss,
  destinatario,
  titulo,
  mensagem,
  tipo
) {

  var lock =
    LockService.getScriptLock();


  try {

    lock.waitLock(10000);


    destinatario =
      safeString(
        destinatario
      ).trim();


    titulo =
      safeString(
        titulo
      ).trim();


    mensagem =
      safeString(
        mensagem
      ).trim();


    tipo =
      safeString(
        tipo
      ).trim();


    if (!destinatario) {
      return false;
    }


    var sheet =
      obterAbaNotificacoes(
        ss
      );


    garantirEstruturaNotificacoes(
      sheet
    );


    var headers =
      obterCabecalhosNotificacao(
        sheet
      );


    // --------------------------------------------------------
    // EVITAR DUPLICAÇÃO RECENTE
    // --------------------------------------------------------

    if (
      notificacaoDuplicada(
        sheet,
        destinatario,
        titulo,
        mensagem
      )
    ) {

      return false;

    }


    var row =
      new Array(
        sheet.getLastColumn()
      );


    for (
      var i = 0;
      i < row.length;
      i++
    ) {

      row[i] = "";

    }


    definirValorColuna(
      row,
      headers,
      "id",
      Utilities.getUuid()
    );


    definirValorColuna(
      row,
      headers,
      "destinatario",
      destinatario
    );


    definirValorColuna(
      row,
      headers,
      "titulo",
      titulo
    );


    definirValorColuna(
      row,
      headers,
      "mensagem",
      mensagem
    );


    definirValorColuna(
      row,
      headers,
      "tipo",
      tipo
    );


    definirValorColuna(
      row,
      headers,
      "data",
      new Date()
    );


    definirValorColuna(
      row,
      headers,
      "lida",
      "NAO"
    );


    sheet.appendRow(
      row
    );


    // Envia o push depois de gravar na planilha — se falhar, a
    // notificação já está registrada normalmente e o polling do
    // app (WorkManager) continua funcionando como plano B.
    try {

      var tokenDestinatario =
        buscarTokenFcmPorNome_(ss, destinatario);

      if (tokenDestinatario) {
        enviarPushFCM(tokenDestinatario, titulo, mensagem);
      }

    } catch (pushError) {

      Logger.log(
        "Erro ao enviar push individual: " +
        safeErrorMessage(pushError)
      );

    }


    return true;


  } catch (error) {

    Logger.log(
      "Erro ao criar notificação: " +
      safeErrorMessage(error)
    );


    return false;


  } finally {

    try {
      lock.releaseLock();
    } catch (e) {}

  }

}


// ============================================================
// DEFINIR VALOR
// ============================================================

function definirValorColuna(
  row,
  headers,
  coluna,
  valor
) {

  var index =
    headers.indexOf(
      coluna
    );


  if (
    index >= 0
  ) {

    row[index] =
      valor;

  }

}


// ============================================================
// VERIFICAR DUPLICAÇÃO
// ============================================================

function notificacaoDuplicada(
  sheet,
  destinatario,
  titulo,
  mensagem
) {

  if (
    sheet.getLastRow() <= 1
  ) {

    return false;

  }


  var headers =
    obterCabecalhosNotificacao(
      sheet
    );


  var colDest =
    headers.indexOf(
      "destinatario"
    );


  var colTitulo =
    headers.indexOf(
      "titulo"
    );


  var colMensagem =
    headers.indexOf(
      "mensagem"
    );


  if (
    colDest < 0 ||
    colTitulo < 0 ||
    colMensagem < 0
  ) {

    return false;

  }


  var values =
    sheet
      .getDataRange()
      .getValues();


  // Apenas notificações recentes.
  // Evita deixar a planilha pesada.

  var inicio =
    Math.max(
      1,
      values.length - 20
    );


  for (
    var i = inicio;
    i < values.length;
    i++
  ) {

    if (

      normalizarTexto(
        values[i][colDest]
      ) ===
      normalizarTexto(
        destinatario
      )

      &&

      safeString(
        values[i][colTitulo]
      ).trim() ===
      safeString(
        titulo
      ).trim()

      &&

      safeString(
        values[i][colMensagem]
      ).trim() ===
      safeString(
        mensagem
      ).trim()

    ) {

      return true;

    }

  }


  return false;

}


// ============================================================
// PUSH VIA FIREBASE CLOUD MESSAGING (FCM)
// ------------------------------------------------------------
// A planilha continua sendo o backend/fonte de verdade — o FCM
// é usado só como canal de entrega. Depende da biblioteca OAuth2
// for Apps Script (ID 1B7FSrk5Zi6L1rSxxTDgDEUsPzlukDsi4KGuTMorsTQHhGBzBkMun4iDF)
// e das propriedades do script FCM_CLIENT_EMAIL, FCM_PRIVATE_KEY
// e FCM_PROJECT_ID, geradas a partir da conta de serviço do
// Firebase (Configurações do projeto > Contas de serviço).
// ============================================================

function getServiceAccountFromProperties_() {
  var props = PropertiesService.getScriptProperties();
  return {
    client_email: props.getProperty("FCM_CLIENT_EMAIL"),
    private_key: (props.getProperty("FCM_PRIVATE_KEY") || "").replace(/\\n/g, "\n"),
    project_id: props.getProperty("FCM_PROJECT_ID")
  };
}

function getFcmService_() {
  var sa = getServiceAccountFromProperties_();
  return OAuth2.createService("FCM")
    .setTokenUrl("https://oauth2.googleapis.com/token")
    .setPrivateKey(sa.private_key)
    .setIssuer(sa.client_email)
    .setPropertyStore(PropertiesService.getScriptProperties())
    .setScope("https://www.googleapis.com/auth/firebase.messaging");
}

// Envia um push para um único token de dispositivo. Retorna
// true/false, e nunca lança exceção para quem chamou — uma falha
// de push nunca deve impedir a notificação de continuar gravada
// normalmente na aba NOTIFICACOES.
function enviarPushFCM(token, titulo, mensagem) {

  if (!token) { return false; }

  try {

    var service = getFcmService_();

    if (!service.hasAccess()) {
      Logger.log("Erro de autenticação FCM: " + service.getLastError());
      return false;
    }

    var sa = getServiceAccountFromProperties_();
    var url = "https://fcm.googleapis.com/v1/projects/" + sa.project_id + "/messages:send";

    var payload = {
      message: {
        token: token,
        notification: {
          title: safeString(titulo),
          body: safeString(mensagem)
        }
      }
    };

    var options = {
      method: "post",
      contentType: "application/json",
      headers: { Authorization: "Bearer " + service.getAccessToken() },
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    };

    var response = UrlFetchApp.fetch(url, options);

    if (response.getResponseCode() !== 200) {
      Logger.log("Erro ao enviar push FCM: " + response.getContentText());
      return false;
    }

    return true;

  } catch (error) {

    Logger.log("Exceção ao enviar push FCM: " + safeErrorMessage(error));
    return false;

  }

}

// Lê a aba INTEGRANTES uma única vez e monta um mapa
// nome normalizado -> token FCM. A coluna NOME é sempre a
// posição 0 (mesmo padrão posicional de mapIntegrante); a coluna
// do token é localizada pelo cabeçalho conter "token".
function buscarMapaTokensFcm_(ss) {

  var mapa = {};

  var sheet = getSheetSecure(ss, ABA_INTEGRANTES);
  if (!sheet) { return mapa; }

  var values = sheet.getDataRange().getValues();
  if (values.length < 2) { return mapa; }

  var headersInt = values[0].map(function(h) { return normalizarTexto(h); });

  var colToken = -1;
  for (var h = 0; h < headersInt.length; h++) {
    if (headersInt[h].indexOf("token") !== -1) {
      colToken = h;
      break;
    }
  }

  if (colToken < 0) { return mapa; }

  for (var i = 1; i < values.length; i++) {
    var nome = normalizarTexto(values[i][0]);
    var token = safeString(values[i][colToken]).trim();
    if (nome && token) { mapa[nome] = token; }
  }

  return mapa;

}

// Busca o token de um único destinatário (usada por
// criarNotificacao, o caminho de notificação individual).
function buscarTokenFcmPorNome_(ss, nome) {
  var mapa = buscarMapaTokensFcm_(ss);
  return mapa[normalizarTexto(nome)] || null;
}

// Envia push para vários destinatários de uma vez (usada por
// criarNotificacoesEmLote), reaproveitando uma única leitura da
// aba INTEGRANTES para todo o lote, em vez de uma leitura por
// destinatário.
function enviarPushParaLote_(ss, rowsToWrite, headersNotificacao, titulo, mensagem) {

  try {

    var colDestIdx = headersNotificacao.indexOf("destinatario");
    if (colDestIdx < 0) { return; }

    var mapaTokens = buscarMapaTokensFcm_(ss);

    for (var r = 0; r < rowsToWrite.length; r++) {

      var destinatario = normalizarTexto(rowsToWrite[r][colDestIdx]);
      var token = mapaTokens[destinatario];

      if (token) {
        enviarPushFCM(token, titulo, mensagem);
      }

    }

  } catch (error) {

    Logger.log("Erro ao enviar pushes em lote: " + safeErrorMessage(error));

  }

}


// ============================================================
// NOTIFICAR TODOS
// ============================================================

function criarNotificacaoParaTodos(
  ss,
  titulo,
  mensagem,
  tipo
) {

  var integrantes =
    getSheetData(
      ss,
      ABA_INTEGRANTES,
      mapIntegrante
    );

  var nomes = integrantes
    .map(function(i) { return safeString(i.nome).trim(); })
    .filter(function(n) { return n !== ""; });

  return criarNotificacoesEmLote(ss, nomes, titulo, mensagem, tipo);

}


// ============================================================
// NOTIFICAR LÍDERES
// ============================================================

function criarNotificacaoParaLideres(
  ss,
  titulo,
  mensagem,
  tipo
) {

  var integrantes =
    getSheetData(
      ss,
      ABA_INTEGRANTES,
      mapIntegrante
    );

  var nomesLideres = [];

  for (var i = 0; i < integrantes.length; i++) {
    var funcao = normalizarTexto(integrantes[i].funcao);
    if (funcao.indexOf("lider") !== -1) {
      var nome = safeString(integrantes[i].nome).trim();
      if (nome) { nomesLideres.push(nome); }
    }
  }

  return criarNotificacoesEmLote(ss, nomesLideres, titulo, mensagem, tipo);

}


// ============================================================
// CRIAR NOTIFICAÇÕES EM LOTE (BATCH)
// ------------------------------------------------------------
// Substitui o padrão antigo de chamar criarNotificacao() dentro
// de um loop (1 lock + 1 leitura + 1 escrita POR destinatário).
// Aqui: 1 lock + 1 leitura + 1 escrita para o lote inteiro.
// Isso é o que elimina os 15-20s de espera ao notificar todos
// os integrantes ou todos os líderes.
// ============================================================

function criarNotificacoesEmLote(
  ss,
  destinatarios,
  titulo,
  mensagem,
  tipo
) {

  if (!destinatarios || destinatarios.length === 0) {
    return 0;
  }

  var lock = LockService.getScriptLock();
  var rowsToWrite = [];
  var headers = [];
  var quantidade = 0;
  var tituloTrim = safeString(titulo).trim();
  var mensagemTrim = safeString(mensagem).trim();

  try {

    lock.waitLock(15000);

    var sheet = obterAbaNotificacoes(ss);
    headers = obterCabecalhosNotificacao(sheet);
    var colCount = Math.max(sheet.getLastColumn(), headers.length);

    var colDest = headers.indexOf("destinatario");
    var colTit = headers.indexOf("titulo");
    var colMsg = headers.indexOf("mensagem");

    // Leitura única para checar duplicidade em memória
    // (mesma regra de negócio de notificacaoDuplicada: só olha
    // as últimas 20 linhas, para não pesar em planilhas grandes).
    var values = sheet.getDataRange().getValues();
    var checkLimit = Math.max(1, values.length - 20);

    var now = new Date();

    for (var d = 0; d < destinatarios.length; d++) {

      var destinatario = safeString(destinatarios[d]).trim();
      if (!destinatario) { continue; }

      var isDup = false;

      if (colDest >= 0 && colTit >= 0 && colMsg >= 0) {
        for (var j = values.length - 1; j >= checkLimit; j--) {
          if (
            normalizarTexto(values[j][colDest]) === normalizarTexto(destinatario) &&
            safeString(values[j][colTit]).trim() === tituloTrim &&
            safeString(values[j][colMsg]).trim() === mensagemTrim
          ) {
            isDup = true;
            break;
          }
        }
      }

      if (isDup) { continue; }

      var newRow = new Array(colCount).fill("");
      definirValorColuna(newRow, headers, "id", Utilities.getUuid());
      definirValorColuna(newRow, headers, "destinatario", destinatario);
      definirValorColuna(newRow, headers, "titulo", tituloTrim);
      definirValorColuna(newRow, headers, "mensagem", mensagemTrim);
      definirValorColuna(newRow, headers, "tipo", safeString(tipo).trim());
      definirValorColuna(newRow, headers, "data", now);
      definirValorColuna(newRow, headers, "lida", "NAO");

      rowsToWrite.push(newRow);
      quantidade++;
    }

    // ESCRITA ÚNICA para todo o lote.
    if (rowsToWrite.length > 0) {
      sheet.getRange(sheet.getLastRow() + 1, 1, rowsToWrite.length, colCount)
        .setValues(rowsToWrite);
    }

  } catch (error) {

    Logger.log("Erro no batch de notificações: " + safeErrorMessage(error));
    return 0;

  } finally {

    try { lock.releaseLock(); } catch (e) {}

  }

  // Envio dos pushes acontece FORA do lock — já foi liberado,
  // então outras requisições não ficam esperando enquanto os
  // pushes (um por destinatário) são enviados um a um.
  if (rowsToWrite.length > 0) {
    enviarPushParaLote_(ss, rowsToWrite, headers, tituloTrim, mensagemTrim);
  }

  return quantidade;

}


// ============================================================
// BUSCAR LINKS DOS LOUVORES
// ============================================================
function getLinkLouvores(params) {
  try {
    var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    var dataEscala = safeString(
      params.data || params.dataEscala || params.data_escala
    ).trim();

    if (!dataEscala) {
      return {
        sucesso: false,
        mensagem: "Data da escala não informada.",
        louvores: []
      };
    }

    return {
      sucesso: true,
      mensagem: "Links dos louvores carregados.",
      louvores: obterLinksLouvoresPorData(ss, dataEscala)
    };
  } catch (error) {
    return {
      sucesso: false,
      mensagem: "Erro ao buscar links dos louvores: " + safeErrorMessage(error),
      louvores: []
    };
  }
}

// ============================================================
// GET NOTIFICAÇÕES
// ============================================================

function getNotificacoes(params) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_NOTIFICACOES
      );


    if (!sheet) {

      return {

        sucesso: true,

        mensagem:
          "Nenhuma notificação encontrada.",

        notificacoes: []

      };

    }


    var nome =
      safeString(
        params.nome
      ).trim();


    if (!nome) {

      return {

        sucesso: false,

        mensagem:
          "Nome do usuário não informado.",

        notificacoes: []

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    if (
      values.length <= 1
    ) {

      return {

        sucesso: true,

        mensagem:
          "Nenhuma notificação encontrada.",

        notificacoes: []

      };

    }


    var headers =
      values[0].map(
        function(header) {

          return normalizarTexto(
            header
          ).replace(
            /\s+/g,
            "_"
          );

        }
      );


    var colId =
      headers.indexOf("id");


    var colDestinatario =
      headers.indexOf(
        "destinatario"
      );


    var colTitulo =
      headers.indexOf("titulo");


    var colMensagem =
      headers.indexOf("mensagem");


    var colTipo =
      headers.indexOf("tipo");


    var colData =
      headers.indexOf("data");


    var colLida =
      headers.indexOf("lida");


    if (
      colDestinatario < 0
    ) {

      return {

        sucesso: false,

        mensagem:
          "A coluna DESTINATARIO não foi encontrada na aba NOTIFICACOES.",

        notificacoes: []

      };

    }


    var notificacoes = [];


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      var row =
        values[i];


      var destinatario =
        safeString(
          row[colDestinatario]
        ).trim();


      if (
        normalizarTexto(
          destinatario
        ) !==
        normalizarTexto(nome)
      ) {

        continue;

      }


      var dataFormatada =
        formatDateISO(
          colData >= 0
            ? row[colData]
            : ""
        );


      notificacoes.push({

        id:
          colId >= 0
            ? safeString(
                row[colId]
              )
            : "",

        destinatario:
          destinatario,

        titulo:
          colTitulo >= 0
            ? safeString(
                row[colTitulo]
              )
            : "",

        mensagem:
          colMensagem >= 0
            ? safeString(
                row[colMensagem]
              )
            : "",

        tipo:
          colTipo >= 0
            ? safeString(
                row[colTipo]
              )
            : "",

        data:
          dataFormatada,

        lida:
          colLida >= 0
            ? (
                safeString(
                  row[colLida]
                ).trim() ||
                "NAO"
              )
            : "NAO"

      });

    }


    // Mais recentes primeiro.

    notificacoes.reverse();


    return {

      sucesso: true,

      mensagem:
        notificacoes.length +
        " notificação(ões) encontrada(s).",

      notificacoes:
        notificacoes

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao buscar notificações: " +
        safeErrorMessage(error),

      notificacoes: []

    };

  }

}


// ============================================================
// MARCAR UMA COMO LIDA
// ============================================================

// ============================================================
// ATUALIZAR TOKEN FCM
// ------------------------------------------------------------
// Recebe {nome, token} do app (enviado logo após o login e
// sempre que o Firebase gera um token novo para o dispositivo)
// e grava esse token na aba INTEGRANTES, na coluna FCM_TOKEN.
// É esse token que criarNotificacao / criarNotificacoesEmLote
// vão usar para saber para qual dispositivo enviar cada push.
// ============================================================

function atualizarTokenFcm(
  params
) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_INTEGRANTES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba de integrantes não encontrada."

      };

    }


    var nome =
      safeString(
        params.nome
      ).trim();


    var token =
      safeString(
        params.token
      ).trim();


    if (!nome) {

      return {

        sucesso: false,

        mensagem:
          "Nome não informado."

      };

    }


    if (!token) {

      return {

        sucesso: false,

        mensagem:
          "Token não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    // A coluna NOME nesta aba é sempre a primeira (posição 0),
    // igual ao que mapIntegrante já assume para o resto do
    // sistema — esta aba não usa busca por texto de cabeçalho
    // para essa coluna.
    var colNome = 0;


    // Já a coluna do token é nova, então localizamos pelo texto
    // do cabeçalho de forma tolerante (aceita "FCM_TOKEN",
    // "FCM TOKEN", "Token FCM", etc. — qualquer célula que
    // contenha "token" no cabeçalho).
    var headers =
      values[0].map(
        function(header) {
          return normalizarTexto(header);
        }
      );


    var colToken = -1;

    for (var h = 0; h < headers.length; h++) {
      if (headers[h].indexOf("token") !== -1) {
        colToken = h;
        break;
      }
    }


    if (colToken < 0) {

      return {

        sucesso: false,

        mensagem:
          "Coluna de token FCM não encontrada na aba INTEGRANTES. " +
          "O cabeçalho dessa coluna precisa conter a palavra \"token\" " +
          "(ex: FCM_TOKEN)."

      };

    }


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (

        normalizarTexto(
          values[i][colNome]
        ) ===
        normalizarTexto(nome)

      ) {

        sheet
          .getRange(
            i + 1,
            colToken + 1
          )
          .setValue(
            token
          );


        return {

          sucesso: true,

          mensagem:
            "Token atualizado com sucesso."

        };

      }

    }


    return {

      sucesso: false,

      mensagem:
        "Integrante não encontrado: " + nome

    };

  } catch (error) {

    Logger.log(
      "Erro em atualizarTokenFcm: " +
      safeErrorMessage(error)
    );

    return {

      sucesso: false,

      mensagem:
        "Erro ao atualizar token: " +
        safeErrorMessage(error)

    };

  }

}


function marcarNotificacaoLida(
  params
) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_NOTIFICACOES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba NOTIFICACOES não encontrada."

      };

    }


    var id =
      safeString(
        params.id
      ).trim();


    var nome =
      safeString(
        params.nome
      ).trim();


    if (!id) {

      return {

        sucesso: false,

        mensagem:
          "ID da notificação não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    if (
      values.length <= 1
    ) {

      return {

        sucesso: false,

        mensagem:
          "Nenhuma notificação encontrada."

      };

    }


    var headers =
      values[0].map(
        function(header) {

          return normalizarTexto(
            header
          ).replace(
            /\s+/g,
            "_"
          );

        }
      );


    var colId =
      headers.indexOf("id");


    var colDest =
      headers.indexOf(
        "destinatario"
      );


    var colLida =
      headers.indexOf("lida");


    if (
      colId < 0 ||
      colLida < 0
    ) {

      return {

        sucesso: false,

        mensagem:
          "Estrutura da aba NOTIFICACOES inválida."

      };

    }


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (
        !compararId(
          values[i][colId],
          id
        )
      ) {

        continue;

      }


      // ------------------------------------------------------
      // SEGURANÇA
      // ------------------------------------------------------

      if (
        nome &&
        colDest >= 0
      ) {

        if (
          normalizarTexto(
            values[i][colDest]
          ) !==
          normalizarTexto(nome)
        ) {

          return {

            sucesso: false,

            mensagem:
              "Notificação não pertence ao usuário informado."

          };

        }

      }


      sheet
        .getRange(
          i + 1,
          colLida + 1
        )
        .setValue(
          "SIM"
        );


      return {

        sucesso: true,

        mensagem:
          "Notificação marcada como lida."

      };

    }


    return {

      sucesso: false,

      mensagem:
        "Notificação não encontrada."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao marcar notificação: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// MARCAR TODAS COMO LIDAS
// ============================================================

function marcarTodasNotificacoesLidas(
  params
) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_NOTIFICACOES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba NOTIFICACOES não encontrada."

      };

    }


    var nome =
      safeString(
        params.nome
      ).trim();


    if (!nome) {

      return {

        sucesso: false,

        mensagem:
          "Nome do usuário não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    if (
      values.length <= 1
    ) {

      return {

        sucesso: true,

        mensagem:
          "Nenhuma notificação para atualizar."

      };

    }


    var headers =
      values[0].map(
        function(header) {

          return normalizarTexto(
            header
          ).replace(
            /\s+/g,
            "_"
          );

        }
      );


    var colDest =
      headers.indexOf(
        "destinatario"
      );


    var colLida =
      headers.indexOf(
        "lida"
      );


    if (
      colDest < 0 ||
      colLida < 0
    ) {

      return {

        sucesso: false,

        mensagem:
          "Estrutura da aba NOTIFICACOES inválida."

      };

    }


    var quantidade =
      0;

    // Monta a coluna LIDA inteira em memória e grava tudo de
    // uma vez só, em vez de 1 setValue() por linha encontrada.
    var colunaLida = [];

    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      var valorAtual = values[i][colLida];

      if (
        normalizarTexto(values[i][colDest]) === normalizarTexto(nome) &&
        normalizarTexto(valorAtual) !== "sim"
      ) {

        colunaLida.push(["SIM"]);
        quantidade++;

      } else {

        colunaLida.push([valorAtual]);

      }

    }

    if (quantidade > 0) {
      sheet.getRange(2, colLida + 1, colunaLida.length, 1)
        .setValues(colunaLida);
    }


    return {

      sucesso: true,

      mensagem:
        quantidade +
        " notificação(ões) marcada(s) como lida(s)."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao marcar notificações: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// EXCLUIR NOTIFICAÇÃO
// ============================================================

function deleteNotificacao(
  params
) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_NOTIFICACOES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba NOTIFICACOES não encontrada."

      };

    }


    var id =
      safeString(
        params.id
      ).trim();


    var nome =
      safeString(
        params.nome
      ).trim();


    if (!id) {

      return {

        sucesso: false,

        mensagem:
          "ID da notificação não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    if (
      values.length <= 1
    ) {

      return {

        sucesso: false,

        mensagem:
          "Notificação não encontrada."

      };

    }


    var headers =
      values[0].map(
        function(header) {

          return normalizarTexto(
            header
          ).replace(
            /\s+/g,
            "_"
          );

        }
      );


    var colId =
      headers.indexOf("id");


    var colDest =
      headers.indexOf(
        "destinatario"
      );


    if (
      colId < 0
    ) {

      return {

        sucesso: false,

        mensagem:
          "Coluna ID não encontrada."

      };

    }


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (
        !compararId(
          values[i][colId],
          id
        )
      ) {

        continue;

      }


      // Segurança: se nome for informado,
      // somente o dono pode excluir.

      if (
        nome &&
        colDest >= 0 &&
        normalizarTexto(
          values[i][colDest]
        ) !==
        normalizarTexto(nome)
      ) {

        return {

          sucesso: false,

          mensagem:
            "Notificação não pertence ao usuário informado."

        };

      }


      sheet.deleteRow(
        i + 1
      );


      return {

        sucesso: true,

        mensagem:
          "Notificação excluída."

      };

    }


    return {

      sucesso: false,

      mensagem:
        "Notificação não encontrada."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao excluir notificação: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// LIMPAR NOTIFICAÇÕES LIDAS
// ============================================================

function limparNotificacoesLidas(
  params
) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var sheet =
      getSheetSecure(
        ss,
        ABA_NOTIFICACOES
      );


    if (!sheet) {

      return {

        sucesso: false,

        mensagem:
          "Aba NOTIFICACOES não encontrada."

      };

    }


    var nome =
      safeString(
        params.nome
      ).trim();


    if (!nome) {

      return {

        sucesso: false,

        mensagem:
          "Nome do usuário não informado."

      };

    }


    var values =
      sheet
        .getDataRange()
        .getValues();


    if (
      values.length <= 1
    ) {

      return {

        sucesso: true,

        mensagem:
          "Nenhuma notificação encontrada."

      };

    }


    var headers =
      values[0].map(
        function(header) {

          return normalizarTexto(
            header
          ).replace(
            /\s+/g,
            "_"
          );

        }
      );


    var colDest =
      headers.indexOf(
        "destinatario"
      );


    var colLida =
      headers.indexOf(
        "lida"
      );


    if (
      colDest < 0 ||
      colLida < 0
    ) {

      return {

        sucesso: false,

        mensagem:
          "Estrutura da aba NOTIFICACOES inválida."

      };

    }


    // Em vez de sheet.deleteRow() repetido (cada chamada reindexa
    // a planilha inteira), filtramos em memória e reescrevemos os
    // dados de uma só vez: 1 limpeza + 1 escrita, não importa
    // quantas notificações sejam removidas.

    var colCount = values[0].length;
    var linhasMantidas = [];
    var removidas = 0;

    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      var doUsuario =
        normalizarTexto(values[i][colDest]) === normalizarTexto(nome);

      var estaLida =
        normalizarTexto(values[i][colLida]) === "sim";

      if (doUsuario && estaLida) {
        removidas++;
      } else {
        linhasMantidas.push(values[i]);
      }

    }

    if (removidas > 0) {

      // Limpa todo o bloco de dados atual...
      sheet.getRange(2, 1, values.length - 1, colCount).clearContent();

      // ...e regrava só o que sobrou, em uma única chamada.
      if (linhasMantidas.length > 0) {
        sheet.getRange(2, 1, linhasMantidas.length, colCount)
          .setValues(linhasMantidas);
      }

    }


    return {

      sucesso: true,

      mensagem:
        removidas +
        " notificação(ões) removida(s)."

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro ao limpar notificações: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// TESTE DO SISTEMA
// ============================================================

function testarSistemaNotificacoesInterno(
  params
) {

  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    var nome =
      safeString(
        params.nome
      ).trim();


    if (!nome) {

      return {

        sucesso: false,

        mensagem:
          "Informe o nome para testar."

      };

    }


    var criada =
      criarNotificacao(

        ss,

        nome,

        "Teste de notificação",

        "Esta é uma notificação de teste do Escala de Louvor 2K26.",

        "TESTE"

      );


    var resultado =
      getNotificacoes({

        nome: nome

      });


    return {

      sucesso:
        resultado.sucesso,

      mensagem:
        criada

          ? "Notificação de teste criada."

          : "Notificação semelhante já existia.",

      notificacoes:
        resultado.notificacoes || []

    };


  } catch (error) {

    return {

      sucesso: false,

      mensagem:
        "Erro no teste: " +
        safeErrorMessage(error)

    };

  }

}


// ============================================================
// TESTE MANUAL - NOTIFICAÇÃO
// ============================================================

function testarGetNotificacoes() {

  var nome =
    "JACO";


  Logger.log(
    "========================================"
  );

  Logger.log(
    "TESTE GET NOTIFICACOES"
  );

  Logger.log(
    "========================================"
  );


  try {

    var resultado =
      getNotificacoes({

        nome:
          nome

      });


    Logger.log(
      JSON.stringify(
        resultado,
        null,
        2
      )
    );


    Logger.log(
      "Sucesso: " +
      resultado.sucesso
    );


    Logger.log(
      "Mensagem: " +
      resultado.mensagem
    );


    Logger.log(
      "Quantidade: " +
      (
        resultado.notificacoes
          ? resultado.notificacoes.length
          : 0
      )
    );


  } catch (error) {

    Logger.log(
      "ERRO:"
    );


    Logger.log(
      safeErrorMessage(error)
    );

  }


  Logger.log(
    "========================================"
  );

}


// ============================================================
// TESTE COMPLETO DE NOTIFICAÇÃO
// ============================================================

function testarNotificacaoCompleta() {

  var nome =
    "JACO";


  Logger.log(
    "=========================================="
  );

  Logger.log(
    "TESTE COMPLETO DO SISTEMA DE NOTIFICAÇÕES"
  );

  Logger.log(
    "=========================================="
  );


  try {

    var ss =
      SpreadsheetApp.openById(
        SPREADSHEET_ID
      );


    Logger.log(
      "1. Planilha acessada com sucesso."
    );


    var sheet =
      obterAbaNotificacoes(
        ss
      );


    Logger.log(
      "2. Aba NOTIFICACOES OK."
    );


    garantirEstruturaNotificacoes(
      sheet
    );


    Logger.log(
      "3. Estrutura da aba OK."
    );


    var criada =
      criarNotificacao(

        ss,

        nome,

        "Teste manual",

        "Se esta mensagem apareceu, a criação de notificações está funcionando.",

        "TESTE"

      );


    Logger.log(
      "4. Notificação criada: " +
      criada
    );


    var resultado =
      getNotificacoes({

        nome:
          nome

      });


    Logger.log(
      "5. Quantidade encontrada: " +
      resultado.notificacoes.length
    );


    Logger.log(
      JSON.stringify(
        resultado,
        null,
        2
      )
    );


  } catch (error) {

    Logger.log(
      "ERRO:"
    );


    Logger.log(
      safeErrorMessage(error)
    );

  }


  Logger.log(
    "=========================================="
  );

}


// ============================================================
// TESTE - MARCAR COMO LIDA
// ============================================================

function testarMarcarNotificacaoLida() {

  var nome =
    "JACO";


  try {

    var resultado =
      getNotificacoes({

        nome:
          nome

      });


    if (
      !resultado.sucesso ||
      !resultado.notificacoes ||
      resultado.notificacoes.length === 0
    ) {

      Logger.log(
        "Nenhuma notificação encontrada para teste."
      );

      return;

    }


    var notificacao =
      resultado.notificacoes[0];


    Logger.log(
      "ID testado: " +
      notificacao.id
    );


    var resposta =
      marcarNotificacaoLida({

        id:
          notificacao.id,

        nome:
          nome

      });


    Logger.log(
      JSON.stringify(
        resposta,
        null,
        2
      )
    );


  } catch (error) {

    Logger.log(
      "ERRO: " +
      safeErrorMessage(error)
    );

  }

}


// ============================================================
// GOOGLE DRIVE
// ============================================================

function saveImageToDrive(
  base64Data
) {

  if (!base64Data) {
    return "";
  }


  try {

    var original =
      String(
        base64Data
      );


    var contentType =
      "image/jpeg";


    var mimeMatch =
      original.match(
        /^data:(image\/[a-zA-Z0-9.+-]+);base64,/
      );


    if (
      mimeMatch
    ) {

      contentType =
        mimeMatch[1];

    }


    var cleanBase64 =
      original.replace(
        /^data:image\/[a-zA-Z0-9.+-]+;base64,/,
        ""
      );


    var folder =
      DriveApp.getFolderById(
        FOLDER_ID
      );


    var extension =
      "jpg";


    if (
      contentType === "image/png"
    ) {

      extension =
        "png";

    }

    else if (
      contentType === "image/webp"
    ) {

      extension =
        "webp";

    }

    else if (
      contentType === "image/gif"
    ) {

      extension =
        "gif";

    }


    var blob =
      Utilities.newBlob(

        Utilities.base64Decode(
          cleanBase64
        ),

        contentType,

        "recado_" +
        new Date().getTime() +
        "." +
        extension

      );


    var file =
      folder.createFile(
        blob
      );


    // Compartilhamento é tentado,
    // mas não impede o upload caso
    // a política do Drive bloqueie.

    try {

      file.setSharing(

        DriveApp.Access.ANYONE_WITH_LINK,

        DriveApp.Permission.VIEW

      );

    } catch (sharingError) {

      Logger.log(

        "Aviso ao compartilhar arquivo: " +
        safeErrorMessage(
          sharingError
        )

      );

    }


    return (
      "https://drive.google.com/uc?export=view&id=" +
      file.getId()
    );


  } catch (error) {

    throw new Error(

      "Erro ao salvar imagem no Google Drive: " +
      safeErrorMessage(error)

    );

  }

}


// ============================================================
// TESTE DRIVE
// ============================================================

function testarCriacaoArquivoDrive() {

  try {

    var folder =
      DriveApp.getFolderById(
        FOLDER_ID
      );


    var blob =
      Utilities.newBlob(

        "Teste de upload - " +
        new Date(),

        "text/plain",

        "teste_recado.txt"

      );


    var file =
      folder.createFile(
        blob
      );


    Logger.log(
      "OK - arquivo criado"
    );


    Logger.log(
      "Nome: " +
      file.getName()
    );


    Logger.log(
      "ID: " +
      file.getId()
    );


    Logger.log(
      "URL: " +
      file.getUrl()
    );


  } catch (error) {

    Logger.log(
      "ERRO AO CRIAR ARQUIVO:"
    );


    Logger.log(
      safeErrorMessage(error)
    );


  }

}


// ============================================================
// TESTE COMPARTILHAMENTO DRIVE
// ============================================================

function testarCompartilhamentoDrive() {

  try {

    var folder =
      DriveApp.getFolderById(
        FOLDER_ID
      );


    var blob =
      Utilities.newBlob(

        "Teste de compartilhamento",

        "text/plain",

        "teste_compartilhamento.txt"

      );


    var file =
      folder.createFile(
        blob
      );


    Logger.log(
      "Arquivo criado: " +
      file.getId()
    );


    file.setSharing(

      DriveApp.Access.ANYONE_WITH_LINK,

      DriveApp.Permission.VIEW

    );


    Logger.log(
      "COMPARTILHAMENTO ALTERADO COM SUCESSO"
    );


    Logger.log(
      "URL: " +
      file.getUrl()
    );


  } catch (error) {

    Logger.log(
      "ERRO:"
    );


    Logger.log(
      safeErrorMessage(error)
    );

  }

}


// ============================================================
// AUXILIARES DE SOLICITAÇÃO
// ============================================================

function obterIdSolicitacao(row) {

  if (!row) {
    return "";
  }


  if (
    row.length >= 8
  ) {

    return safeString(
      row[0]
    ).trim();

  }


  return "";

}


// ============================================================

function obterDataSolicitacao(row) {

  if (!row) {
    return "";
  }


  if (
    row.length >= 8
  ) {

    return row[1];

  }


  return row[0];

}


// ============================================================

function obterQuemPediu(row) {

  if (!row) {
    return "";
  }


  if (
    row.length >= 8
  ) {

    return safeString(
      row[2]
    );

  }


  return safeString(
    row[1]
  );

}


// ============================================================

function obterFuncaoSolicitacao(row) {

  if (
    !row ||
    row.length < 8
  ) {

    return "";

  }


  return safeString(
    row[3]
  );

}


// ============================================================

function obterInstrumentoSolicitacao(row) {

  if (
    !row ||
    row.length < 8
  ) {

    return "";

  }


  return safeString(
    row[4]
  );

}


// ============================================================

function obterSubstituto(row) {

  if (!row) {
    return "";
  }


  if (
    row.length >= 8
  ) {

    return safeString(
      row[5]
    );

  }


  return safeString(
    row[2]
  );

}


// ============================================================

function obterMotivoSolicitacao(row) {

  if (!row) {
    return "";
  }


  if (
    row.length >= 8
  ) {

    return safeString(
      row[6]
    );

  }


  return safeString(
    row[3]
  );

}


// ============================================================

function obterStatusSolicitacao(row) {

  if (!row) {
    return "";
  }


  if (
    row.length >= 8
  ) {

    return safeString(
      row[7]
    )
      .trim()
      .toUpperCase();

  }


  return safeString(
    row[4]
  )
    .trim()
    .toUpperCase();

}


// ============================================================
// NOME
// ============================================================

function contemNome(
  texto,
  nome
) {

  var textoNormalizado =
    normalizarTexto(
      texto
    );


  var nomeNormalizado =
    normalizarTexto(
      nome
    );


  if (
    !textoNormalizado ||
    !nomeNormalizado
  ) {

    return false;

  }


  return (
    textoNormalizado.indexOf(
      nomeNormalizado
    ) !== -1
  );

}


// ============================================================
// SUBSTITUIR NOME
// ============================================================

function substituirNome(
  texto,
  nomeAntigo,
  nomeNovo
) {

  if (
    !nomeAntigo
  ) {

    return safeString(
      texto
    );

  }


  var regex =
    new RegExp(
      escapeRegExp(
        nomeAntigo
      ),
      "gi"
    );


  return safeString(
    texto
  ).replace(
    regex,
    nomeNovo
  );

}


// ============================================================
// ESCAPAR REGEX
// ============================================================

function escapeRegExp(
  texto
) {

  return String(
    texto
  ).replace(
    /[.*+?^${}()|[\]\\]/g,
    "\\$&"
  );

}


// ============================================================
// COMPARAR ID
// ============================================================

function compararId(
  idA,
  idB
) {

  return (

    safeString(idA)
      .trim()

    ===

    safeString(idB)
      .trim()

  );

}


// ============================================================
// COMPARAÇÃO DE DATAS
// ============================================================

function sameDate(
  sheetValue,
  receivedValue
) {

  if (
    sheetValue === null ||
    sheetValue === undefined ||
    receivedValue === null ||
    receivedValue === undefined
  ) {

    return false;

  }


  var recebido =
    parseDateOnly(
      receivedValue
    );


  var planilha =
    parseDateOnly(
      sheetValue
    );


  if (
    !recebido ||
    !planilha
  ) {

    return (

      normalizarTexto(
        sheetValue
      ) ===
      normalizarTexto(
        receivedValue
      )

    );

  }


  return (

    planilha.year ===
    recebido.year

    &&

    planilha.month ===
    recebido.month

    &&

    planilha.day ===
    recebido.day

  );

}


// ============================================================
// PARSE DE DATA SEM ERRO DE FUSO
// ============================================================

function parseDateOnly(
  value
) {

  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {

    return null;

  }


  // ----------------------------------------------------------
  // Date real
  // ----------------------------------------------------------

  if (
    Object.prototype.toString.call(
      value
    ) ===
    "[object Date]"
  ) {

    if (
      isNaN(
        value.getTime()
      )
    ) {

      return null;

    }


    return {

      year:
        Number(
          Utilities.formatDate(
            value,
            "GMT-3",
            "yyyy"
          )
        ),

      month:
        Number(
          Utilities.formatDate(
            value,
            "GMT-3",
            "MM"
          )
        ),

      day:
        Number(
          Utilities.formatDate(
            value,
            "GMT-3",
            "dd"
          )
        )

    };

  }


  var texto =
    safeString(
      value
    ).trim();


  // dd/MM/yyyy

  var br =
    texto.match(
      /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/
    );


  if (br) {

    return {

      year:
        Number(br[3]),

      month:
        Number(br[2]),

      day:
        Number(br[1])

    };

  }


  // yyyy-MM-dd

  var iso =
    texto.match(
      /^(\d{4})-(\d{2})-(\d{2})/
    );


  if (iso) {

    return {

      year:
        Number(iso[1]),

      month:
        Number(iso[2]),

      day:
        Number(iso[3])

    };

  }


  return null;

}


// ============================================================
// NORMALIZAÇÃO DE TEXTO
// ============================================================

function normalizarTexto(
  txt
) {

  if (
    txt === null ||
    txt === undefined
  ) {

    return "";

  }


  return txt
    .toString()
    .toLowerCase()
    .trim()
    .normalize("NFD")
    .replace(
      /[\u0300-\u036f]/g,
      ""
    )
    .replace(
      /\s+/g,
      " "
    );

}


// ============================================================
// STRING SEGURA
// ============================================================

function safeString(
  value
) {

  if (
    value === null ||
    value === undefined
  ) {

    return "";

  }


  return String(
    value
  );

}


// ============================================================
// ERRO
// ============================================================

function safeErrorMessage(
  error
) {

  try {

    if (!error) {

      return "Erro desconhecido.";

    }


    if (
      error.message
    ) {

      return String(
        error.message
      );

    }


    return String(
      error
    );


  } catch (e) {

    return "Erro desconhecido.";

  }

}


// ============================================================
// DATA BR
// ============================================================

function formatDateBR(
  value
) {

  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {

    return "";

  }


  try {

    // Date real

    if (
      Object.prototype.toString.call(
        value
      ) ===
      "[object Date]"
    ) {

      if (
        isNaN(
          value.getTime()
        )
      ) {

        return "";

      }


      return Utilities.formatDate(

        value,

        "GMT-3",

        "dd/MM/yyyy"

      );

    }


    var texto =
      safeString(
        value
      ).trim();


    // Já está em formato BR.

    if (
      /^\d{1,2}\/\d{1,2}\/\d{4}$/.test(
        texto
      )
    ) {

      var partes =
        texto.split("/");


      return (

        ("0" +
          partes[0]
        ).slice(-2)

        +

        "/" +

        ("0" +
          partes[1]
        ).slice(-2)

        +

        "/" +

        partes[2]

      );

    }


    // ISO.

    var date =
      new Date(
        texto
      );


    if (
      isNaN(
        date.getTime()
      )
    ) {

      return texto;

    }


    return Utilities.formatDate(

      date,

      "GMT-3",

      "dd/MM/yyyy"

    );


  } catch (error) {

    return safeString(
      value
    );

  }

}


// ============================================================
// DATA/HORA BR
// ============================================================

function formatDateTimeBR(
  value
) {

  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {

    return "";

  }


  try {

    if (
      Object.prototype.toString.call(
        value
      ) ===
      "[object Date]"
    ) {

      if (
        isNaN(
          value.getTime()
        )
      ) {

        return "";

      }


      return Utilities.formatDate(

        value,

        "GMT-3",

        "dd/MM/yyyy HH:mm"

      );

    }


    var date =
      new Date(
        value
      );


    if (
      isNaN(
        date.getTime()
      )
    ) {

      return safeString(
        value
      );

    }


    return Utilities.formatDate(

      date,

      "GMT-3",

      "dd/MM/yyyy HH:mm"

    );


  } catch (error) {

    return safeString(
      value
    );

  }

}


// ============================================================
// DATA ISO PARA O ANDROID
// ============================================================

function formatDateISO(
  value
) {

  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {

    return "";

  }


  try {

    if (
      Object.prototype.toString.call(
        value
      ) ===
      "[object Date]"
    ) {

      if (
        isNaN(
          value.getTime()
        )
      ) {

        return "";

      }


      return value.toISOString();

    }


    var date =
      new Date(
        value
      );


    if (
      isNaN(
        date.getTime()
      )
    ) {

      return safeString(
        value
      );

    }


    return date.toISOString();


  } catch (error) {

    return safeString(
      value
    );

  }

}


// ============================================================
// JSON
// ============================================================

function responseJSON(
  obj
) {

  try {

    return ContentService

      .createTextOutput(
        JSON.stringify(obj)
      )

      .setMimeType(
        ContentService.MimeType.JSON
      );


  } catch (error) {

    return ContentService

      .createTextOutput(

        JSON.stringify({

          sucesso: false,

          mensagem:
            "Erro ao gerar resposta JSON."

        })

      )

      .setMimeType(
        ContentService.MimeType.JSON
      );

  }

}


// teste notificação

function testarEnvioPush() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var token = buscarTokenFcmPorNome_(ss, "JADSON");
  Logger.log("Token encontrado: " + token);

  if (token) {
    var enviado = enviarPushFCM(token, "Teste FCM", "Se você recebeu isso, funcionou!");
    Logger.log("Push enviado: " + enviado);
  }
}
