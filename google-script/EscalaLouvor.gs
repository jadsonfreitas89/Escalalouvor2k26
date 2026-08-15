// ============================================================
// ESCALA DE LOUVOR 2K26
// GOOGLE APPS SCRIPT - BACKEND ÚNICO
// ============================================================
//
// Backend único e consolidado.
//
// Compatível com:
// br.com.jadson.escalalouvor2k26.data.repository.EscalaRepository
//
// FUNCIONALIDADES:
//
// - ESCALA
// - INTEGRANTES
// - SOLICITAÇÕES
// - RECADOS
// - GOOGLE DRIVE
// - NOTIFICAÇÕES
//
// NOTIFICAÇÕES:
//
// - Criar para usuário
// - Criar para todos
// - Criar para líderes
// - Buscar notificações
// - Marcar uma como lida
// - Marcar todas como lidas
// - Excluir uma
// - Limpar todas as lidas
// - Proteção contra duplicação
//
// ============================================================


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
      recados: []

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

function getSheetSecure(
  ss,
  name
) {

  if (!ss || !name) {
    return null;
  }


  var sheets =
    ss.getSheets();


  var alvo =
    normalizarTexto(name);


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

      return sheets[i];

    }

  }


  return null;

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


  // Estrutura completa
  if (
    lastColumn >= 8
  ) {

    sheet
      .getRange(
        rowIndex,
        8
      )
      .setValue(
        status
      );


    sheet
      .getRange(
        rowIndex,
        9
      )
      .setValue(
        new Date()
      );


    if (
      lastColumn >= 10
    ) {

      sheet
        .getRange(
          rowIndex,
          10
        )
        .setValue(
          decididoPor
        );

    }


    if (
      lastColumn >= 11
    ) {

      sheet
        .getRange(
          rowIndex,
          11
        )
        .setValue(
          motivo
        );

    }


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


        sheet
          .getRange(
            i + 1,
            2,
            1,
            4
          )
          .setValues([[

            safeString(p.titulo),

            safeString(p.mensagem),

            imagem,

            ativo

          ]]);


        sheet
          .getRange(
            i + 1,
            7
          )
          .setValue(
            new Date()
          );


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


  var quantidade =
    0;


  for (
    var i = 0;
    i < integrantes.length;
    i++
  ) {

    var nome =
      safeString(
        integrantes[i].nome
      ).trim();


    if (!nome) {
      continue;
    }


    if (
      criarNotificacao(
        ss,
        nome,
        titulo,
        mensagem,
        tipo
      )
    ) {

      quantidade++;

    }

  }


  return quantidade;

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


  var quantidade =
    0;


  for (
    var i = 0;
    i < integrantes.length;
    i++
  ) {

    var funcao =
      normalizarTexto(
        integrantes[i].funcao
      );


    if (
      funcao.indexOf("lider") === -1
    ) {

      continue;

    }


    var nome =
      safeString(
        integrantes[i].nome
      ).trim();


    if (!nome) {
      continue;
    }


    if (
      criarNotificacao(
        ss,
        nome,
        titulo,
        mensagem,
        tipo
      )
    ) {

      quantidade++;

    }

  }


  return quantidade;

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


    for (
      var i = 1;
      i < values.length;
      i++
    ) {

      if (

        normalizarTexto(
          values[i][colDest]
        ) ===
        normalizarTexto(nome)

      ) {

        if (

          normalizarTexto(
            values[i][colLida]
          ) !== "sim"

        ) {

          sheet
            .getRange(
              i + 1,
              colLida + 1
            )
            .setValue(
              "SIM"
            );


          quantidade++;

        }

      }

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


    var removidas =
      0;


    // Sempre de baixo para cima.

    for (
      var i = values.length - 1;
      i >= 1;
      i--
    ) {

      if (

        normalizarTexto(
          values[i][colDest]
        ) ===
        normalizarTexto(nome)

        &&

        normalizarTexto(
          values[i][colLida]
        ) ===
        "sim"

      ) {

        sheet.deleteRow(
          i + 1
        );


        removidas++;

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


// ============================================================
// FIM DO BACKEND
// ============================================================
