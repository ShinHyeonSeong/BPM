package com.example.bpm.controller;

import com.example.bpm.dto.*;
import com.example.bpm.entity.UserEntity;
import com.example.bpm.entity.WorkEntity;
import com.example.bpm.service.*;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
@ToString
@RequiredArgsConstructor
public class ProjectDetailController {
    @Autowired
    private ProjectDetailSerivce projectDetailSerivce;
    @Autowired
    private ProjectSerivce projectSerivce;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private UserService userService;
    @Autowired
    private CalendarService calendarService;
    @Autowired
    private ExceptionService exceptionService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private HttpSession session;


    // 생각을 해보니 말야 매번 세션 호출하는것보다는 그냥 따로 메서드 만드는게 훨씬 효율이 좋을듯. 편하기도 하고
    public UserDto getSessionUser() {
        UserDto currentUser = (UserDto) session.getAttribute("userInfo");
        return currentUser;
    }

    public ProjectDto getSessionProject() {
        ProjectDto currentProject = (ProjectDto) session.getAttribute("currentProject");
        return currentProject;
    }

    // 세션 유저 권한 확인
    public Long getSessionAuth() {
        Long auth = (Long) session.getAttribute("auth");
        return auth;
    }

    public void checkAuth() {
        ProjectDto projectDto = getSessionProject();
        UserDto userDto = getSessionUser();
        Long auth = userService.checkRole(projectDto.getProjectId(), userDto.getUuid());
        session.setAttribute("auth", auth);
    }

    // 프로젝트 메인 창 매핑
    @GetMapping("/project/main")
    public String goProjectMain(Model model) {
        ProjectDto sessionProject = getSessionProject();
        UserDto userDto = getSessionUser();
        List<HeadDto> headDtoList = projectDetailSerivce.selectAllHead(sessionProject);
        List<WorkDto> userWorkDtoList = projectDetailSerivce.selectAllWorkForUser(userDto);
        model.addAttribute("headDtoList", headDtoList);
        model.addAttribute("userWorkDtoList", userWorkDtoList);
        return "redirect:/project/" + sessionProject.getProjectId();
    }

    /* - - - - 목표 관련 메서드- - - -*/
    // 목표 리스트창 매핑
    @GetMapping("/project/goals")
    public String goals(Model model) {
        ProjectDto currentProject = getSessionProject();
        List<HeadDto> headDtoList = projectDetailSerivce.selectAllHead(currentProject);
        List<DetailDto> detailDtoList = projectDetailSerivce.selectAllDetailForProject(currentProject);
        model.addAttribute("detailDtoList", detailDtoList);
        Long auth = getSessionAuth();
        model.addAttribute("headDtoList", headDtoList);
        model.addAttribute("auth", auth);
        return "goal";
    }

    // 상위 목표 생성 진입
    @GetMapping("/project/head/create")
    public String goHeadDetail(Model model, @RequestParam(value = "message", required = false) String message) {
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "head-create";
    }

    // 하위 목표 생성 main
    @GetMapping("/project/detail/create")
    public String goCreateDetail(Model model, @RequestParam(value = "message", required = false) String message) {
        ProjectDto currentProject = getSessionProject();
        List<HeadDto> headDtoList = projectDetailSerivce.selectAllHead(currentProject);
        model.addAttribute("headDtoList", headDtoList);
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "detail-create";
    }

    // head 생성 메서드
    @PostMapping("/project/goal/createHead")
    public String createGoal(@RequestParam(value = "title") String title,
                             @RequestParam(value = "startDay") String startDay,
                             @RequestParam(value = "deadline") String deadline,
                             @RequestParam(value = "discription") String discription,
                             Model model) {
        ProjectDto currentProject = getSessionProject();
        log.info("목표 생성 컨트롤러 작동, ");
        String message = exceptionService.headErrorCheck(currentProject, title, startDay, deadline);
        log.info("head 생성 예외 처리 검사");
        if (message != null) {
            log.info("예외 처리 결과 : " + message);
            model.addAttribute("message", message);
            return "head-create";
        }
        HeadDto createHeadDto = projectDetailSerivce.createHead(title, startDay, deadline, discription, currentProject);
        return "redirect:/project/goals";
    }

