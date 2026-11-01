package br.com.openoffice.repository;

import br.com.openoffice.model.reserva.Reserva;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ReservaRepository extends RepositorioJson<Reserva> {

    public ReservaRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "reservas.json", Reserva.class, "R");
    }

    /** Reservas ativas do espaço que se sobrepõem ao intervalo informado. */
    public List<Reserva> conflitos(String espacoId, LocalDateTime inicio, LocalDateTime fim) {
        return filtrar(r -> r.getEspacoId().equals(espacoId) && r.sobrepoe(inicio, fim));
    }

    public List<Reserva> doMembro(String membroId) {
        return filtrar(r -> membroId.equals(r.getMembroId()));
    }

    public List<Reserva> ativasNoPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return filtrar(r -> r.sobrepoe(inicio, fim));
    }
}
