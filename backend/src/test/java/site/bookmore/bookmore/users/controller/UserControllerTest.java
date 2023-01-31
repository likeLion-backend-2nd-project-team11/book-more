package site.bookmore.bookmore.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.bookmore.bookmore.common.exception.conflict.DuplicateEmailException;
import site.bookmore.bookmore.common.exception.conflict.DuplicateNicknameException;
import site.bookmore.bookmore.common.exception.not_found.UserNotFoundException;
import site.bookmore.bookmore.common.exception.unauthorized.InvalidPasswordException;
import site.bookmore.bookmore.common.exception.unauthorized.InvalidTokenException;
import site.bookmore.bookmore.users.dto.*;
import site.bookmore.bookmore.users.service.UserService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@WithMockUser
class UserControllerTest {
    @MockBean
    UserService userService;

    @Autowired
    ObjectMapper objectMapper;

    private LocalDate testDate = LocalDate.of(2022, 01, 01);

    @Autowired
    private MockMvc mockMvc;
    UserJoinRequest userJoinRequest = new UserJoinRequest("email@gmail.com", "password", "nickname", testDate);
    UserLoginRequest userLoginRequest = new UserLoginRequest("email@gmail.com", "password");
    UserUpdateRequest userUpdateRequest = new UserUpdateRequest("password1", null, null);


