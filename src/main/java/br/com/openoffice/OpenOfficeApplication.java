package br.com.openoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Ponto de entrada: inicia o servidor web em http://localhost:8080. */
@SpringBootApplication
public class OpenOfficeApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenOfficeApplication.class, args);
    }
}
