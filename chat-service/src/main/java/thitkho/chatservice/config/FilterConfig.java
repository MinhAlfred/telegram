package thitkho.chatservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import thitkho.chatservice.filter.InternalSecretFilter;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final InternalSecretFilter internalSecretFilter;

    @Bean
    public FilterRegistrationBean<InternalSecretFilter> registerInternalFilter() {
        FilterRegistrationBean<InternalSecretFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(internalSecretFilter);
        bean.addUrlPatterns("/*");
        bean.setOrder(1);
        return bean;
    }
}