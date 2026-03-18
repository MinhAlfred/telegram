package thitkho.wsservice.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeignInternalInterceptor implements RequestInterceptor {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Secret", internalSecret);
    }
}