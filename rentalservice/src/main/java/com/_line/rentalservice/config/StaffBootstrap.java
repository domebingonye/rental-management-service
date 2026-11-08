package com._line.rentalservice.config;

import com._line.rentalservice.dto.request.SignupRequest;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.service.SystemUserDaoService;
import com._line.rentalservice.service.SystemUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Staff cannot self-register, so the first staff account is created on startup
 * (only when no staff user exists yet). Credentials come from configuration / env vars.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StaffBootstrap implements ApplicationRunner {
    private final SystemUserDaoService systemUserDaoService;
    private final SystemUserService systemUserService;
    private final PropsReader propsReader;

    @Override
    public void run(ApplicationArguments args) {
        if (systemUserDaoService.roleExists(UserRoleType.STAFF)) return;

        SignupRequest request = new SignupRequest();
        request.setUsername(propsReader.getBootstrapStaffUsername());
        request.setEmail(propsReader.getBootstrapStaffEmail());
        request.setPassword(propsReader.getBootstrapStaffPassword());
        request.setFirstName("System");
        request.setSurname("Administrator");
        systemUserService.onboardStaff(request);
        log.warn("Created bootstrap staff user '{}'. Change its password before going live.", request.getUsername());
    }
}
