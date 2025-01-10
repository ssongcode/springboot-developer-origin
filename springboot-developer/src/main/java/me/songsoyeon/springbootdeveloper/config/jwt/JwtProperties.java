package me.songsoyeon.springbootdeveloper.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("jwt")
@Component
@Setter
@Getter
public class JwtProperties {
    private String issuer;
    private String secretKey;
}