    @Test
    @DisplayName("회원가입 - 성공")
    void join_success() throws Exception {

        UserJoinResponse userJoinResponse = new UserJoinResponse(0L, userJoinRequest.getEmail(), userJoinRequest.getNickname());

        given(userService.join(any(UserJoinRequest.class))).willReturn(userJoinResponse);

        mockMvc.perform(post("/api/v1/users/join")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userJoinRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.result.id").value(0L))
                .andExpect(jsonPath("$.result.email").value("email@gmail.com"))
                .andExpect(jsonPath("$.result.nickname").value("nickname"));

        verify(userService).join(any(UserJoinRequest.class));
    }


    @Test
    @DisplayName("회원가입 - 실패(이메일 중복)")
    void join_fail_1() throws Exception {
        given(userService.join(any(UserJoinRequest.class))).willThrow(new DuplicateEmailException());

        mockMvc.perform(post("/api/v1/users/join")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userJoinRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result.errorCode").value("DUPLICATED_EMAIL"))
                .andExpect(jsonPath("$.result.message").value("이미 사용중인 이메일입니다."));

        verify(userService).join(any(UserJoinRequest.class));
    }

    @Test
    @DisplayName("회원가입 - 실패(닉네임 중복)")
    void join_fail_2() throws Exception {
        given(userService.join(any(UserJoinRequest.class))).willThrow(new DuplicateNicknameException());

        mockMvc.perform(post("/api/v1/users/join")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userJoinRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result.errorCode").value("DUPLICATED_NICKNAME"))
                .andExpect(jsonPath("$.result.message").value("이미 사용중인 닉네임입니다."));

        verify(userService).join(any(UserJoinRequest.class));
    }

    @Test
    @DisplayName("회원가입 - 실패(잘못된 이메일 형식)")
    void join_fail_3() throws Exception {

        UserJoinRequest errorEmailFormat = new UserJoinRequest("email", "password", "nickname", testDate);

        mockMvc.perform(post("/api/v1/users/join")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(errorEmailFormat))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result").value("[email] 올바른 형식의 이메일 주소여야 합니다"));

//        verify(userService).join(any(UserJoinRequest.class));
    }

    @Test
    @DisplayName("로그인 - 성공")
    void login_success() throws Exception {

        UserLoginResponse userLoginResponse = new UserLoginResponse("token");

        given(userService.login(any(UserLoginRequest.class))).willReturn(userLoginResponse);

        mockMvc.perform(post("/api/v1/users/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userLoginRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.result.jwt").value("token"));

        verify(userService).login(any(UserLoginRequest.class));
    }

    @Test
    @DisplayName("로그인 - 실패(패스워드 틀림)")
    void login_fail_1() throws Exception {

        given(userService.login(any(UserLoginRequest.class))).willThrow(new InvalidPasswordException());

        mockMvc.perform(post("/api/v1/users/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userLoginRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result.errorCode").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.result.message").value("잘못된 패스워드입니다."));

        verify(userService).login(any(UserLoginRequest.class));
    }

    @Test
    @DisplayName("로그인 - 실패(이메일 틀림)")
    void login_fail_2() throws Exception {

        given(userService.login(any(UserLoginRequest.class))).willThrow(new UserNotFoundException());

        mockMvc.perform(post("/api/v1/users/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userLoginRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.result.message").value("해당하는 유저를 찾을 수 없습니다."));

        verify(userService).login(any(UserLoginRequest.class));
    }

    @Test
    @DisplayName("회원 정보 수정 - 성공")
    void infoUpdate_success() throws Exception {

        UserResponse userResponse = new UserResponse(0L, "수정 완료 했습니다.");

        given(userService.infoUpdate(any(), any(), any(UserUpdateRequest.class))).willReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/0")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userUpdateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.result.id").value(0L))
                .andExpect(jsonPath("$.result.message").value("수정 완료 했습니다."));

        verify(userService).infoUpdate(any(), any(), any(UserUpdateRequest.class));
    }

    @Test
    @DisplayName("회원 정보 수정 - 실패(nickname 중복)")
    void infoUpdate_fail() throws Exception {


        given(userService.infoUpdate(any(), any(), any(UserUpdateRequest.class))).willThrow(new DuplicateNicknameException());

        mockMvc.perform(post("/api/v1/users/0")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userUpdateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result.errorCode").value("DUPLICATED_NICKNAME"))
                .andExpect(jsonPath("$.result.message").value("이미 사용중인 닉네임입니다."));

        verify(userService).infoUpdate(any(), any(), any(UserUpdateRequest.class));
    }


    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void userDelete_success() throws Exception {

        UserResponse userDeleteResponse = new UserResponse(0L, "회원 탈퇴 완료.");

        given(userService.delete(any(), any())).willReturn(userDeleteResponse);

        mockMvc.perform(delete("/api/v1/users/0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.result.id").value(0L))
                .andExpect(jsonPath("$.result.message").value("회원 탈퇴 완료."));

        verify(userService).delete(any(), any());
    }

    @Test
    @DisplayName("회원 탈퇴 - 실패(토큰이 다를경우)")
    void userDelete_fail() throws Exception {

        given(userService.delete(any(), any())).willThrow(new InvalidTokenException());

        mockMvc.perform(delete("/api/v1/users/0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result.errorCode").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.result.message").value("잘못된 토큰입니다."));


        verify(userService).delete(any(), any());
    }


    @Test
    @DisplayName("회원 검증 - 성공")
    void verify_success() throws Exception {

        UserJoinResponse userJoinResponse = new UserJoinResponse(0L, userJoinRequest.getEmail(), userJoinRequest.getNickname());

        given(userService.verify(any())).willReturn(userJoinResponse);

        mockMvc.perform(post("/api/v1/users/verify")
                        .with(csrf())
                        .content(objectMapper.writeValueAsBytes(userJoinRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.result.id").value(0L))
                .andExpect(jsonPath("$.result.email").value("email@gmail.com"))
                .andExpect(jsonPath("$.result.nickname").value("nickname"));

        verify(userService).verify(any());
    }


    @Test
    @DisplayName("회원 검증 - 실패(토큰이 다를경우)")
    void userVerify_fail() throws Exception {

        given(userService.verify(any())).willThrow(new UserNotFoundException());

        mockMvc.perform(post("/api/v1/users/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("ERROR"))
                .andExpect(jsonPath("$.result.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.result.message").value("해당하는 유저를 찾을 수 없습니다."));

        verify(userService).verify(any());
    }
}