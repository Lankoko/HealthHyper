package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.UserContext;
import com.example.demo.dto.profile.ProfileUpdateRequest;
import com.example.demo.entity.HealthProfile;
import com.example.demo.service.HealthProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3. 用户档案", description = "健康档案 CRUD（Skill: user_profile）")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService profileService;

    @GetMapping("/profile")
    public Result<HealthProfile> getProfile() {
        return Result.ok(profileService.getProfile(UserContext.get()));
    }

    @PutMapping("/profile")
    public Result<HealthProfile> updateProfile(@RequestBody ProfileUpdateRequest req) {
        return Result.ok(profileService.updateProfile(UserContext.get(), req, "user_edit"));
    }
}
