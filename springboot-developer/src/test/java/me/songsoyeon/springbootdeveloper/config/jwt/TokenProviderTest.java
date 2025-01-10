package me.songsoyeon.springbootdeveloper.config.jwt;

import io.jsonwebtoken.Jwts;
import me.songsoyeon.springbootdeveloper.domain.User;
import me.songsoyeon.springbootdeveloper.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TokenProviderTest {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserRepository userRepository;

    /*
    generateToken() 검증 테스트
    given : 토큰에 유저 정보를 추가하기 위한 테스트 유저 생성
    when : 토큰 제공자의 generateToken() 메서드를 호출해 토큰 생성
    then : jjwt 라이브러리를 사용해 토큰 복호화. 토큰을 만들 때 클레임으로 넣어둔 id 값이 given 절에서 만든 유저 ID와 동일한지 확인
    */
    @DisplayName("generateToken(): 유저 정보와 만료 기간을 전달해 토큰을 만들 수 있다.")
    @Test
    void generateToken() {
        // given
        User testUser = userRepository.save(User.builder()
                .email("user@email.com")
                .password("test")
                .build());

        // when
        String token = tokenProvider.generateToken(testUser, Duration.ofDays(14));

        // then
        Long userId = Jwts.parser()
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody()
                .get("id", Long.class);

        assertThat(userId).isEqualTo(testUser.getId());
    }

    /*
    validToken() 검증 테스트
    given : jjwt 라이브러리를 사용해 이미 만료된 토큰 생성
    when : 토큰 제공자의 validToken() 메서드를 호출해 유효한 토큰인지 검증한 뒤 결과값 반환
    then : 반환값이 false(유효한 토큰이 아님)인 것을 확인
    */
    @DisplayName("validToken(): 만료된 토큰인 때에 유효성 검증에 실패한다.")
    @Test
    void validToken_invalidToken() {
        // given
        String token = JwtFactory.builder()
                .expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis()))
                .build()
                .createToken(jwtProperties);

        // when
        boolean result = tokenProvider.validToken(token);

        // then
        assertThat(result).isFalse();
    }

    /*
    validToken() 검증 테스트
    given : jjwt 라이브러리를 사용해 만료되지 않은 토큰 생성
    when : 토큰 제공자의 validToken() 메서드를 호출해 유효한 토큰인지 검증한 뒤 결과값 반환
    then : 반환값이 true(유효한 토큰)인 것을 확인
    */
    @DisplayName("validToken(): 유효한 토큰인 때에 유효성 검증에 성공한다.")
    @Test
    void validToken_validToken() {
        // given
        String token = JwtFactory.withDefaultValues()
                .createToken(jwtProperties);

        // when
        boolean result = tokenProvider.validToken(token);

        // then
        assertThat(result).isTrue();
    }

    /*
    getAuthentication() 검증 테스트
    given : jjwt 라이브러리를 사용해 토큰 생성. 토큰 제목인 subject 는 "user@email.com" 사용
    when : 토큰 제공자의 getAuthentication() 메서드를 호출해 인증 객체 반환
    then : 반환받은 인증 객체의 유저 이름을 가져와 given절에서 설정한 subject 값과 같은지 확인.
    */
    @DisplayName("getAuthentication(): 토큰 기반으로 인증 정보를 가져올 수 있다.")
    @Test
    void getAuthentication() {
        // given
        String userEmail = "user@email.com";
        String token = JwtFactory.builder()
                .subject(userEmail)
                .build()
                .createToken(jwtProperties);

        // when
        Authentication authentication = tokenProvider.getAuthentication(token);

        // then
        assertThat(((UserDetails) authentication.getPrincipal()).getUsername()).isEqualTo(userEmail);
    }

    /*
    getUserId() 검증 테스트
    given : jjwt 라이브러리를 사용해 토큰 생성. 키는 "id", 값은 1이라는 유저 ID 클레임 추가
    when : 토큰 제공자의 getUserId() 메서드를 호출해 유저 ID 반환
    then : 반환받은 유저 ID가 given절에서 설정한 유저 ID값인 1과 같은지 확인
    */
    @DisplayName("getUserId(): 토큰으로 유저 ID를 가져올 수 있다.")
    @Test
    void getUserId() {
        // given
        Long userId = 1L;
        String token = JwtFactory.builder()
                .claims(Map.of("id", userId))
                .build()
                .createToken(jwtProperties);

        // when
        Long userIdByToken = tokenProvider.getUserId(token);

        // then
        assertThat(userIdByToken).isEqualTo(userId);
    }

}
