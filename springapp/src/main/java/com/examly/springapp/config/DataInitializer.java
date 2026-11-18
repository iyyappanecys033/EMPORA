package com.examly.springapp.config;

import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.model.Profile;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.repository.ProfileRepo;
import com.examly.springapp.repository.RoleRepo;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepo roleRepo;
    private final UserAccountRepo userAccountRepo;
    private final UserRoleMappingRepo userRoleMappingRepo;
    private final ProfileRepo profileRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepo roleRepo,
                           UserAccountRepo userAccountRepo,
                           UserRoleMappingRepo userRoleMappingRepo,
                           ProfileRepo profileRepo,
                           PasswordEncoder passwordEncoder) {
        this.roleRepo = roleRepo;
        this.userAccountRepo = userAccountRepo;
        this.userRoleMappingRepo = userRoleMappingRepo;
        this.profileRepo = profileRepo;
        this.passwordEncoder = passwordEncoder;
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepo.findByRoleName(roleName).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(roleName);
            return roleRepo.save(r);
        });
    }

    private void seedEmployee(String username, String rawPassword, String email, String phone,
                              Role role, String empId, String firstName, String lastName,
                              String department, String designation, Double salary,
                              LocalDate joiningDate, String reportingManager, String address, String gender) {
        if (userAccountRepo.findByUsername(username).isEmpty()) {
            UserAccount account = new UserAccount();
            account.setUsername(username);
            account.setPasswordHash(passwordEncoder.encode(rawPassword));
            account.setEmail(email);
            account.setPhone(phone);
            account.setStatus(AccountStatus.ACTIVE);
            account.setFailedLoginAttempts(0);
            UserAccount saved = userAccountRepo.save(account);

            UserRoleMapping mapping = new UserRoleMapping();
            mapping.setUser(saved);
            mapping.setRole(role);
            userRoleMappingRepo.save(mapping);

            Profile profile = new Profile();
            profile.setEmployeeId(empId);
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setDepartment(department);
            profile.setDesignation(designation);
            profile.setSalary(salary);
            profile.setDateOfJoining(joiningDate);
            profile.setReportingManager(reportingManager);
            profile.setEmergencyContact("9876500000");
            profile.setAddress(address);
            profile.setDob(LocalDate.of(1992, 5, 20));
            profile.setGender(gender);
            profile.setUser(saved);
            profileRepo.save(profile);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed default roles
        Role adminRole = getOrCreateRole("ADMIN");
        Role hrRole = getOrCreateRole("HR_MANAGER");
        Role empRole = getOrCreateRole("EMPLOYEE");
        Role userRole = getOrCreateRole("USER");

        // 1. Admin
        seedEmployee("admin", "admin123", "admin@epams.com", "9876543210",
                adminRole, "EMP-1001", "System", "Admin",
                "Executive", "System Administrator & Lead", 125000.0,
                LocalDate.of(2023, 1, 10), "Board of Directors",
                "EPAMS Headquarters, Tech Park", "other");

        // 2. HR Manager
        seedEmployee("hr_manager", "hr123", "hr@epams.com", "9876543215",
                hrRole, "EMP-1002", "Priya", "Sharma",
                "Human Resources", "Senior HR Operations Lead", 95000.0,
                LocalDate.of(2023, 4, 15), "admin",
                "Suite 404, Silicon Boulevard", "female");

        // 3. Senior Engineer (John Doe)
        seedEmployee("john_doe", "user123", "john.doe@epams.com", "9876543211",
                empRole, "EMP-1003", "John", "Doe",
                "Engineering", "Senior Full-Stack Engineer", 108000.0,
                LocalDate.of(2023, 6, 1), "admin",
                "123 Innovation Drive, Silicon Valley", "male");

        // 4. Frontend Architect
        seedEmployee("sarah_connor", "user123", "sarah.c@epams.com", "9876543212",
                empRole, "EMP-1004", "Sarah", "Connor",
                "Engineering", "Lead UI/UX Architect", 112000.0,
                LocalDate.of(2023, 8, 15), "admin",
                "45 Skyline Avenue, Metro City", "female");

        // 5. Finance Specialist
        seedEmployee("alex_chen", "user123", "alex.chen@epams.com", "9876543213",
                empRole, "EMP-1005", "Alex", "Chen",
                "Finance", "Corporate Financial Analyst", 92000.0,
                LocalDate.of(2024, 2, 1), "hr_manager",
                "88 Wall Street Lane, Financial District", "male");

        // 6. Operations Specialist
        seedEmployee("emily_watson", "user123", "emily.w@epams.com", "9876543214",
                empRole, "EMP-1006", "Emily", "Watson",
                "Operations", "Operations & Logistics Specialist", 82000.0,
                LocalDate.of(2024, 5, 20), "hr_manager",
                "12 Harbor Bay Road, Coastal Hub", "female");
    }
}
