package site.bookmore.bookmore.challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.bookmore.bookmore.challenge.dto.ChallengeDetailResponse;
import site.bookmore.bookmore.challenge.dto.ChallengeRequest;
import site.bookmore.bookmore.challenge.dto.ChallengeResponse;
import site.bookmore.bookmore.challenge.service.ChallengeService;
import site.bookmore.bookmore.common.exception.ErrorCode;
import site.bookmore.bookmore.common.exception.forbidden.InvalidPermissionException;
import site.bookmore.bookmore.common.exception.internal_server_error.DatabaseException;
import site.bookmore.bookmore.common.exception.not_found.ReviewNotFoundException;
import site.bookmore.bookmore.common.exception.not_found.UserNotFoundException;
import site.bookmore.bookmore.common.exception.unauthorized.InvalidPasswordException;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ChallengeController.class, excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class})
@MockBean(JpaMetamodelMappingContext.class)
class ChallengeControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    ChallengeService challengeService;

    @Autowired
    ObjectMapper objectMapper;


    @DisplayName("제목에 빈문자열 전송 시 에러 발생")
    @Test
    @WithMockUser
    public void notNullTest() throws Exception {
        ChallengeRequest challengeRequest = new ChallengeRequest("", "description", LocalDate.of(2023, 10, 11), 0);
        when(challengeService.add(anyString(), any(ChallengeRequest.class))).thenReturn(new ChallengeResponse("message", 1L));

        mockMvc.perform(post("/api/v1/challenges")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("ERROR"));
    }



    @DisplayName("challenge 1개 작성 성공")
    @Test
    @WithMockUser
    public void Test() throws Exception {
        ChallengeRequest challengeRequest = ChallengeRequest.builder()
                .title("title")
                .description("description")
                .progress(0)
                .deadline(LocalDate.of(2023, 5, 30))
                .build();
        when(challengeService.add(anyString(), any(ChallengeRequest.class))).thenReturn(new ChallengeResponse("message", 1L));

        mockMvc.perform(post("/api/v1/challenges")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(status().isOk());
    }

    @DisplayName("challenge 1개 작성 실패 : 권한 없음")
    @Test
    @WithAnonymousUser
    public void Test2() throws Exception {
        ChallengeRequest challengeRequest = ChallengeRequest.builder().title("title").description("description").build();
        when(challengeService.add(anyString(), any(ChallengeRequest.class))).thenThrow(new InvalidPermissionException());

        mockMvc.perform(post("/api/v1/challenges")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("challenge 1개 수정 성공")
    @Test
    @WithMockUser
    public void Test3() throws Exception {
        ChallengeRequest challengeRequest = ChallengeRequest.builder()
                .title("title")
                .description("description")
                .deadline(LocalDate.parse("2300-01-01"))
                .progress(0)
                .build();

        when(challengeService.modify(any(), any(), any())).thenReturn(new ChallengeResponse("Message", 1L));

        mockMvc.perform(patch("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(jsonPath("$.result.message").exists())
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser // 인증 되지 않은 상태
    @DisplayName("challenge 1개 수정 실패 : 권한 없음")
    void modify_fail1() throws Exception {
        ChallengeRequest challengeRequest = ChallengeRequest.builder().title("title").description("description").build();

        when(challengeService.modify(any(), any(), any()))
                .thenThrow(new InvalidPasswordException());

        mockMvc.perform(put("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("challenge 1개 수정 실패 : 선택 게시글 불일치")
    void modify_fail2() throws Exception {

        ChallengeRequest challengeRequest = ChallengeRequest.builder()
                .title("title")
                .description("description")
                .deadline(LocalDate.parse("2300-01-01"))
                .progress(0)
                .build();
        when(challengeService.modify(any(), any(), any()))
                .thenThrow(new ReviewNotFoundException());

        mockMvc.perform(patch("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(status().is(ErrorCode.REVIEW_NOT_FOUND.getHttpStatus().value()));
    }

    @Test
    @WithMockUser   // 인증된 상태
    @DisplayName("challenge 1개 수정 실패 : 작성자 불일치")
    void modify_fail3() throws Exception {


        ChallengeRequest challengeRequest = ChallengeRequest.builder()
                .title("title")
                .description("description")
                .deadline(LocalDate.parse("2300-01-01"))
                .progress(0)
                .build();

        when(challengeService.modify(any(), any(), any()))
                .thenThrow(new UserNotFoundException());

        mockMvc.perform(patch("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(status().is(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()));
    }

    @Test
    @WithMockUser   // 인증된 상태
    @DisplayName("challenge 1개 수정 실패 : 데이터베이스 에러")
    void modify_fail4() throws Exception {

        ChallengeRequest challengeRequest = ChallengeRequest.builder()
                .title("title")
                .description("description")
                .deadline(LocalDate.parse("2300-01-01"))
                .progress(0)
                .build();

        when(challengeService.modify(any(), any(), any()))
                .thenThrow(new DatabaseException());

        mockMvc.perform(patch("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(challengeRequest)))
                .andExpect(status().is(ErrorCode.DATABASE_ERROR.getHttpStatus().value()));
    }


    @Test
    @WithMockUser   // 인증된 상태
    @DisplayName("challenge 1개 삭제 성공")
    void delete_success() throws Exception {
        when(challengeService.delete(any(), any())).thenReturn(new ChallengeResponse("message", 1L));

        mockMvc.perform(delete("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result.message").exists())
                .andExpect(jsonPath("$.resultCode").exists())
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser // 인증 된지 않은 상태
    @DisplayName("challenge 1개 삭제 실패 : 권한없음")
    void delete_fail1() throws Exception {

        when(challengeService.delete(any(), any()))
                .thenThrow(new InvalidPermissionException());

        mockMvc.perform(delete("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser   // 인증된 상태
    @DisplayName("challenge 1개 삭제 실패 : 내용 불일치")
    void delete_fail2() throws Exception {

        when(challengeService.delete(any(), any()))
                .thenThrow(new ReviewNotFoundException());

        mockMvc.perform(delete("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(ErrorCode.REVIEW_NOT_FOUND.getHttpStatus().value()));
    }


    @Test
    @WithMockUser   // 인증된 상태
    @DisplayName("challenge 1개 삭제 실패 : 작성자 불일치")
    void delete_fail3() throws Exception {

        when(challengeService.delete(any(), any()))
                .thenThrow(new UserNotFoundException());

        mockMvc.perform(delete("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()));
    }


    @Test
    @WithMockUser   // 인증된 상태
    @DisplayName("challenge 1개 삭제 실패 : 데이터베이스 에러")
    void delete_fail4() throws Exception {

        when(challengeService.delete(any(), any()))
                .thenThrow(new DatabaseException());

        mockMvc.perform(delete("/api/v1/challenges/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(ErrorCode.DATABASE_ERROR.getHttpStatus().value()));
    }

    @Test
    @WithMockUser
    @DisplayName("첼린지 상세 조회 - 성공")
    void challengeDetail_success() throws Exception {
        ChallengeDetailResponse challengeDetailResponse = ChallengeDetailResponse.builder()
                .id(3L)
                .nickname("nickname")
                .title("제목")
                .description("설명")
                .progress(2)
                .completed(true)
                .deadline("2222-01-01")
                .build();

        given(challengeService.get(any(), any())).willReturn(challengeDetailResponse);

        mockMvc.perform(get("/api/v1/challenges/3")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));

        verify(challengeService).get(any(), any());
    }

    @Test
    @WithMockUser   // 인증된 상태
    @DisplayName("첼린지 리스트 조회 - 성공")
    void challengeList_success() throws Exception {

        given(challengeService.list(any(), any())).willReturn(Page.empty());

        mockMvc.perform(get("/api/v1/challenges")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));

        verify(challengeService).list(any(), any());
    }

}