    // 디테일 생성 메서드
    @PostMapping("/project/goal/createDetail")
    public String createGoal(@RequestParam(value = "title") String title,
                             @RequestParam(value = "startDay") String startDay,
                             @RequestParam(value = "deadline") String deadline,
                             @RequestParam(value = "discription") String discription,
                             @RequestParam(value = "headId") Long headId,
                             RedirectAttributes rttr,
                             Model model) {
        ProjectDto currentProject = getSessionProject();
        log.info("목표 생성 컨트롤러 작동, ");
        String message = exceptionService.detailErrorCheck(title, startDay, deadline, headId);
        if (message != null) {
            log.info("예외 처리 결과 : " + message);
            rttr.addFlashAttribute("message", message);
            return "redirect:/project/detail/create";
        }
        if (headId == 0) {
            log.info("headDto == null");
            HeadDto createHeadDto = projectDetailSerivce.createHead(title, startDay, deadline, discription, currentProject);
        } else if (headId != 0) {
            HeadDto headDto = projectDetailSerivce.selectHead(headId);
            log.info("headDto.get" + headDto.getHeadId());
            DetailDto createDetailDto = projectDetailSerivce.createDetail(title, startDay, deadline, discription, headDto, currentProject);
        }
        return "redirect:/project/goals";
    }

    // 목표 상세창 이동 메서드
    @RequestMapping("/project/goal/headView/{id}")
    public String goHeadView(@PathVariable("id") Long id, Model model) {
        HeadDto headDto = projectDetailSerivce.selectHead(id);
        List<DetailDto> detailDtoList = projectDetailSerivce.selectAllDetailForHead(headDto);
        Long auth = getSessionAuth();
        model.addAttribute("headDto", headDto);
        model.addAttribute("connectDetailList", detailDtoList);
        model.addAttribute("auth", auth);
        return "headView";
    }

    // 디테일 상세창 이동 메서드
    @RequestMapping("/project/goal/detailView/{id}")
    public String goDetailView(@PathVariable("id") Long id, Model model) {
        DetailDto detailDto = projectDetailSerivce.selectDetail(id);
        HeadDto headDto = projectDetailSerivce.selectHead(detailDto.getHeadIdToDetail().getHeadId());
        List<WorkDto> workDtoList = projectDetailSerivce.selectAllWorkForDetail(id);
        Map<WorkDto, UserDto> userWorkMap = projectDetailSerivce.selectAllUserWorkForWorkList(workDtoList);
        Long auth = getSessionAuth();
        model.addAttribute("detailDto", detailDto);
        model.addAttribute("headDto", headDto);
        model.addAttribute("workDtoList", workDtoList);
        model.addAttribute("userWorkMap", userWorkMap);
        model.addAttribute("auth", auth);
        return "detailView";
    }

    // head 수정창 매핑 메서드
    @RequestMapping("/project/goal/head/edit/{id}")
    public String goEditHead(@PathVariable("id") Long headId,
                             @RequestParam(value = "message", required = false) String message,
                             Model model) {
        if (message != null) {
            model.addAttribute("message", message);
        }
        HeadDto headDto = projectDetailSerivce.selectHead(headId);
        model.addAttribute("headDto", headDto);
        return "headEdit";
    }

    // detail 수정창 매핑 메서드
    @RequestMapping("/project/goal/detail/edit/{id}")
    public String goEditDetail(@PathVariable("id") Long detailId,
                               @RequestParam(value = "message", required = false) String message,
                               Model model) {

        DetailDto detailDto = projectDetailSerivce.selectDetail(detailId);
        ProjectDto currentProject = getSessionProject();
        List<HeadDto> headDtoList = projectDetailSerivce.selectAllHead(currentProject);
        if (message != null) {
            model.addAttribute("message", message);
        }
        model.addAttribute("detailDto", detailDto);
        model.addAttribute("headDtoList", headDtoList);
        return "detailEdit";
    }

    // head 수정 실행 메서드
    @PostMapping("/project/head/edit")
    public String editHead(@RequestParam(value = "title") String title,
                           @RequestParam(value = "startDay") String startDay,
                           @RequestParam(value = "deadline") String deadline,
                           @RequestParam(value = "discription") String discription,
                           @RequestParam(value = "headId") Long headId,
                           RedirectAttributes rttr,
                           Model model) {
        ProjectDto currentProject = getSessionProject();
        String message = exceptionService.headEditErrorCheck(currentProject, title, startDay, deadline);
        if (message != null) {
            log.info("예외 처리 결과 : " + message);
            rttr.addFlashAttribute("message", message);
            return "redirect:/project/goal/head/edit/" + headId;
        }
        HeadDto headDto = projectDetailSerivce.editHead(title, startDay, deadline, discription, headId);
        return "redirect:/project/goals";
    }

