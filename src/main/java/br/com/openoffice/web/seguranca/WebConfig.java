package br.com.openoffice.web.seguranca;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Liga o interceptor de login às áreas restritas do sistema. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacaoInterceptor interceptor;

    public WebConfig(AutenticacaoInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/membro/**", "/admin/**");
    }
}
