// Comportamentos simples das páginas (sem frameworks).
document.addEventListener('DOMContentLoaded', () => {

    // Mostra os campos do cartão só quando "Cartão" estiver selecionado.
    document.querySelectorAll('.pagamento').forEach(bloco => {
        const atualizar = () => {
            const forma = bloco.querySelector('input[type=radio]:checked')?.value || 'PIX';
            bloco.querySelector('.dados-cartao').hidden = forma !== 'CARTAO';
            bloco.querySelector('.dica-pix').hidden = forma !== 'PIX';
            bloco.querySelector('.dica-boleto').hidden = forma !== 'BOLETO';
            bloco.closest('form')?.querySelectorAll('[data-cartao-teste]')
                .forEach(b => { b.hidden = forma !== 'CARTAO'; });
        };
        bloco.querySelectorAll('input[type=radio]').forEach(r => r.addEventListener('change', atualizar));
        atualizar();
    });

    // Pede confirmação antes de ações importantes (formulários com data-confirmar).
    document.querySelectorAll('form[data-confirmar]').forEach(form => {
        form.addEventListener('submit', evento => {
            if (!window.confirm(form.dataset.confirmar)) {
                evento.preventDefault();
            }
        });
    });

    // Mapa de calor: pinta as células conforme o valor (quanto maior, mais escuro).
    document.querySelectorAll('table.calor').forEach(tabela => {
        const celulas = [...tabela.querySelectorAll('td[data-v]')];
        const valores = celulas.map(c => Number(c.dataset.v));
        const maior = Math.max(1, ...valores);
        celulas.forEach((c, i) => {
            const intensidade = valores[i] / maior;
            c.style.background = `rgba(31, 111, 92, ${0.06 + intensidade * 0.8})`;
            c.style.color = intensidade > 0.55 ? '#fff' : '';
        });
    });

    // Botão de preencher cartão de teste.
    document.querySelectorAll('[data-cartao-teste]').forEach(botao => {
        botao.addEventListener('click', () => {
            const bloco = botao.closest('form');
            bloco.querySelector('[name="pagamento.titular"]').value = 'Cliente Teste';
            bloco.querySelector('[name="pagamento.numeroCartao"]').value = '4242 4242 4242 4242';
            bloco.querySelector('[name="pagamento.validade"]').value = '12/30';
            bloco.querySelector('[name="pagamento.cvv"]').value = '123';
        });
    });
});