    // detail 수정 실행 메서드
    @PostMapping("/project/detail/edit")
    public String editDetail(@RequestParam(value = "title") String title,
                             @RequestParam(value = "startDay") String startDay,
                             @RequestParam(value = "deadline") String deadline,
                             @RequestParam(value = "discription") String discription,
                             @RequestParam(value = "headId") Long headId,
                             @RequestParam(value = "detailId") Long detailId,
                             RedirectAttributes rttr,
                             Model model) {
        String message = exceptionService.detailEditErrorCheck(title, startDay, deadline, headId);
        if (message != null) {
            log.info("예외 처리 결과 : " + message);
            rttr.addFlashAttribute("message", message);
            return "redirect:/project/goal/detail/edit/" + detailId;
        }
        DetailDto detailDto = projectDetailSerivce.editDetail(title, startDay, deadline, discription, headId, detailId);
        return "redirect:/project/goals";
    }
    /* - - - - 목표 관련 메서드 끝 - - - -*/


    /* - - - - 작업 관련 메서드- - - -*/
    // work 목록 진입 매핑
    @GetMapping("/project/works")
    public String works(Model model) {
        UserDto currentUser = getSessionUser();
        ProjectDto currentProject = getSessionProject();
        List<WorkDto> sessionUserWorkDtoList = projectDetailSerivce.selectAllWorkForUser(currentUser);
        List<WorkDto> userWorkDtoList = new ArrayList<>();
        log.info("현재 진입 프로젝트 : " + currentProject.getProjectId());

        for (WorkDto workDto : sessionUserWorkDtoList) {
            log.info("session user workDto list에서 현재 프로젝트 id와 같은 dto 리스트에 삽입 " + currentProject.getProjectId());
            if (workDto.getProjectIdToWork().getProjectId() == currentProject.getProjectId()) {
                log.info("리스트 삽입 : " + workDto.getWorkId() + ".");
                log.info("해당 dto의 projectId : " + workDto.getProjectIdToWork().getProjectId() + ".");
                userWorkDtoList.add(workDto);
            }
        }
        List<WorkDto> projectWorkDtoList = projectDetailSerivce.selectAllWorkForProject(currentProject);
        Long auth = getSessionAuth();
        if (projectWorkDtoList != null) {
            model.addAttribute("projectWorkDtoList", projectWorkDtoList);
        }
        model.addAttribute("userWorkDtoList", userWorkDtoList);
        model.addAttribute("auth", auth);
        return "work";
    }

    // work 생성창 진입 메서드
    @GetMapping("/project/work/create")
    public String goCreateWork(Model model, @RequestParam(value = "message", required = false) String message) {
        ProjectDto currentProject = getSessionProject();
//        List<UserDto> userDtoList = userService.searchUserToProject(currentProject.getProjectId());
        List<DetailDto> detailDtoList = projectDetailSerivce.selectAllDetailForProject(currentProject);
        if (message != null) {
            model.addAttribute("message", message);
        }
        model.addAttribute("userDtoList", userDtoList);
        model.addAttribute("detailDtoList", detailDtoList);
        return "workCreate";
    }

    // work 생성 메서드
    @PostMapping("/project/work/createWork")
    public String createWork(@RequestParam("title") String title,
                             @RequestParam("discription") String discription,
                             @RequestParam("startDay") String startDay,
                             @RequestParam("deadline") String deadline,
                             @RequestParam("connectDetail") Long detailId,
                             @RequestParam("chargeUsers") List<String> chargeUsers,
                             RedirectAttributes rttr) {
        ProjectDto currentProject = getSessionProject();
        String message = exceptionService.workEditErrorCheck(startDay, deadline, detailId);
        if (message != null) {
            log.info("예외 처리 결과 : " + message);
            rttr.addFlashAttribute("message", message);
            return "redirect:/project/work/create";
        }
        DetailDto connectDetail = projectDetailSerivce.selectDetail(detailId);
        WorkDto createWorkDto = projectDetailSerivce.createWork(title, discription, startDay, deadline,
                connectDetail, currentProject);
//        log.info("작업 생성 메서드 완료, id = " + createWorkDto.getWorkId());
        projectDetailSerivce.addUserWork(createWorkDto, chargeUser);
        return "redirect:/project/works";
    }

