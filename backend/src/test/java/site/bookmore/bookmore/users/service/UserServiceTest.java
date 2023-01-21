package site.bookmore.bookmore.users.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.bookmore.bookmore.common.exception.AbstractAppException;
import site.bookmore.bookmore.common.exception.conflict.DuplicateEmailException;
import site.bookmore.bookmore.common.exception.conflict.DuplicateNicknameException;
import site.bookmore.bookmore.common.exception.not_found.UserNotFoundException;
import site.bookmore.bookmore.common.exception.unauthorized.InvalidPasswordException;
import site.bookmore.bookmore.common.exception.unauthorized.InvalidTokenException;
import site.bookmore.bookmore.security.provider.JwtProvider;
import site.bookmore.bookmore.users.dto.UserJoinRequest;
import site.bookmore.bookmore.users.dto.UserLoginRequest;
import site.bookmore.bookmore.users.dto.UserUpdateRequest;
import site.bookmore.bookmore.users.entity.User;
import site.bookmore.bookmore.users.repositroy.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static site.bookmore.bookmore.common.exception.ErrorCode.*;


class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    private final JwtProvider jwtProvider = mock(JwtProvider.class);

    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final UserService userService = new UserService(passwordEncoder, jwtProvider, userRepository);

    private final User user = User.builder()
            .id(0L)
            .email("email")
            .nickname("nickname")
            .password("password")
            .build();


    @Test
    @DisplayName("회원가입 - 성공")
    void join_success() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(userRepository.findByNickname(user.getNickname()))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        Assertions.assertDoesNotThrow(() -> userService.join(new UserJoinRequest()));

    }


    @Test
    @DisplayName("회원가입 - 실패(이메일 중복")
    void join_fail_1() {

        // 가입을 하려고 했어 파인드바이 이메일 저 유저가 나온다
        when(userRepository.findByEmail(user.getEmail())) //
                .thenReturn(Optional.of(user)); // 스터빙 이미로 전환

        AbstractAppException abstractAppException = assertThrows(DuplicateEmailException.class, () -> {
            userService.join(new UserJoinRequest(user.getEmail(), "password", "nickname", LocalDate.of(2022, 04, 03)));
        });

        assertThat(abstractAppException.getErrorCode()).isEqualTo(DUPLICATED_EMAIL);
    }

    @Test
    @DisplayName("회원가입 - 실패(닉네임 중복)")
    void join_fail_2() {

        // 가입을 하려고 했어 파인드바이 이메일 저 유저가 나온다
        when(userRepository.findByNickname(user.getNickname())) //
                .thenReturn(Optional.of(user)); // 스터빙 이미로 전환

        AbstractAppException abstractAppException = assertThrows(DuplicateNicknameException.class, () -> {
            userService.join(new UserJoinRequest(user.getEmail(), "password", "nickname", LocalDate.of(2022, 04, 03)));
        });

        assertThat(abstractAppException.getErrorCode()).isEqualTo(DUPLICATED_NICKNAME);
    }

    @Test
    @DisplayName("로그인 - 성공")
    void login_success() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(!passwordEncoder.matches(user.getPassword(), "password"))
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> userService.login(new UserLoginRequest(user.getEmail(), user.getPassword())));
    }

    @Test
    @DisplayName("로그인 - 실패(사용자 찾을 수 없음)")
    void login_fail_1() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        AbstractAppException abstractAppException = assertThrows(UserNotFoundException.class, () -> {
            userService.login(new UserLoginRequest("test@gmail.com", user.getPassword()));
        });

        assertThat(abstractAppException.getErrorCode()).isEqualTo(USER_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 - 실패(비밀번호 틀림)")
    void login_fail_2() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(!passwordEncoder.matches(user.getPassword(), "password"))
                .thenReturn(true);

        AbstractAppException abstractAppException = assertThrows(InvalidPasswordException.class, () -> {
            userService.login(new UserLoginRequest(user.getEmail(), "failPassword"));
        });

        assertThat(abstractAppException.getErrorCode()).isEqualTo(INVALID_PASSWORD);
    }


    @Test
    @DisplayName("회원 정보 수정 - 성공")
    void infoUpdate_success() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        Assertions.assertDoesNotThrow(() -> userService.infoUpdate(user.getEmail(), 0L, new UserUpdateRequest("updatePw", null, null)));
    }

    @Test
    @DisplayName("회원 정보 수정 - 실패(잘못된 토큰)")
    void infoUpdate_fail_1() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        AbstractAppException abstractAppException = assertThrows(InvalidTokenException.class, () -> {
            userService.infoUpdate(user.getEmail(), 1L, new UserUpdateRequest("updatePw", null, null));
        });

        assertThat(abstractAppException.getErrorCode()).isEqualTo(INVALID_TOKEN);
    }


    @Test
    @DisplayName("회원 정보 수정 - 실패(중복된 이름)")
    void infoUpdate_fail_2() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(userRepository.findByNickname(user.getNickname()))
                .thenReturn(Optional.of(user));

        AbstractAppException abstractAppException = assertThrows(DuplicateNicknameException.class, () -> {
            userService.infoUpdate(user.getEmail(), 0L, new UserUpdateRequest("updatePw", "nickname", null));
        });

        assertThat(abstractAppException.getErrorCode()).isEqualTo(DUPLICATED_NICKNAME);
    }

}