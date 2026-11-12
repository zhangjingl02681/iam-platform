package com.enterprise.iam.config;

import com.enterprise.iam.identity.domain.IamRole;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.IamUserRole;
import com.enterprise.iam.identity.domain.RoleStatus;
import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.repo.IamRoleRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.repo.IamUserRoleRepository;
import com.enterprise.iam.identity.service.PasswordService;
import com.enterprise.iam.identity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Order(1)
public class DevBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevBootstrap.class);

    private final IamProperties properties;
    private final IamUserRepository userRepository;
    private final IamRoleRepository roleRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final PasswordService passwordService;

    public DevBootstrap(
            IamProperties properties,
            IamUserRepository userRepository,
            IamRoleRepository roleRepository,
            IamUserRoleRepository userRoleRepository,
            PasswordService passwordService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordService = passwordService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        IamProperties.Admin admin = properties.getBootstrap().getAdmin();
        if (!admin.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(admin.getPassword())) {
            log.warn("IAM bootstrap admin is enabled but no password was provided; skipping");
            return;
        }
        IamRole role = roleRepository.findByCode("admin").orElseGet(() -> {
            IamRole created = new IamRole();
            created.setCode("admin");
            created.setName("Administrator");
            created.setDescription("Built-in administrator role");
            created.setStatus(RoleStatus.ACTIVE);
            return roleRepository.save(created);
        });
        String username = UserService.normalizeUsername(admin.getUsername());
        IamUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new IamUser();
            user.setUsername(username);
            user.setDisplayName(StringUtils.hasText(admin.getDisplayName()) ? admin.getDisplayName() : "系统管理员");
            user.setStatus(UserStatus.ACTIVE);
            user = userRepository.save(user);
            passwordService.setPassword(user.getId(), admin.getPassword(), false, false);
            log.info("Bootstrapped IAM admin user '{}'. Change the dev password before any non-dev use.", username);
        } else {
            log.info("IAM admin user '{}' already exists; password was left unchanged.", username);
        }
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            IamUserRole assignment = new IamUserRole();
            assignment.setUserId(user.getId());
            assignment.setRoleId(role.getId());
            userRoleRepository.save(assignment);
        }
    }
}