    // work 상세창 진입 메서드
    @RequestMapping("/project/work/detail/{id}")
    public String goWorkDetail(@PathVariable("id") Long id, Model model) {
        WorkDto workDto = projectDetailSerivce.selectWork(id);
        UserDto userDto = projectDetailSerivce.selectUserForUserWork(workDto);
        List<DocumentDto> documentDtoList = documentService.getDocumentByWorkId(id);
        List<WorkCommentDto> commentDtoList = projectDetailSerivce.findByComment(id);
        if (commentDtoList.isEmpty()) {
            int i = 0;
            model.addAttribute("listNum", i);
            model.addAttribute("CommentList", commentDtoList);
        } else {
            int i = commentDtoList.size();
            model.addAttribute("listNum", i);
            model.addAttribute("CommentList", commentDtoList);
        }
        Long auth = getSessionAuth();
        model.addAttribute("auth", auth);
        model.addAttribute("workDto", workDto);
        model.addAttribute("userDto", userDto);
        model.addAttribute("DocumentList", documentDtoList);
        return "workDetail";
    }
    /* - - - - 작업 관련 메서드 끝 - - - -*/

    /* - - - - 삭제 메서드 - - - - */
    @RequestMapping("/project/goal/head/delete/{id}")
    public String deleteHead(@PathVariable("id") Long headId) {
        projectDetailSerivce.deleteHeadEntity(headId);
        return "redirect:/project/goals";
    }

    @RequestMapping("/project/goal/detail/delete/{id}")
    public String deleteDetail(@PathVariable("id") Long detailId) {
        projectDetailSerivce.deleteDetailEntity(detailId);
        return "redirect:/project/goals";
    }

    @RequestMapping("/project/goal/work/delete/{id}")
    public String deleteWork(@PathVariable("id") Long workId) {
        projectDetailSerivce.deleteWorkEntity(workId);
        return "redirect:/project/works";
    }


    /* - - - - 댓글 관련 메서드 - - - -*/
    @PostMapping("/workDetail/addComment")
    public String plusComment(@RequestParam("workId") Long workId,
                              @RequestParam("comment") String comment,
                              HttpSession session, Model model, HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        /* 댓글을 추가 시키는 메서드 */
        WorkDto workDto = projectDetailSerivce.selectWork(workId);
        UserDto nowUser = getSessionUser();
        WorkCommentDto workCommentDto = new WorkCommentDto();
        workCommentDto.setComment(comment);
        workCommentDto.setWorkIdToComment(WorkEntity.toWorkEntity(workDto));
        workCommentDto.setUserIdToComment(UserEntity.toUserEntity(nowUser));
        /* 댓글을 추가 시키는 메서드 끝 */

        /*추가 시킬 댓글 내용과, 현재 documentID 를 같이 넘겨 리턴 값으로 자동 리스트를 뽑아온다*/
        List<WorkCommentDto> list = projectDetailSerivce.plusComment(workCommentDto, workId);
        model.addAttribute("commentList", list);
        return "redirect:" + referer;
    }

    //댓글 수정을 하기 위한 댓글 데이터를 가져오는 메서드 (프론트에서는 댓글을 수정할 수 있는 화면이 필요하다
    @GetMapping("/workDetail/commentUpdate")
    public String updateForm(@RequestParam("commentId") Long commentId, Model model) {
        WorkCommentDto updateComment = projectDetailSerivce.findComment(commentId);
        model.addAttribute("updateComment", updateComment);

        return "";
    }

    @PostMapping("댓글 수정 완료 시")
    public String updateComment(@RequestParam("workId") Long workId,
                                @RequestParam("comment") String comment, HttpSession session, Model model) {

        WorkDto workDto = projectDetailSerivce.selectWork(workId);
        UserDto nowUser = getSessionUser();
        WorkCommentDto workCommentDto = new WorkCommentDto();
        workCommentDto.setComment(comment);
        workCommentDto.setWorkIdToComment(WorkEntity.toWorkEntity(workDto));
        workCommentDto.setUserIdToComment(UserEntity.toUserEntity(nowUser));

        List<WorkCommentDto> list = projectDetailSerivce.plusComment(workCommentDto, workId);
        model.addAttribute("commentList", list);
        return "";
    }

