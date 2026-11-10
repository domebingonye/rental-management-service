package com.threelinelimited.rentalservice.config;

import com.threelinelimited.rentalservice.dto.request.SignupRequest;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.service.SystemUserDaoService;
import com.threelinelimited.rentalservice.service.SystemUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

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
