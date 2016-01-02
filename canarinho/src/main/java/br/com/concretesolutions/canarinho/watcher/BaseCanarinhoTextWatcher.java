package br.com.concretesolutions.canarinho.watcher;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;

import br.com.concretesolutions.canarinho.formatador.Formatador;
import br.com.concretesolutions.canarinho.validator.Validador;
import br.com.concretesolutions.canarinho.watcher.evento.EventoDeValidacao;

public abstract class BaseCanarinhoTextWatcher implements TextWatcher {

    private boolean mudancaInterna = false;
    private int tamanhoAnterior = 0;
    private EventoDeValidacao eventoDeValidacao;

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Não faz nada aqui
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Não faz nada aqui
    }

    protected boolean isApagouCaracter(Editable s) {
        return tamanhoAnterior > s.length();
    }

    // Usa o Editable para atualizar o Editable
    // O cursor SEMPRE sera posicionado no final do conteúdo
    protected void atualizaTexto(Validador validador, Validador.ResultadoParcial resultadoParcial,
                                 Editable s, StringBuilder builder) {

        tamanhoAnterior = builder.length();
        mudancaInterna = true;
        s.replace(0, s.length(), builder, 0, builder.length());

        if (builder.toString().equals(s.toString())) {
            // TODO: estudar implantar a manutenção da posição do cursor
            Selection.setSelection(s, builder.length());
        }

        efetuaValidacao(validador, resultadoParcial, s);
        mudancaInterna = false;
    }

    // CUIDADO AO ATUALIZAR O Editable AQUI!!!
    protected void efetuaValidacao(Validador validador, Validador.ResultadoParcial resultadoParcial, Editable s) {
        validador.ehValido(s, resultadoParcial);

        if (eventoDeValidacao == null) {
            return;
        }

        if (!resultadoParcial.isParcialmenteValido()) {
            eventoDeValidacao.invalido(s.toString(), resultadoParcial.getMensagem());
        } else if (!resultadoParcial.isValido()) {
            eventoDeValidacao.parcialmenteValido(s.toString());
        } else {
            eventoDeValidacao.totalmenteValido(s.toString());
        }
    }

    protected StringBuilder trataAdicaoRemocaoDeCaracter(Editable s, char[] mascara) {
        return isApagouCaracter(s)
                ? trataRemocaoDeCaracter(s, mascara)
                : trataAdicaoDeCaracter(s, mascara);
    }

    private StringBuilder trataAdicaoDeCaracter(Editable s, char[] mascara) {
        return carregarMascara(s.toString(), mascara);
    }

    // Só é chamado após uma deleção, portanto, é seguro chamar mascara[s.length()]
    private StringBuilder trataRemocaoDeCaracter(Editable s, char[] mascara) {
        final StringBuilder builder = new StringBuilder(s);

        // Obtém a posição do último caracter excluído
        final int posicaoUltimoCaracter = mascara.length > s.length() ? s.length() : mascara.length - 1;

        // Verifica se o último caracter que foi excluído fazia parte da máscara
        final boolean ultimoCaracterEraMascara = mascara[posicaoUltimoCaracter] != '#';

        // Se o último caracter excluido fazia parte da máscara,
        // deve excluir até o primeiro caracter que não faz parte da máscara
        if (ultimoCaracterEraMascara) {
            boolean encontrouCaracterValido = false;
            while (builder.length() > 0 && !encontrouCaracterValido) {
                encontrouCaracterValido = mascara[builder.length() - 1] == '#';
                builder.deleteCharAt(builder.length() - 1);
            }
        }

        // Caso haja mais de um caracter de formatação (da máscara) faz um loop
        // até chegar em um caracter que não seja de formatação
        while (builder.length() > 0 && mascara[builder.length() - 1] != '#') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return carregarMascara(builder.toString(), mascara);
    }

    private StringBuilder carregarMascara(String s, char[] mascara) {
        final StringBuilder builder = new StringBuilder();
        final String str = Formatador.Padroes.PADRAO_SOMENTE_NUMEROS.matcher(s).replaceAll("");

        // Só carregará a máscara se existir algum valor informado
        if (str.length() > 0) {
            int j = 0; // Acompanha a posição nos dígitos

            // É recomendado não usar enhanced for em Android
            for (int i = 0; i < mascara.length; i++) {

                final char charMascara = mascara[i];

                if (charMascara != '#') { // '#' -> caracter de formatação
                    builder.append(charMascara);
                    continue;
                }

                if (j >= str.length()) {
                    break;
                }

                builder.append(str.charAt(j));
                j++;
            }
        }

        return builder;
    }

    public boolean isMudancaInterna() {
        return mudancaInterna;
    }

    public EventoDeValidacao getEventoDeValidacao() {
        return eventoDeValidacao;
    }

    public void setEventoDeValidacao(EventoDeValidacao eventoDeValidacao) {
        this.eventoDeValidacao = eventoDeValidacao;
    }
}