    @RequestMapping("/workDetail/commentDelete/{cid}")
    public String deleteComment(@PathVariable("cid") Long commentId, Model model) {
        WorkCommentDto workCommentDto = projectDetailSerivce.findComment(commentId);
        Long workId = workCommentDto.getWorkIdToComment().getWorkId();
        List<WorkCommentDto> dtoList = projectDetailSerivce.deleteComment(commentId, workId);
        model.addAttribute("CommentList", dtoList);
        return "redirect:/project/work/detail/" + workId;
    }
    /* - - - - 댓글 관련 메서드 끝 - - - -*/

    /* 상태 완료 처리 메서드 */
    @RequestMapping("/project/work/completion/change/{id}")
    public String workCompletionChange(@PathVariable("id") Long workId) {
        WorkDto targetWorkDto = projectDetailSerivce.selectWork(workId);
        WorkDto changeWorkDto = projectDetailSerivce.workCompletionChange(targetWorkDto);
        return "redirect:/project/works";
    }

    @RequestMapping("/project/detail/completion/change/{id}")
    public String detailCompletionChange(@PathVariable("id") Long detailId) {
        DetailDto targetDetailDto = projectDetailSerivce.selectDetail(detailId);
        DetailDto changeDetailDto = projectDetailSerivce.detailCompletionChange(targetDetailDto);
        return "redirect:/project/goals";
    }

    @RequestMapping("/project/head/completion/change/{id}")
    public String headCompletionChange(@PathVariable("id") Long headId) {
        HeadDto targetHeadDto = projectDetailSerivce.selectHead(headId);
        HeadDto changeHeadDto = projectDetailSerivce.headCompletionChange(targetHeadDto);
        return "redirect:/project/goals";
    }

    /*  - - - - - Calendar Controller - - - - - */
    @GetMapping("/project/calender") //기본 페이지 표시
    public String viewCalendar() {

        return "calendar";
    }

    @RequestMapping(value = "/calendar/event", method = {RequestMethod.GET}) //ajax 데이터 전송 URL
    public @ResponseBody List<Map<String, Object>> getEvent() {

        ProjectDto projectDto = getSessionProject();

        return calendarService.getEventList(projectDto.getProjectId());
    }


    /* - - - - - Message Contorller - - - - - - */
    @GetMapping("/recvMessageList")
    public String viewRecvMessage(HttpSession session, Model model) {
        UserDto userDto = getSessionUser();
        ProjectDto projectDto = getSessionProject();
        List<MessageDto> messageDtoList = messageService.selectAllRecv(userDto, projectDto);

        model.addAttribute("List", messageDtoList);
        return "recvMessageList";
    }

    @GetMapping("/sendMessageList")
    public String viewSendMessage(HttpSession session, Model model) {
        UserDto userDto = getSessionUser();
        ProjectDto projectDto = getSessionProject();
        List<MessageDto> messageDtoList = messageService.selectAllSend(userDto, projectDto);

        model.addAttribute("List", messageDtoList);

        return "sendMessageList";
    }

    @GetMapping("/messageForm")
    public String sendMessageForm(Model model) {
        List<UserDto> userDtos = userService.searchUserToProject(getSessionProject().getProjectId());
        model.addAttribute("userList", userDtos);
        return "messageForm";
    }

    @RequestMapping("/message/{id}")
    public String selectMessage(@PathVariable("id") Long id, Model model) {
        MessageDto messageDto = messageService.selectMessage(id);
        model.addAttribute("message", messageDto);

        return "messageDetail";
    }

    @PostMapping("/sendMessage")
    public String sendMessage(@RequestParam(value = "title") String title,
                              @RequestParam(value = "content") String content,
                              @RequestParam(value = "recvName") String name,
                              HttpSession session) {
        log.info(name + "입니다.");
        messageService.sendMessage(title, content, getSessionUser(), name, getSessionProject());
        return "redirect:/sendMessageList";
    }
}

