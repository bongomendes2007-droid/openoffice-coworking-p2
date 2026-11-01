package br.com.openoffice.util;

import br.com.openoffice.exception.DadosInvalidosException;

import java.util.regex.Pattern;

/** Validações reutilizadas em vários pontos do sistema. */
public final class Validador {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");

    private Validador() {
    }

    public static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo " + campo + " é obrigatório.");
        }
        return valor.trim();
    }

    public static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public static boolean emailValido(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    /** Valida CPF pelo cálculo dos dois dígitos verificadores. */
    public static boolean cpfValido(String cpf) {
        String d = apenasDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return d.charAt(9) - '0' == digitoCpf(d, 9) && d.charAt(10) - '0' == digitoCpf(d, 10);
    }

    private static int digitoCpf(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static String formatarCpf(String cpf) {
        String d = apenasDigitos(cpf);
        if (d.length() != 11) {
            return cpf;
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Algoritmo de Luhn, usado pelas operadoras para validar números de cartão. */
    public static boolean cartaoValido(String numero) {
        String d = apenasDigitos(numero);
        if (d.length() < 13 || d.length() > 19) {
            return false;
        }
        int soma = 0;
        boolean dobrar = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (dobrar) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            soma += n;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }

    /** Normaliza documentos (CPF/RG) para comparação: só letras e números, em maiúsculas. */
    public static String normalizarDocumento(String documento) {
        return documento == null ? "" : documento.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
    }
}
