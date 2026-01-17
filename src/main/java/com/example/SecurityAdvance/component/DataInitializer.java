package com.example.SecurityAdvance.component;

import com.example.SecurityAdvance.entities.Role;
import com.example.SecurityAdvance.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createRoleIfNotExists("USER", "Người dùng thông thường - quyền cơ bản");
        createRoleIfNotExists("ADMIN", "Quản trị viên - toàn quyền hệ thống");
        createRoleIfNotExists("MODERATOR", "Người kiểm duyệt - quản lý nội dung");
    }

    private void createRoleIfNotExists(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();

            roleRepository.save(role);
            log.info("Đã tạo role mặc định: {} (description: {})", name, description);
        } else {
            log.debug("Role {} đã tồn tại, bỏ qua việc tạo mới", name);
        }
    }
}
