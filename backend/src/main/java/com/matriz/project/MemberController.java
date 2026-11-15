package com.matriz.project;

import com.matriz.common.Identifiers;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Members and roles (US-03). The access step has already checked that the caller belongs to the
 * project; the role it found decides what they may do.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
public class MemberController {

    private final MemberService members;

    public MemberController(MemberService members) {
        this.members = members;
    }

    @GetMapping
    public List<MemberResponse> list(@PathVariable long projectId) {
        return members.list(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse add(@PathVariable long projectId,
                              @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role,
                              @RequestBody MemberAddRequest body) {
        return members.add(projectId, role, body);
    }

    @PatchMapping("/{userId}")
    public MemberResponse changeRole(@PathVariable long projectId, @PathVariable String userId,
                                     @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role,
                                     @RequestBody MemberRoleChangeRequest body) {
        return members.changeRole(projectId, role, Identifiers.parse(userId), body);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable long projectId, @PathVariable String userId,
                       @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role) {
        members.remove(projectId, role, Identifiers.parse(userId));
    }
}
