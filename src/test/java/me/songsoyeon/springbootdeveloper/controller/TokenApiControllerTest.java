package me.songsoyeon.springbootdeveloper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.songsoyeon.springbootdeveloper.config.jwt.JwtFactory;
import me.songsoyeon.springbootdeveloper.config.jwt.JwtProperties;
import me.songsoyeon.springbootdeveloper.domain.RefreshToken;
import me.songsoyeon.springbootdeveloper.domain.User;
import me.songsoyeon.springbootdeveloper.dto.CreateAccessTokenRequest;
import me.songsoyeon.springbootdeveloper.repository.RefreshTokenRepository;
import me.songsoyeon.springbootdeveloper.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TokenApiControllerTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    JwtProperties jwtProperties;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    User user;

    @BeforeEach
    public void mockMvcSetUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();
        userRepository.deleteAll();
    }

    @BeforeEach
    public void setSecurityContext() {
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
    }

    /*
    given : 테스트 유저를 생성하고, jjwt 라이브러리를 이용해 리프레시 토큰을 만들어 데이터베이스에 저장. 토큰 생성 API의 요청 본문에 리프레시 토큰을 포함하여 요청 객체 생성
    when : 토큰 추가 API에 요청을 보냄. given 절에서 미리 만들어둔 객체를 본문에 함꼐 보냄.
    then : 응답코드가 201 Created인지 확인하고 응답으로 온 액세스 토큰이 비어 있지 않은지 확인.
    */
    @DisplayName("createNewAccessToken: 새로운 액세스 토큰을 발급한다.")
    @Test
    public void createNewAccessToken() throws Exception {
        //given
        final String url = "/api/token";

        User testUser = userRepository.save(User.builder()
                .email("user@email.com")
                .password("test")
                .build());

        String refreshToken = JwtFactory.builder()
                .claims(Map.of("id", testUser.getId()))
                .build()
                .createToken(jwtProperties);

        refreshTokenRepository.save(new RefreshToken(testUser.getId(), refreshToken));

        CreateAccessTokenRequest request = new CreateAccessTokenRequest();
        request.setRefreshToken(refreshToken);
        final String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(requestBody));

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

    }

    @DisplayName("deleteRefreshToken: 리프레시 토큰을 삭제한다.")
    @Test
    public void deleteRefreshToken() throws Exception {
        // given
        final String url = "/api/refresh-token";
        String refreshToken = createRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, refreshToken, user.getAuthorities()));

        // when
        ResultActions resultActions = mockMvc.perform(delete(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE));

        // then
        resultActions
                .andExpect(status().isOk());

        assertThat(refreshTokenRepository.findByRefreshToken(refreshToken)).isEmpty();
    }

    private String createRefreshToken() {
        return JwtFactory.builder()
                .claims(Map.of("id", user.getId()))
                .build()
                .createToken(jwtProperties);
    }

}